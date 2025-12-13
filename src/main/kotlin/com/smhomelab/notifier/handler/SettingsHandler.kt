package com.smhomelab.notifier.handler

import com.smhomelab.notifier.bot.BotCallbacks
import com.smhomelab.notifier.bot.BotCommands
import com.smhomelab.notifier.bot.BotSteps
import com.smhomelab.notifier.bot.secureCallback
import com.smhomelab.notifier.bot.secureCommand
import com.smhomelab.notifier.bot.secureStep
import com.smhomelab.notifier.model.common.ValidationResult
import com.smhomelab.notifier.model.pushover.NotificationPriority
import com.smhomelab.notifier.model.pushover.SendResult
import com.smhomelab.notifier.service.AuthorizationService
import com.smhomelab.notifier.service.SettingsService
import io.github.dehuckakpyt.telegrambot.annotation.HandlerComponent
import io.github.dehuckakpyt.telegrambot.factory.keyboard.inlineKeyboard
import io.github.dehuckakpyt.telegrambot.handler.BotHandler
import io.github.oshai.kotlinlogging.KotlinLogging

private val logger = KotlinLogging.logger {}

@HandlerComponent
class SettingsHandler(
    private val auth: AuthorizationService,
    private val settingsService: SettingsService,
) : BotHandler({

        fun mainSettingsText(configured: Boolean): String {
            val status = if (configured) "✓ Настроен" else "⚠️ Не настроен"
            return "⚙️ Настройки\n\n📱 Pushover: $status"
        }

        val mainSettingsKeyboard = inlineKeyboard(
            callbackButton("📱 Pushover", BotCallbacks.SETTINGS_PUSHOVER.callback),
        )

        fun testMenuTextWithResult(result: SendResult?) = when (result) {
            is SendResult.Sent -> "✓ Отправлено: ${result.priority.displayName}\n\nПриоритет:"
            SendResult.Failed -> "✗ Ошибка отправки\n\nПриоритет:"
            SendResult.Disabled -> "✗ Pushover недоступен\n\nПриоритет:"
            SendResult.UserDisabled -> "✗ Уведомления выключены\n\nПриоритет:"
            null -> "Приоритет:"
        }

        fun pushoverMenuText(enabled: Boolean): String {
            val status = if (enabled) "✓ Настроен" else "⏸ Настроен (пауза)"
            return "📱 Pushover: $status\n\nPushover позволяет получать уведомления на телефон."
        }

        fun pushoverMenuKeyboard(enabled: Boolean) = inlineKeyboard(
            callbackButton(if (enabled) "🔕 Выключить" else "🔔 Включить", BotCallbacks.PUSHOVER_TOGGLE.callback),
            callbackButton("🔔 Тест", BotCallbacks.PUSHOVER_TEST_MENU.callback),
            callbackButton("✏️ Изменить", BotCallbacks.PUSHOVER_CONFIGURE.callback),
            callbackButton("🗑 Удалить", BotCallbacks.PUSHOVER_REMOVE.callback),
            callbackButton("« Назад", BotCallbacks.SETTINGS_BACK.callback),
        )

        val pushoverNotConfiguredText = "📱 Pushover: ⚠️ Не настроен\n\nPushover позволяет получать уведомления на телефон."

        val pushoverNotConfiguredKeyboard = inlineKeyboard(
            callbackButton("⚙️ Настроить", BotCallbacks.PUSHOVER_CONFIGURE.callback),
            callbackButton("« Назад", BotCallbacks.SETTINGS_BACK.callback),
        )

        val testMenuKeyboard = inlineKeyboard(
            callbackButton("🔇 Без звука", BotCallbacks.PUSHOVER_TEST_LOWEST.callback),
            callbackButton("🔈 Тихо", BotCallbacks.PUSHOVER_TEST_LOW.callback),
            callbackButton("🔔 Обычно", BotCallbacks.PUSHOVER_TEST_NORMAL.callback),
            callbackButton("🔊 Важно", BotCallbacks.PUSHOVER_TEST_HIGH.callback),
            callbackButton("🚨 Экстренно", BotCallbacks.PUSHOVER_TEST_EMERGENCY.callback),
            callbackButton("« Назад", BotCallbacks.SETTINGS_PUSHOVER.callback),
        )

        // /settings - начало flow, отправляем новое сообщение
        secureCommand(BotCommands.SETTINGS, auth) {
            logger.debug { "/settings from telegramId=${from.id}" }
            val configured = settingsService.isPushoverConfigured(from.id)
            sendMessage(
                mainSettingsText(configured),
                replyMarkup = mainSettingsKeyboard,
            )
        }

        // Возврат в главные настройки - редактируем
        secureCallback(BotCallbacks.SETTINGS_BACK, auth) {
            val configured = settingsService.isPushoverConfigured(from.id)
            editMessageText(
                messageId = message.messageId,
                text = mainSettingsText(configured),
                replyMarkup = mainSettingsKeyboard,
            )
        }

        // Переход в меню Pushover - редактируем
        secureCallback(BotCallbacks.SETTINGS_PUSHOVER, auth) {
            val config = settingsService.getPushoverConfig(from.id)
            if (config == null) {
                editMessageText(
                    messageId = message.messageId,
                    text = pushoverNotConfiguredText,
                    replyMarkup = pushoverNotConfiguredKeyboard,
                )
            } else {
                editMessageText(
                    messageId = message.messageId,
                    text = pushoverMenuText(config.enabled),
                    replyMarkup = pushoverMenuKeyboard(config.enabled),
                )
            }
        }

        // Запрос ключа - отправляем новое (ждём ввод от пользователя)
        secureCallback(BotCallbacks.PUSHOVER_CONFIGURE, auth, next = BotSteps.GET_PUSHOVER_KEY.step) {
            sendMessage(
                "Введи свой Pushover User Key:\n\n" +
                    "1. Установи приложение Pushover\n" +
                    "2. Зайди на pushover.net\n" +
                    "3. Скопируй User Key с главной страницы",
            )
        }

        // Обработка ввода ключа - отправляем новое (ответ на ввод)
        secureStep(BotSteps.GET_PUSHOVER_KEY, auth) {
            when (val result = settingsService.validatePushoverKey(text)) {
                is ValidationResult.Invalid -> {
                    sendMessage(result.error)
                    return@secureStep
                }

                is ValidationResult.Valid -> {
                    logger.debug { "Saving Pushover key for telegramId=${from.id}" }
                    settingsService.savePushoverKey(from.id, result.value)
                    next(null)
                    sendMessage(
                        "✓ Pushover настроен!\n\nРекомендуем отправить тестовое уведомление.",
                        replyMarkup = inlineKeyboard(
                            callbackButton("🔔 Тест", BotCallbacks.PUSHOVER_TEST_MENU.callback),
                            callbackButton("« К настройкам", BotCallbacks.SETTINGS_PUSHOVER.callback),
                        ),
                    )
                }
            }
        }

        // Toggle - редактируем (обновление статуса на месте)
        secureCallback(BotCallbacks.PUSHOVER_TOGGLE, auth) {
            val config = settingsService.getPushoverConfig(from.id) ?: return@secureCallback
            val newEnabled = !config.enabled
            logger.debug { "Toggling Pushover enabled=$newEnabled for telegramId=${from.id}" }
            settingsService.setPushoverEnabled(from.id, newEnabled)

            editMessageText(
                messageId = message.messageId,
                text = pushoverMenuText(newEnabled),
                replyMarkup = pushoverMenuKeyboard(newEnabled),
            )
        }

        // Меню тестов - редактируем
        secureCallback(BotCallbacks.PUSHOVER_TEST_MENU, auth) {
            editMessageText(
                messageId = message.messageId,
                text = testMenuTextWithResult(null),
                replyMarkup = testMenuKeyboard,
            )
        }

        // Тесты - показываем результат в меню приоритетов
        secureCallback(BotCallbacks.PUSHOVER_TEST_LOWEST, auth) {
            val result = settingsService.sendTestNotification(from.id, NotificationPriority.LOWEST)
            editMessageText(
                messageId = message.messageId,
                text = testMenuTextWithResult(result),
                replyMarkup = testMenuKeyboard,
            )
        }

        secureCallback(BotCallbacks.PUSHOVER_TEST_LOW, auth) {
            val result = settingsService.sendTestNotification(from.id, NotificationPriority.LOW)
            editMessageText(
                messageId = message.messageId,
                text = testMenuTextWithResult(result),
                replyMarkup = testMenuKeyboard,
            )
        }

        secureCallback(BotCallbacks.PUSHOVER_TEST_NORMAL, auth) {
            val result = settingsService.sendTestNotification(from.id, NotificationPriority.NORMAL)
            editMessageText(
                messageId = message.messageId,
                text = testMenuTextWithResult(result),
                replyMarkup = testMenuKeyboard,
            )
        }

        secureCallback(BotCallbacks.PUSHOVER_TEST_HIGH, auth) {
            val result = settingsService.sendTestNotification(from.id, NotificationPriority.HIGH)
            editMessageText(
                messageId = message.messageId,
                text = testMenuTextWithResult(result),
                replyMarkup = testMenuKeyboard,
            )
        }

        secureCallback(BotCallbacks.PUSHOVER_TEST_EMERGENCY, auth) {
            val result = settingsService.sendTestNotification(from.id, NotificationPriority.EMERGENCY)
            editMessageText(
                messageId = message.messageId,
                text = testMenuTextWithResult(result),
                replyMarkup = testMenuKeyboard,
            )
        }

        // Удаление - редактируем
        secureCallback(BotCallbacks.PUSHOVER_REMOVE, auth) {
            logger.debug { "Removing Pushover config for telegramId=${from.id}" }
            settingsService.removePushoverConfig(from.id)
            editMessageText(
                messageId = message.messageId,
                text = "✓ Pushover настройки удалены",
                replyMarkup = inlineKeyboard(
                    callbackButton("« К настройкам", BotCallbacks.SETTINGS_PUSHOVER.callback),
                ),
            )
        }
    })
