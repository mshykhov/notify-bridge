package com.smhomelab.notifier.handler

import com.smhomelab.notifier.bot.BotCallbacks
import com.smhomelab.notifier.bot.BotCommands
import com.smhomelab.notifier.bot.BotSteps
import com.smhomelab.notifier.bot.secureCallback
import com.smhomelab.notifier.bot.secureCommand
import com.smhomelab.notifier.bot.secureStep
import com.smhomelab.notifier.common.ValidationResult
import com.smhomelab.notifier.service.AuthorizationService
import com.smhomelab.notifier.service.SettingsService
import com.smhomelab.notifier.service.SettingsService.SendResult
import io.github.dehuckakpyt.telegrambot.annotation.HandlerComponent
import io.github.dehuckakpyt.telegrambot.container.GeneralContainer
import io.github.dehuckakpyt.telegrambot.factory.keyboard.inlineKeyboard
import io.github.dehuckakpyt.telegrambot.handler.BotHandler

@HandlerComponent
class SettingsHandler(
    private val auth: AuthorizationService,
    private val settingsService: SettingsService,
) : BotHandler({

        suspend fun GeneralContainer.showMainSettings() {
            val configured = settingsService.isPushoverConfigured(from.id)
            val status = if (configured) "✓ Настроен" else "⚠️ Не настроен"

            sendMessage(
                "⚙️ Настройки\n\n📱 Pushover: $status",
                replyMarkup = inlineKeyboard(
                    callbackButton("📱 Pushover", BotCallbacks.SETTINGS_PUSHOVER.callback),
                ),
            )
        }

        secureCommand(BotCommands.SETTINGS, auth) {
            showMainSettings()
        }

        secureCallback(BotCallbacks.SETTINGS_BACK, auth) {
            showMainSettings()
        }

        secureCallback(BotCallbacks.SETTINGS_PUSHOVER, auth) {
            val configured = settingsService.isPushoverConfigured(from.id)

            val buttons = if (configured) {
                arrayOf(
                    callbackButton("🔔 Тест", BotCallbacks.PUSHOVER_TEST.callback),
                    callbackButton("✏️ Изменить", BotCallbacks.PUSHOVER_CONFIGURE.callback),
                    callbackButton("🗑 Удалить", BotCallbacks.PUSHOVER_REMOVE.callback),
                    callbackButton("« Назад", BotCallbacks.SETTINGS_BACK.callback),
                )
            } else {
                arrayOf(
                    callbackButton("⚙️ Настроить", BotCallbacks.PUSHOVER_CONFIGURE.callback),
                    callbackButton("« Назад", BotCallbacks.SETTINGS_BACK.callback),
                )
            }

            val status = if (configured) "✓ Настроен" else "⚠️ Не настроен"
            sendMessage(
                "📱 Pushover: $status\n\nPushover позволяет получать уведомления на телефон.",
                replyMarkup = inlineKeyboard(*buttons),
            )
        }

        secureCallback(BotCallbacks.PUSHOVER_CONFIGURE, auth, next = BotSteps.GET_PUSHOVER_KEY.step) {
            sendMessage(
                "Введи свой Pushover User Key:\n\n" +
                    "1. Установи приложение Pushover\n" +
                    "2. Зайди на pushover.net\n" +
                    "3. Скопируй User Key с главной страницы",
            )
        }

        secureStep(BotSteps.GET_PUSHOVER_KEY, auth) {
            when (val result = settingsService.validatePushoverKey(text)) {
                is ValidationResult.Invalid -> {
                    sendMessage(result.error)
                    return@secureStep
                }

                is ValidationResult.Valid -> {
                    settingsService.savePushoverKey(from.id, result.value)
                    next(null)

                    val message = when (settingsService.sendTestNotification(from.id)) {
                        SendResult.Sent -> "✓ Pushover настроен!\n\nТестовое уведомление отправлено."
                        SendResult.Failed -> "✓ Ключ сохранён, но тест не прошёл. Проверь ключ."
                        SendResult.Disabled -> "✓ Ключ сохранён. Pushover временно недоступен."
                    }
                    sendMessage(
                        message,
                        replyMarkup = inlineKeyboard(
                            callbackButton("« К настройкам", BotCallbacks.SETTINGS_PUSHOVER.callback),
                        ),
                    )
                }
            }
        }

        secureCallback(BotCallbacks.PUSHOVER_TEST, auth) {
            val message = when (settingsService.sendTestNotification(from.id)) {
                SendResult.Sent -> "✓ Тестовое уведомление отправлено!"
                SendResult.Failed -> "✗ Не удалось отправить. Проверь ключ."
                SendResult.Disabled -> "✗ Pushover временно недоступен."
            }
            sendMessage(message)
        }

        secureCallback(BotCallbacks.PUSHOVER_REMOVE, auth) {
            settingsService.removePushoverKey(from.id)
            sendMessage(
                "✓ Pushover отключён",
                replyMarkup = inlineKeyboard(
                    callbackButton("« К настройкам", BotCallbacks.SETTINGS_PUSHOVER.callback),
                ),
            )
        }
    })
