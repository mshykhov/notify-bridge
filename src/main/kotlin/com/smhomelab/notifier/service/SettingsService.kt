package com.smhomelab.notifier.service

import com.smhomelab.notifier.model.NotificationPriority
import com.smhomelab.notifier.model.common.ValidationResult
import com.smhomelab.notifier.persistence.facade.UserPushoverConfigFacade
import com.smhomelab.notifier.persistence.model.UserPushoverConfig
import com.smhomelab.notifier.pushover.PushoverService
import org.springframework.stereotype.Service

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
        } else {
            pushoverConfigFacade.create(telegramId, key)
        }
    }

    fun removePushoverConfig(telegramId: Long) {
        pushoverConfigFacade.deleteByTelegramId(telegramId)
    }

    fun setPushoverEnabled(telegramId: Long, enabled: Boolean) {
        pushoverConfigFacade.updateEnabled(telegramId, enabled)
    }

    fun isServerPushoverEnabled(): Boolean = pushoverService.isEnabled()

    fun sendTestNotification(
        telegramId: Long,
        priority: NotificationPriority = NotificationPriority.NORMAL,
    ): SendResult {
        if (!pushoverService.isEnabled()) return SendResult.Disabled
        val config = getPushoverConfig(telegramId) ?: return SendResult.Failed
        if (!config.enabled) return SendResult.UserDisabled
        val pushoverPriority = PushoverService.Priority.entries.find { it.value == priority.value }
            ?: PushoverService.Priority.NORMAL
        return if (pushoverService.sendWithPriority(
                config.userKey,
                "Тестовое уведомление от Notifier",
                "Тест",
                pushoverPriority,
            )
        ) {
            SendResult.Sent
        } else {
            SendResult.Failed
        }
    }

    sealed interface SendResult {
        data object Sent : SendResult

        data object Failed : SendResult

        data object Disabled : SendResult

        data object UserDisabled : SendResult
    }
}
