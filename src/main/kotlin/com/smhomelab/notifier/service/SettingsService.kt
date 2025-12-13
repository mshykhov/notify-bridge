package com.smhomelab.notifier.service

import com.smhomelab.notifier.model.NotificationPriority
import com.smhomelab.notifier.model.common.ValidationResult
import com.smhomelab.notifier.persistence.facade.UserPushoverConfigFacade
import com.smhomelab.notifier.persistence.model.UserPushoverConfig
import com.smhomelab.notifier.pushover.PushoverService
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service

private val logger = KotlinLogging.logger {}

@Service
class SettingsService(
    private val pushoverConfigFacade: UserPushoverConfigFacade,
    private val pushoverService: PushoverService,
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
        val pushoverPriority = PushoverService.Priority.entries.find { it.value == priority.value }
            ?: PushoverService.Priority.NORMAL
        val success = pushoverService.sendWithPriority(
            config.userKey,
            "Тестовое уведомление от Notifier\nПриоритет: ${priority.displayName}",
            "Тест",
            pushoverPriority,
        )
        return if (success) {
            logger.debug { "Test notification sent: telegramId=$telegramId, priority=$priority" }
            SendResult.Sent(priority)
        } else {
            logger.warn { "Test notification failed: telegramId=$telegramId" }
            SendResult.Failed
        }
    }

    sealed interface SendResult {
        data class Sent(
            val priority: NotificationPriority,
        ) : SendResult

        data object Failed : SendResult

        data object Disabled : SendResult

        data object UserDisabled : SendResult
    }
}
