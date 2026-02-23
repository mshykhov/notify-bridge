package com.smhomelab.notifier.handler

import com.smhomelab.notifier.bot.BotCallbacks
import com.smhomelab.notifier.bot.BotCommands
import com.smhomelab.notifier.bot.BotSteps
import com.smhomelab.notifier.bot.secureCallback
import com.smhomelab.notifier.bot.secureCommand
import com.smhomelab.notifier.bot.secureStep
import com.smhomelab.notifier.api.NotificationPriority
import com.smhomelab.notifier.model.common.ValidationResult
import com.smhomelab.notifier.model.pushover.SendResult
import com.smhomelab.notifier.model.pushover.botDisplayName
import com.smhomelab.notifier.service.AuthorizationService
import com.smhomelab.notifier.service.SettingsService
import com.smhomelab.notifier.util.TimeUtils
import io.github.dehuckakpyt.telegrambot.annotation.HandlerComponent
import io.github.dehuckakpyt.telegrambot.factory.keyboard.inlineKeyboard
import io.github.dehuckakpyt.telegrambot.handler.BotHandler
import io.github.oshai.kotlinlogging.KotlinLogging
import java.time.Instant

private val logger = KotlinLogging.logger {}

@HandlerComponent
class SettingsHandler(
    private val auth: AuthorizationService,
    private val settingsService: SettingsService,
) : BotHandler({

        fun mainSettingsText(pushoverConfigured: Boolean, timezone: String): String {
            val pushoverStatus = if (pushoverConfigured) "✓ Настроен" else "⚠️ Не настроен"
            return "⚙️ Настройки\n\n📱 Pushover: $pushoverStatus\n🕐 Часовой пояс: $timezone"
        }

        val mainSettingsKeyboard = inlineKeyboard(
            callbackButton("📱 Pushover", BotCallbacks.SETTINGS_PUSHOVER.callback),
            callbackButton("🕐 Часовой пояс", BotCallbacks.SETTINGS_TIMEZONE.callback),
        )

        fun testMenuTextWithResult(result: SendResult?) = when (result) {
            is SendResult.Sent -> "✓ Отправлено: ${result.priority.botDisplayName}\n\nПриоритет:"
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
            val pushoverConfigured = settingsService.isPushoverConfigured(from.id)
            val timezone = settingsService.getTimezoneString(from.id)
            sendMessage(
                mainSettingsText(pushoverConfigured, timezone),
                replyMarkup = mainSettingsKeyboard,
            )
        }

        // Возврат в главные настройки - редактируем
        secureCallback(BotCallbacks.SETTINGS_BACK, auth) {
            val pushoverConfigured = settingsService.isPushoverConfigured(from.id)
            val timezone = settingsService.getTimezoneString(from.id)
            editMessageText(
                messageId = message.messageId,
                text = mainSettingsText(pushoverConfigured, timezone),
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

        val enterKeyText = "Введи свой Pushover User Key:\n\n" +
            "1. Установи приложение Pushover\n" +
            "2. Зайди на pushover.net\n" +
            "3. Скопируй User Key с главной страницы"

        val enterKeyKeyboard = inlineKeyboard(
            callbackButton("✗ Отмена", BotCallbacks.PUSHOVER_CANCEL.callback),
        )

        // Запрос ключа - редактируем меню и передаём messageId в step
        secureCallback(BotCallbacks.PUSHOVER_CONFIGURE, auth) {
            editMessageText(
                messageId = message.messageId,
                text = enterKeyText,
                replyMarkup = enterKeyKeyboard,
            )
            next(BotSteps.GET_PUSHOVER_KEY.step, message.messageId)
        }

        // Отмена ввода ключа - возврат в меню
        secureCallback(BotCallbacks.PUSHOVER_CANCEL, auth) {
            next(null)
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

        // Обработка ввода ключа - редактируем исходное сообщение
        secureStep(BotSteps.GET_PUSHOVER_KEY, auth) {
            val promptMessageId = transferred<Long>()
            deleteMessage(message.messageId)

            when (val result = settingsService.validatePushoverKey(text)) {
                is ValidationResult.Invalid -> {
                    editMessageText(
                        messageId = promptMessageId,
                        text = "❌ ${result.error}\n\n$enterKeyText",
                        replyMarkup = enterKeyKeyboard,
                    )
                    next(BotSteps.GET_PUSHOVER_KEY.step, promptMessageId)
                }

                is ValidationResult.Valid -> {
                    logger.debug { "Saving Pushover key for telegramId=${from.id}" }
                    settingsService.savePushoverKey(from.id, result.value)
                    next(null)
                    editMessageText(
                        messageId = promptMessageId,
                        text = "✓ Pushover настроен!\n\nРекомендуем отправить тестовое уведомление.",
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

        // Удаление - подтверждение
        secureCallback(BotCallbacks.PUSHOVER_REMOVE, auth) {
            editMessageText(
                messageId = message.messageId,
                text = "⚠️ Удалить настройки Pushover?\n\nЭто действие нельзя отменить.",
                replyMarkup = inlineKeyboard(
                    callbackButton("🗑 Удалить", BotCallbacks.PUSHOVER_REMOVE_CONFIRM.callback),
                    callbackButton("« Назад", BotCallbacks.SETTINGS_PUSHOVER.callback),
                ),
            )
        }

        // Удаление - подтверждено
        secureCallback(BotCallbacks.PUSHOVER_REMOVE_CONFIRM, auth) {
            logger.debug { "Removing Pushover config for telegramId=${from.id}" }
            settingsService.removePushoverConfig(from.id)
            editMessageText(
                messageId = message.messageId,
                text = "✓ Pushover настройки удалены",
                replyMarkup = inlineKeyboard(
                    callbackButton("« К настройкам", BotCallbacks.SETTINGS_BACK.callback),
                ),
            )
        }

        // ==================== Timezone ====================

        fun timezoneMenuText(timezone: String, zoneId: java.time.ZoneId): String {
            val currentTime = TimeUtils.formatFull(Instant.now(), zoneId)
            return "🕐 Часовой пояс: $timezone\n\nТекущее время: $currentTime"
        }

        val timezoneMenuKeyboard = inlineKeyboard(
            callbackButton("✏️ Изменить", BotCallbacks.TIMEZONE_CONFIGURE.callback),
            callbackButton("« Назад", BotCallbacks.SETTINGS_BACK.callback),
        )

        // Меню timezone
        secureCallback(BotCallbacks.SETTINGS_TIMEZONE, auth) {
            val timezone = settingsService.getTimezoneString(from.id)
            val zoneId = settingsService.getTimezone(from.id)
            editMessageText(
                messageId = message.messageId,
                text = timezoneMenuText(timezone, zoneId),
                replyMarkup = timezoneMenuKeyboard,
            )
        }

        val enterTimezoneText = "Введи часовой пояс:\n\n" +
            "Примеры: Europe/Kiev, UTC, America/New_York\n\n" +
            "Список зон: en.wikipedia.org/wiki/List_of_tz_database_time_zones"

        val enterTimezoneKeyboard = inlineKeyboard(
            callbackButton("✗ Отмена", BotCallbacks.TIMEZONE_CANCEL.callback),
        )

        // Запрос timezone
        secureCallback(BotCallbacks.TIMEZONE_CONFIGURE, auth) {
            editMessageText(
                messageId = message.messageId,
                text = enterTimezoneText,
                replyMarkup = enterTimezoneKeyboard,
            )
            next(BotSteps.GET_TIMEZONE.step, message.messageId)
        }

        // Отмена ввода timezone
        secureCallback(BotCallbacks.TIMEZONE_CANCEL, auth) {
            next(null)
            val timezone = settingsService.getTimezoneString(from.id)
            val zoneId = settingsService.getTimezone(from.id)
            editMessageText(
                messageId = message.messageId,
                text = timezoneMenuText(timezone, zoneId),
                replyMarkup = timezoneMenuKeyboard,
            )
        }

        // Обработка ввода timezone
        secureStep(BotSteps.GET_TIMEZONE, auth) {
            val promptMessageId = transferred<Long>()
            deleteMessage(message.messageId)

            when (val result = settingsService.validateTimezone(text)) {
                is ValidationResult.Invalid -> {
                    editMessageText(
                        messageId = promptMessageId,
                        text = "❌ ${result.error}\n\n$enterTimezoneText",
                        replyMarkup = enterTimezoneKeyboard,
                    )
                    next(BotSteps.GET_TIMEZONE.step, promptMessageId)
                }

                is ValidationResult.Valid -> {
                    logger.debug { "Setting timezone=${result.value} for telegramId=${from.id}" }
                    settingsService.setTimezone(from.id, result.value)
                    next(null)
                    val zoneId = settingsService.getTimezone(from.id)
                    val currentTime = TimeUtils.formatFull(Instant.now(), zoneId)
                    editMessageText(
                        messageId = promptMessageId,
                        text = "✓ Часовой пояс установлен: ${result.value}\n\nТекущее время: $currentTime",
                        replyMarkup = inlineKeyboard(
                            callbackButton("« К настройкам", BotCallbacks.SETTINGS_BACK.callback),
                        ),
                    )
                }
            }
        }
    })
