package com.smhomelab.notifier.service

import com.smhomelab.notifier.common.ValidationResult
import com.smhomelab.notifier.persistence.facade.BotUserFacade
import com.smhomelab.notifier.pushover.PushoverService
import org.springframework.stereotype.Service

@Service
class SettingsService(
    private val botUserFacade: BotUserFacade,
    private val pushoverService: PushoverService,
) {
    fun isPushoverConfigured(telegramId: Long): Boolean {
        val user = botUserFacade.findByTelegramId(telegramId)
        return user?.pushoverUserKey != null
    }

    fun getPushoverKey(telegramId: Long): String? = botUserFacade.findByTelegramId(telegramId)?.pushoverUserKey

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
        botUserFacade.updatePushoverKey(telegramId, key)
    }

    fun removePushoverKey(telegramId: Long) {
        botUserFacade.updatePushoverKey(telegramId, null)
    }

    fun isPushoverEnabled(): Boolean = pushoverService.isEnabled()

    fun sendTestNotification(telegramId: Long): SendResult {
        if (!pushoverService.isEnabled()) return SendResult.Disabled
        val key = getPushoverKey(telegramId) ?: return SendResult.Failed
        return if (pushoverService.send(key, "Тестовое уведомление от Notifier", "Тест")) {
            SendResult.Sent
        } else {
            SendResult.Failed
        }
    }

    sealed interface SendResult {
        data object Sent : SendResult

        data object Failed : SendResult

        data object Disabled : SendResult
    }
}
