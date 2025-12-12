package com.smhomelab.notifier.handler

import com.smhomelab.notifier.bot.BotCallbacks
import com.smhomelab.notifier.bot.BotCommands
import com.smhomelab.notifier.bot.BotSteps
import com.smhomelab.notifier.bot.secureCallback
import com.smhomelab.notifier.bot.secureCommand
import com.smhomelab.notifier.bot.secureStep
import com.smhomelab.notifier.persistence.model.UserRole
import com.smhomelab.notifier.service.AuthorizationService
import com.smhomelab.notifier.service.BotUserService
import com.smhomelab.notifier.service.InvitationService
import io.github.dehuckakpyt.telegrambot.annotation.HandlerComponent
import io.github.dehuckakpyt.telegrambot.factory.keyboard.inlineKeyboard
import io.github.dehuckakpyt.telegrambot.handler.BotHandler

@HandlerComponent
class AdminHandler(
    private val auth: AuthorizationService,
    private val botUserService: BotUserService,
    private val invitationService: InvitationService,
) : BotHandler({

    // === LIST_USERS ===
    secureCommand(BotCommands.LIST_USERS, auth) {
        val users = botUserService.getAllUsers()
        val invitations = invitationService.getAll()

        val usersText = if (users.isEmpty()) {
            "Нет зарегистрированных пользователей"
        } else {
            users.mapIndexed { i, user ->
                val username = user.username?.let { "@$it" } ?: "—"
                val name = listOfNotNull(user.firstName, user.lastName)
                    .joinToString(" ").ifEmpty { "—" }
                "${i + 1}. $username ($name) — ${user.role}"
            }.joinToString("\n")
        }

        val invitationsText = if (invitations.isEmpty()) {
            ""
        } else {
            "\n\nПриглашения:\n" + invitations.mapIndexed { i, inv ->
                "${i + 1}. @${inv.username} — ${inv.role}"
            }.joinToString("\n")
        }

        sendMessage("Пользователи:\n$usersText$invitationsText")
    }

    // === ADD_USER ===
    secureCommand(BotCommands.ADD_USER, auth) {
        sendMessage(
            "Как добавить пользователя?",
            replyMarkup = inlineKeyboard(
                callbackButton("По Telegram ID", BotCallbacks.ADMIN_ADD_BY_ID.callback),
                callbackButton("По @username", BotCallbacks.ADMIN_ADD_BY_USERNAME.callback),
                callbackButton("Отмена", BotCallbacks.ADMIN_CANCEL.callback)
            )
        )
    }

    secureCallback(BotCallbacks.ADMIN_ADD_BY_ID, auth, next = BotSteps.GET_USER_ID.step) {
        sendMessage("Введи Telegram ID пользователя:")
    }

    secureCallback(BotCallbacks.ADMIN_ADD_BY_USERNAME, auth, next = BotSteps.GET_USERNAME.step) {
        sendMessage("Введи @username пользователя (без @):")
    }

    secureCallback(BotCallbacks.ADMIN_CANCEL, auth) {
        sendMessage("Отменено")
    }

    secureStep(BotSteps.GET_USER_ID, auth) {
        val input = text.trim()
        val telegramId = input.toLongOrNull()

        if (telegramId == null) {
            sendMessage("Неверный формат ID. Введи число:")
            return@secureStep
        }

        if (botUserService.existsByTelegramId(telegramId)) {
            sendMessage("Пользователь с таким ID уже существует")
            return@secureStep
        }

        transfer(telegramId)
        next(null)
        sendMessage(
            "Выбери роль для пользователя:",
            replyMarkup = inlineKeyboard(
                callbackButton("User", BotCallbacks.ROLE_USER.callback),
                callbackButton("Admin", BotCallbacks.ROLE_ADMIN.callback)
            )
        )
    }

    secureStep(BotSteps.GET_USERNAME, auth) {
        val username = text.trim().removePrefix("@").lowercase()

        if (username.isEmpty()) {
            sendMessage("Username не может быть пустым. Введи ещё раз:")
            return@secureStep
        }

        if (botUserService.existsByUsername(username) || invitationService.existsByUsername(username)) {
            sendMessage("Пользователь или приглашение с таким username уже существует")
            return@secureStep
        }

        transfer(username)
        next(null)
        sendMessage(
            "Выбери роль для @$username:",
            replyMarkup = inlineKeyboard(
                callbackButton("User", BotCallbacks.ROLE_USER.callback),
                callbackButton("Admin", BotCallbacks.ROLE_ADMIN.callback)
            )
        )
    }

    secureCallback(BotCallbacks.ROLE_USER, auth) {
        handleRoleSelection(UserRole.USER)
    }

    secureCallback(BotCallbacks.ROLE_ADMIN, auth) {
        handleRoleSelection(UserRole.ADMIN)
    }

    // === REMOVE_USER ===
    secureCommand(BotCommands.REMOVE_USER, auth) {
        val users = botUserService.getAllUsers()
        val invitations = invitationService.getAll()

        if (users.isEmpty() && invitations.isEmpty()) {
            sendMessage("Нет пользователей или приглашений для удаления")
            return@secureCommand
        }

        val buttons = mutableListOf<Pair<String, String>>()

        users.forEach { user ->
            val label = user.username?.let { "@$it" } ?: "ID:${user.telegramId}"
            buttons.add(label to "remove:user:${user.telegramId}")
        }

        invitations.forEach { inv ->
            buttons.add("@${inv.username} (invite)" to "remove:invite:${inv.username}")
        }

        buttons.add("Отмена" to BotCallbacks.ADMIN_CANCEL.callback)

        sendMessage(
            "Выбери кого удалить:",
            replyMarkup = inlineKeyboard(*buttons.map { callbackButton(it.first, it.second) }.toTypedArray())
        )
    }

    callbackWithPrefix("remove:user:") {
        if (!auth.isAuthorized(from.id, UserRole.ADMIN)) return@callbackWithPrefix

        val telegramId = callbackData.removePrefix("remove:user:").toLongOrNull()
        if (telegramId == null) {
            sendMessage("Ошибка: неверный ID")
            return@callbackWithPrefix
        }

        botUserService.deleteUser(telegramId)
        sendMessage("Пользователь удалён")
    }

    callbackWithPrefix("remove:invite:") {
        if (!auth.isAuthorized(from.id, UserRole.ADMIN)) return@callbackWithPrefix

        val username = callbackData.removePrefix("remove:invite:")
        invitationService.delete(username)
        sendMessage("Приглашение для @$username удалено")
    }
}) {
    private suspend fun io.github.dehuckakpyt.telegrambot.container.CallbackContainer.handleRoleSelection(role: UserRole) {
        val data = transferredOrNull<Any>()

        when (data) {
            is Long -> {
                botUserService.createUser(data, role)
                sendMessage("Пользователь с ID $data добавлен с ролью $role")
            }
            is String -> {
                invitationService.create(data, role, from.id)
                sendMessage("Приглашение для @$data создано с ролью $role.\nПользователь получит доступ после /start")
            }
            else -> {
                sendMessage("Ошибка: данные не найдены. Попробуй снова /add_user")
            }
        }
    }
}
