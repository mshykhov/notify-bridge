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
import io.github.oshai.kotlinlogging.KotlinLogging

private val logger = KotlinLogging.logger {}

@HandlerComponent
class UserManagementHandler(
    private val auth: AuthorizationService,
    private val userManagementService: UserManagementService,
    private val userService: UserService,
    private val invitationService: InvitationService,
) : BotHandler({

        val enterIdText = "Введи Telegram ID пользователя:"
        val enterUsernameText = "Введи @username пользователя (без @):"

        val cancelKeyboard = inlineKeyboard(
            callbackButton("✗ Отмена", BotCallbacks.ADMIN_CANCEL.callback),
        )

        // === LIST_USERS ===
        secureCommand(BotCommands.LIST_USERS, auth) {
            logger.debug { "/list_users by adminId=${from.id}" }
            sendMessage(userManagementService.getUsersListText())
        }

        // === ADD_USER ===
        secureCommand(BotCommands.ADD_USER, auth) {
            logger.debug { "/add_user by adminId=${from.id}" }
            sendMessage(
                "Как добавить пользователя?",
                replyMarkup = inlineKeyboard(
                    callbackButton("По Telegram ID", BotCallbacks.ADMIN_ADD_BY_ID.callback),
                    callbackButton("По @username", BotCallbacks.ADMIN_ADD_BY_USERNAME.callback),
                    callbackButton("✗ Отмена", BotCallbacks.ADMIN_CANCEL.callback),
                ),
            )
        }

        secureCallback(BotCallbacks.ADMIN_ADD_BY_ID, auth) {
            editMessageText(
                messageId = message.messageId,
                text = enterIdText,
                replyMarkup = cancelKeyboard,
            )
            next(BotSteps.GET_USER_ID.step, message.messageId)
        }

        secureCallback(BotCallbacks.ADMIN_ADD_BY_USERNAME, auth) {
            editMessageText(
                messageId = message.messageId,
                text = enterUsernameText,
                replyMarkup = cancelKeyboard,
            )
            next(BotSteps.GET_USERNAME.step, message.messageId)
        }

        secureCallback(BotCallbacks.ADMIN_CANCEL, auth) {
            next(null)
            editMessageText(
                messageId = message.messageId,
                text = "✗ Отменено",
            )
        }

        secureStep(BotSteps.GET_USER_ID, auth) {
            val promptMessageId = transferred<Long>()
            deleteMessage(message.messageId)

            when (val result = userManagementService.validateTelegramId(text)) {
                is ValidationResult.Invalid -> {
                    editMessageText(
                        messageId = promptMessageId,
                        text = "❌ ${result.error}\n\n$enterIdText",
                        replyMarkup = cancelKeyboard,
                    )
                    next(BotSteps.GET_USER_ID.step, promptMessageId)
                }
                is ValidationResult.Valid -> {
                    next(null)
                    editMessageText(
                        messageId = promptMessageId,
                        text = "Выбери роль для пользователя:",
                        replyMarkup = inlineKeyboard(
                            callbackButton("User", next = BotCallbacks.ROLE_USER.callback, content = "id:${result.value}"),
                            callbackButton("Admin", next = BotCallbacks.ROLE_ADMIN.callback, content = "id:${result.value}"),
                        ),
                    )
                }
            }
        }

        secureStep(BotSteps.GET_USERNAME, auth) {
            val promptMessageId = transferred<Long>()
            deleteMessage(message.messageId)

            when (val result = userManagementService.validateUsername(text)) {
                is ValidationResult.Invalid -> {
                    editMessageText(
                        messageId = promptMessageId,
                        text = "❌ ${result.error}\n\n$enterUsernameText",
                        replyMarkup = cancelKeyboard,
                    )
                    next(BotSteps.GET_USERNAME.step, promptMessageId)
                }
                is ValidationResult.Valid -> {
                    next(null)
                    editMessageText(
                        messageId = promptMessageId,
                        text = "Выбери роль для @${result.value}:",
                        replyMarkup = inlineKeyboard(
                            callbackButton("User", next = BotCallbacks.ROLE_USER.callback, content = "username:${result.value}"),
                            callbackButton("Admin", next = BotCallbacks.ROLE_ADMIN.callback, content = "username:${result.value}"),
                        ),
                    )
                }
            }
        }

        secureCallback(BotCallbacks.ROLE_USER, auth) {
            val data = transferred<String>()
            logger.debug { "Adding user with role=USER, data=$data, by adminId=${from.id}" }
            val result = userManagementService.addUser(data, UserRole.USER, from.id)
            editMessageText(
                messageId = message.messageId,
                text = result,
            )
        }

        secureCallback(BotCallbacks.ROLE_ADMIN, auth) {
            val data = transferred<String>()
            logger.debug { "Adding user with role=ADMIN, data=$data, by adminId=${from.id}" }
            val result = userManagementService.addUser(data, UserRole.ADMIN, from.id)
            editMessageText(
                messageId = message.messageId,
                text = result,
            )
        }

        secureCommand(BotCommands.REMOVE_USER, auth) {
            logger.debug { "/remove_user by adminId=${from.id}" }
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

            buttons.add(callbackButton("✗ Отмена", BotCallbacks.ADMIN_CANCEL.callback))

            sendMessage(
                "Выбери кого удалить:",
                replyMarkup = inlineKeyboard(*buttons.toTypedArray()),
            )
        }

        secureCallback(BotCallbacks.REMOVE_USER, auth) {
            val telegramId = transferred<String>().toLongOrNull()
            if (telegramId == null) {
                editMessageText(
                    messageId = message.messageId,
                    text = "❌ Ошибка: неверный ID",
                )
                return@secureCallback
            }
            logger.debug { "Removing user telegramId=$telegramId, by adminId=${from.id}" }
            val result = userManagementService.removeUser(telegramId)
            editMessageText(
                messageId = message.messageId,
                text = result,
            )
        }

        secureCallback(BotCallbacks.REMOVE_INVITE, auth) {
            val username = transferred<String>()
            logger.debug { "Removing invitation username=$username, by adminId=${from.id}" }
            val result = userManagementService.removeInvitation(username)
            editMessageText(
                messageId = message.messageId,
                text = result,
            )
        }
    })
