package com.smhomelab.notifier.handler

import com.smhomelab.notifier.bot.BotCallbacks
import com.smhomelab.notifier.bot.BotCommands
import com.smhomelab.notifier.bot.secureCallback
import com.smhomelab.notifier.bot.secureCommand
import com.smhomelab.notifier.service.AppInfoService
import com.smhomelab.notifier.service.AuthorizationService
import com.smhomelab.notifier.service.PushoverLimitsMonitorService
import com.smhomelab.notifier.service.SettingsService
import com.smhomelab.notifier.service.UserService
import io.github.dehuckakpyt.telegrambot.annotation.HandlerComponent
import io.github.dehuckakpyt.telegrambot.factory.keyboard.inlineKeyboard
import io.github.dehuckakpyt.telegrambot.handler.BotHandler
import io.github.oshai.kotlinlogging.KotlinLogging

private val logger = KotlinLogging.logger {}

@HandlerComponent
class AdminHandler(
    private val auth: AuthorizationService,
    private val pushoverLimitsMonitor: PushoverLimitsMonitorService,
    private val userService: UserService,
    private val appInfoService: AppInfoService,
    private val settingsService: SettingsService,
) : BotHandler({

        fun adminPanelText(telegramId: Long, forceRefresh: Boolean = false): String {
            val limits = if (forceRefresh) {
                pushoverLimitsMonitor.refreshLimits()
            } else {
                pushoverLimitsMonitor.getLimits()
            }
            val fetchedAt = pushoverLimitsMonitor.getLastFetchedAt()
            val zoneId = settingsService.getTimezone(telegramId)
            val limitsStatus = pushoverLimitsMonitor.formatStatusMessage(limits, fetchedAt, zoneId)
            val usersCount = userService.getAllUsers().size
            val version = appInfoService.getVersion()

            return buildString {
                appendLine("<b>🔧 Панель администратора</b>")
                appendLine()
                appendLine("📦 Версия: <b>$version</b>")
                appendLine("👥 Пользователей: <b>$usersCount</b>")
                appendLine()
                append(limitsStatus)
            }
        }

        val adminPanelKeyboard = inlineKeyboard(
            callbackButton("🔄 Обновить", BotCallbacks.ADMIN_REFRESH.callback),
        )

        secureCommand(BotCommands.ADMIN, auth) {
            logger.debug { "/admin from telegramId=${from.id}" }
            sendMessage(
                adminPanelText(from.id),
                replyMarkup = adminPanelKeyboard,
                parseMode = "HTML",
            )
        }

        secureCallback(BotCallbacks.ADMIN_REFRESH, auth) {
            logger.debug { "Refreshing admin panel for telegramId=${from.id}" }
            editMessageText(
                messageId = message.messageId,
                text = adminPanelText(from.id, forceRefresh = true),
                replyMarkup = adminPanelKeyboard,
                parseMode = "HTML",
            )
        }
    })
