package com.smhomelab.notifier.handler

import com.smhomelab.notifier.bot.BotCallbacks
import com.smhomelab.notifier.bot.BotCommands
import com.smhomelab.notifier.bot.secureCallback
import com.smhomelab.notifier.bot.secureCommand
import com.smhomelab.notifier.service.AuthorizationService
import com.smhomelab.notifier.service.LimitsMonitorService
import com.smhomelab.notifier.service.UserService
import io.github.dehuckakpyt.telegrambot.annotation.HandlerComponent
import io.github.dehuckakpyt.telegrambot.factory.keyboard.inlineKeyboard
import io.github.dehuckakpyt.telegrambot.handler.BotHandler
import io.github.oshai.kotlinlogging.KotlinLogging

private val logger = KotlinLogging.logger {}

@HandlerComponent
class AdminHandler(
    private val auth: AuthorizationService,
    private val limitsMonitorService: LimitsMonitorService,
    private val userService: UserService,
) : BotHandler({

        fun adminPanelText(): String {
            val limits = limitsMonitorService.getLimits()
            val limitsStatus = limitsMonitorService.formatStatusMessage(limits)
            val usersCount = userService.getAllUsers().size

            return buildString {
                appendLine("<b>🔧 Панель администратора</b>")
                appendLine()
                appendLine("👥 Пользователей: <b>$usersCount</b>")
                appendLine()
                append(limitsStatus)
            }
        }

        val adminPanelKeyboard = inlineKeyboard(
            callbackButton("🔄 Обновить лимиты", BotCallbacks.ADMIN_LIMITS_REFRESH.callback),
        )

        secureCommand(BotCommands.ADMIN, auth) {
            logger.debug { "/admin from telegramId=${from.id}" }
            sendMessage(
                adminPanelText(),
                replyMarkup = adminPanelKeyboard,
                parseMode = "HTML",
            )
        }

        secureCallback(BotCallbacks.ADMIN_LIMITS_REFRESH, auth) {
            logger.debug { "Refreshing limits for telegramId=${from.id}" }
            editMessageText(
                messageId = message.messageId,
                text = adminPanelText(),
                replyMarkup = adminPanelKeyboard,
                parseMode = "HTML",
            )
        }
    })
