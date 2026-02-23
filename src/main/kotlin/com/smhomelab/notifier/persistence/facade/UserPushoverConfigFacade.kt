package com.smhomelab.notifier.persistence.facade

import com.smhomelab.notifier.persistence.model.UserPushoverConfig
import com.smhomelab.notifier.persistence.repository.BotUserRepository
import com.smhomelab.notifier.persistence.repository.UserPushoverConfigRepository
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

private val logger = KotlinLogging.logger {}

@Component
class UserPushoverConfigFacade(
    private val pushoverConfigRepository: UserPushoverConfigRepository,
    private val botUserRepository: BotUserRepository,
) {
    @Transactional(readOnly = true)
    fun findByTelegramId(telegramId: Long): UserPushoverConfig? {
        val userId = botUserRepository.findByTelegramId(telegramId)?.id ?: return null
        return pushoverConfigRepository.findByUserId(userId)
    }

    @Transactional(readOnly = true)
    fun existsByTelegramId(telegramId: Long): Boolean {
        val userId = botUserRepository.findByTelegramId(telegramId)?.id ?: return false
        return pushoverConfigRepository.existsByUserId(userId)
    }

    @Transactional
    fun create(telegramId: Long, userKey: String): UserPushoverConfig {
        val userId = botUserRepository.findByTelegramId(telegramId)?.id
            ?: throw IllegalArgumentException("User not found: $telegramId")
        logger.debug { "Creating pushover config: telegramId=$telegramId, userId=$userId" }
        return pushoverConfigRepository.save(
            UserPushoverConfig(userId = userId, userKey = userKey),
        )
    }

    @Transactional
    fun update(telegramId: Long, block: UserPushoverConfig.() -> Unit): UserPushoverConfig? {
        val config = findByTelegramId(telegramId) ?: return null
        config.block()
        return pushoverConfigRepository.save(config)
    }

    @Transactional
    fun updateUserKey(telegramId: Long, userKey: String): UserPushoverConfig? =
        update(telegramId) { this.userKey = userKey }

    @Transactional
    fun updateEnabled(telegramId: Long, enabled: Boolean): UserPushoverConfig? =
        update(telegramId) { this.enabled = enabled }

    @Transactional
    fun updateSound(telegramId: Long, sound: String?): UserPushoverConfig? =
        update(telegramId) { this.sound = sound }

    @Transactional
    fun deleteByTelegramId(telegramId: Long) {
        val userId = botUserRepository.findByTelegramId(telegramId)?.id ?: return
        logger.debug { "Deleting pushover config: telegramId=$telegramId, userId=$userId" }
        pushoverConfigRepository.deleteByUserId(userId)
    }
}
