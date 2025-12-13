package com.smhomelab.notifier.persistence.facade

import com.smhomelab.notifier.persistence.model.UserPushoverConfig
import com.smhomelab.notifier.persistence.repository.BotUserRepository
import com.smhomelab.notifier.persistence.repository.UserPushoverConfigRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
@Transactional
class UserPushoverConfigFacade(
    private val pushoverConfigRepository: UserPushoverConfigRepository,
    private val botUserRepository: BotUserRepository,
) {
    fun findByTelegramId(telegramId: Long): UserPushoverConfig? {
        val userId = botUserRepository.findByTelegramId(telegramId)?.id ?: return null
        return pushoverConfigRepository.findByUserId(userId)
    }

    fun existsByTelegramId(telegramId: Long): Boolean {
        val userId = botUserRepository.findByTelegramId(telegramId)?.id ?: return false
        return pushoverConfigRepository.existsByUserId(userId)
    }

    fun create(telegramId: Long, userKey: String): UserPushoverConfig {
        val userId = botUserRepository.findByTelegramId(telegramId)?.id
            ?: throw IllegalArgumentException("User not found: $telegramId")
        return pushoverConfigRepository.save(
            UserPushoverConfig(userId = userId, userKey = userKey),
        )
    }

    fun update(telegramId: Long, block: UserPushoverConfig.() -> Unit): UserPushoverConfig? {
        val config = findByTelegramId(telegramId) ?: return null
        config.block()
        return pushoverConfigRepository.save(config)
    }

    fun updateUserKey(telegramId: Long, userKey: String): UserPushoverConfig? =
        update(telegramId) { this.userKey = userKey }

    fun updateEnabled(telegramId: Long, enabled: Boolean): UserPushoverConfig? =
        update(telegramId) { this.enabled = enabled }

    fun updateSound(telegramId: Long, sound: String?): UserPushoverConfig? =
        update(telegramId) { this.sound = sound }

    fun deleteByTelegramId(telegramId: Long) {
        val userId = botUserRepository.findByTelegramId(telegramId)?.id ?: return
        pushoverConfigRepository.deleteByUserId(userId)
    }
}
