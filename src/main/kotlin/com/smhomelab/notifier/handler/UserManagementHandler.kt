package com.smhomelab.notifier.handler

import com.smhomelab.notifier.bot.BotCallbacks
import com.smhomelab.notifier.bot.BotCommands
import com.smhomelab.notifier.bot.BotSteps
import com.smhomelab.notifier.bot.secureCallback
import com.smhomelab.notifier.bot.secureCommand
import com.smhomelab.notifier.bot.secureStep
import com.smhomelab.notifier.model.common.ValidationResult
import com.smhomelab.notifier.persistence.model.UserRole
import com.smhomelab.notifier.service.AuthorizationService
import com.smhomelab.notifier.service.InvitationService
import com.smhomelab.notifier.service.UserManagementService
import com.smhomelab.notifier.service.UserService
import io.github.dehuckakpyt.telegrambot.annotation.HandlerComponent
import io.github.dehuckakpyt.telegrambot.factory.keyboard.inlineKeyboard
import io.github.dehuckakpyt.telegrambot.handler.BotHandler
import io.github.dehuckakpyt.telegrambot.model.telegram.InlineKeyboardButton

@HandlerComponent
class UserManagementHandler(
    private val auth: AuthorizationService,
    private val userManagementService: UserManagementService,
    private val userService: UserService,
    private val invitationService: InvitationService,
) : BotHandler({

        // === LIST_USERS ===
        secureCommand(BotCommands.LIST_USERS, auth) {
            sendMessage(userManagementService.getUsersListText())
        }

        // === ADD_USER ===
        secureCommand(BotCommands.ADD_USER, auth) {
            sendMessage(
                "Как добавить пользователя?",
                replyMarkup = inlineKeyboard(
                    callbackButton("По Telegram ID", BotCallbacks.ADMIN_ADD_BY_ID.callback),
                    callbackButton("По @username", BotCallbacks.ADMIN_ADD_BY_USERNAME.callback),
                    callbackButton("Отмена", BotCallbacks.ADMIN_CANCEL.callback),
                ),
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
            when (val result = userManagementService.validateTelegramId(text)) {
                is ValidationResult.Invalid -> {
                    sendMessage(result.error)
                    return@secureStep
                }
                is ValidationResult.Valid -> {
                    next(null)
                    sendMessage(
                        "Выбери роль для пользователя:",
                        replyMarkup = inlineKeyboard(
                            callbackButton("User", next = BotCallbacks.ROLE_USER.callback, content = "id:${result.value}"),
                            callbackButton("Admin", next = BotCallbacks.ROLE_ADMIN.callback, content = "id:${result.value}"),
                        ),
                    )
                }
            }
        }

        secureStep(BotSteps.GET_USERNAME, auth) {
            when (val result = userManagementService.validateUsername(text)) {
                is ValidationResult.Invalid -> {
                    sendMessage(result.error)
                    return@secureStep
                }
                is ValidationResult.Valid -> {
                    next(null)
                    sendMessage(
                        "Выбери роль для @${result.value}:",
                        replyMarkup = inlineKeyboard(
                            callbackButton("User", next = BotCallbacks.ROLE_USER.callback, content = "username:${result.value}"),
                            callbackButton("Admin", next = BotCallbacks.ROLE_ADMIN.callback, content = "username:${result.value}"),
                        ),
                    )
                }
            }
        }

        secureCallback(BotCallbacks.ROLE_USER, auth) {
            val message = userManagementService.addUser(transferred<String>(), UserRole.USER, from.id)
            sendMessage(message)
        }

        secureCallback(BotCallbacks.ROLE_ADMIN, auth) {
            val message = userManagementService.addUser(transferred<String>(), UserRole.ADMIN, from.id)
            sendMessage(message)
        }

        secureCommand(BotCommands.REMOVE_USER, auth) {
            if (!userManagementService.hasUsersOrInvitations()) {
                sendMessage("Нет пользователей или приглашений для удаления")
                return@secureCommand
            }

            val users = userService.getAllUsers()
            val invitations = invitationService.getAll()
            val buttons = mutableListOf<InlineKeyboardButton>()

            users.forEach { user ->
                val label = user.username?.let { "@$it" } ?: "ID:${user.telegramId}"
                buttons.add(callbackButton(label, next = BotCallbacks.REMOVE_USER.callback, content = user.telegramId.toString()))
            }

            invitations.forEach { inv ->
                buttons.add(callbackButton("@${inv.username} (invite)", next = BotCallbacks.REMOVE_INVITE.callback, content = inv.username))
            }

            buttons.add(callbackButton("Отмена", BotCallbacks.ADMIN_CANCEL.callback))

            sendMessage(
                "Выбери кого удалить:",
                replyMarkup = inlineKeyboard(*buttons.toTypedArray()),
            )
        }

        secureCallback(BotCallbacks.REMOVE_USER, auth) {
            val telegramId = transferred<String>().toLongOrNull()
            if (telegramId == null) {
                sendMessage("Ошибка: неверный ID")
                return@secureCallback
            }
            val message = userManagementService.removeUser(telegramId)
            sendMessage(message)
        }

        secureCallback(BotCallbacks.REMOVE_INVITE, auth) {
            val message = userManagementService.removeInvitation(transferred<String>())
            sendMessage(message)
        }
    })
