package com.smhomelab.notifier.service

import com.smhomelab.notifier.model.common.ValidationResult
import com.smhomelab.notifier.model.pushover.NotificationPriority
import com.smhomelab.notifier.model.pushover.SendResult
import com.smhomelab.notifier.persistence.facade.BotUserFacade
import com.smhomelab.notifier.persistence.facade.UserPushoverConfigFacade
import com.smhomelab.notifier.persistence.model.UserPushoverConfig
import com.smhomelab.notifier.pushover.PushoverService
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service
import java.time.ZoneId

private val logger = KotlinLogging.logger {}

@Service
class SettingsService(
    private val pushoverConfigFacade: UserPushoverConfigFacade,
    private val pushoverService: PushoverService,
    private val botUserFacade: BotUserFacade,
) {
    fun getPushoverConfig(telegramId: Long): UserPushoverConfig? =
        pushoverConfigFacade.findByTelegramId(telegramId)

    fun isPushoverConfigured(telegramId: Long): Boolean =
        pushoverConfigFacade.existsByTelegramId(telegramId)

    fun validatePushoverKey(key: String): ValidationResult<String> {
        val trimmed = key.trim()
        if (trimmed.length != 30) {
            return ValidationResult.Invalid("Неверный формат ключа. Ключ должен быть 30 символов.")
        }
        if (!trimmed.matches(Regex("[A-Za-z0-9]+"))) {
            return ValidationResult.Invalid("Ключ может содержать только буквы и цифры.")
        }
        if (!pushoverService.validateUserKey(trimmed)) {
            return ValidationResult.Invalid("Ключ не найден в Pushover. Проверь правильность ключа.")
        }
        return ValidationResult.Valid(trimmed)
    }

    fun savePushoverKey(telegramId: Long, key: String) {
        val existing = pushoverConfigFacade.findByTelegramId(telegramId)
        if (existing != null) {
            pushoverConfigFacade.updateUserKey(telegramId, key)
            logger.debug { "Updated Pushover key for telegramId=$telegramId" }
        } else {
            pushoverConfigFacade.create(telegramId, key)
            logger.info { "Created Pushover config for telegramId=$telegramId" }
        }
    }

    fun removePushoverConfig(telegramId: Long) {
        pushoverConfigFacade.deleteByTelegramId(telegramId)
        logger.info { "Removed Pushover config for telegramId=$telegramId" }
    }

    fun setPushoverEnabled(telegramId: Long, enabled: Boolean) {
        pushoverConfigFacade.updateEnabled(telegramId, enabled)
        logger.debug { "Set Pushover enabled=$enabled for telegramId=$telegramId" }
    }

    fun isServerPushoverEnabled(): Boolean = pushoverService.isEnabled()

    fun sendTestNotification(
        telegramId: Long,
        priority: NotificationPriority = NotificationPriority.NORMAL,
    ): SendResult {
        logger.debug { "Sending test notification: telegramId=$telegramId, priority=$priority" }
        if (!pushoverService.isEnabled()) {
            logger.debug { "Test notification skipped: Pushover disabled on server" }
            return SendResult.Disabled
        }
        val config = getPushoverConfig(telegramId)
        if (config == null) {
            logger.debug { "Test notification failed: config not found for telegramId=$telegramId" }
            return SendResult.Failed
        }
        if (!config.enabled) {
            logger.debug { "Test notification skipped: user disabled for telegramId=$telegramId" }
            return SendResult.UserDisabled
        }
        val sound = pushoverService.getDefaultSoundForPriority(priority)
        val soundName = sound?.displayName ?: "По умолчанию"
        val success = pushoverService.send(
            userKey = config.userKey,
            message = "Тестовое уведомление от Notifier\nПриоритет: ${priority.displayName}\nЗвук: $soundName",
            title = "Тест",
            priority = priority,
            ttl = TEST_NOTIFICATION_TTL,
        )
        return if (success) {
            logger.debug { "Test notification sent: telegramId=$telegramId, priority=${priority.displayName}, sound=$soundName" }
            SendResult.Sent(priority)
        } else {
            logger.warn { "Test notification failed: telegramId=$telegramId" }
            SendResult.Failed
        }
    }

    fun getTimezone(telegramId: Long): ZoneId =
        try {
            ZoneId.of(botUserFacade.getTimezone(telegramId))
        } catch (_: Exception) {
            ZoneId.of("UTC")
        }

    fun getTimezoneString(telegramId: Long): String =
        botUserFacade.getTimezone(telegramId)

    fun validateTimezone(input: String): ValidationResult<String> {
        val trimmed = input.trim()
        return try {
            ZoneId.of(trimmed)
            ValidationResult.Valid(trimmed)
        } catch (_: Exception) {
            ValidationResult.Invalid("Неверный формат. Примеры: Europe/Kiev, UTC, America/New_York")
        }
    }

    fun setTimezone(telegramId: Long, timezone: String) {
        botUserFacade.updateTimezone(telegramId, timezone)
        logger.debug { "Set timezone=$timezone for telegramId=$telegramId" }
    }

    companion object {
        private const val TEST_NOTIFICATION_TTL = 60
    }
}
