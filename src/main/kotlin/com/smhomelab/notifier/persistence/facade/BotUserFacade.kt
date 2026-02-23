package com.smhomelab.notifier.persistence.facade

import com.smhomelab.notifier.persistence.model.BotUserEntity
import com.smhomelab.notifier.persistence.model.UserRole
import com.smhomelab.notifier.persistence.repository.BotUserRepository
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

private val logger = KotlinLogging.logger {}

@Component
class BotUserFacade(
    private val botUserRepository: BotUserRepository,
) {
    @Transactional(readOnly = true)
    fun findByTelegramId(telegramId: Long): BotUserEntity? =
        botUserRepository.findByTelegramId(telegramId)

    @Transactional(readOnly = true)
    fun existsByTelegramId(telegramId: Long): Boolean =
        botUserRepository.existsByTelegramId(telegramId)

    @Transactional(readOnly = true)
    fun existsByUsername(username: String): Boolean =
        botUserRepository.existsByUsernameIgnoreCase(username.lowercase())

    @Transactional(readOnly = true)
    fun findAll(): List<BotUserEntity> =
        botUserRepository.findAll()

    @Transactional
    fun create(
        telegramId: Long,
        role: UserRole,
        username: String? = null,
        firstName: String? = null,
        lastName: String? = null,
    ): BotUserEntity {
        logger.debug { "Creating user: telegramId=$telegramId, role=$role, username=$username" }
        return botUserRepository.save(
            BotUserEntity(
                telegramId = telegramId,
                role = role,
                username = username,
                firstName = firstName,
                lastName = lastName,
            ),
        )
    }

    @Transactional
    fun delete(telegramId: Long) {
        logger.debug { "Deleting user: telegramId=$telegramId" }
        botUserRepository.deleteByTelegramId(telegramId)
    }

    @Transactional
    fun updateUserInfo(telegramId: Long, username: String?, firstName: String?, lastName: String?) {
        val user = botUserRepository.findByTelegramId(telegramId) ?: return
        user.username = username
        user.firstName = firstName
        user.lastName = lastName
        botUserRepository.save(user)
    }

    @Transactional
    fun updateUserInfoIfChanged(telegramId: Long, username: String?, firstName: String?, lastName: String?) {
        val user = botUserRepository.findByTelegramId(telegramId) ?: return
        if (user.username == username && user.firstName == firstName && user.lastName == lastName) {
            return
        }
        logger.debug { "Updating user info: telegramId=$telegramId, username=$username" }
        user.username = username
        user.firstName = firstName
        user.lastName = lastName
        botUserRepository.save(user)
    }

    @Transactional(readOnly = true)
    fun getTimezone(telegramId: Long): String =
        botUserRepository.findByTelegramId(telegramId)?.timezone ?: "UTC"

    @Transactional
    fun updateTimezone(telegramId: Long, timezone: String) {
        val user = botUserRepository.findByTelegramId(telegramId) ?: return
        logger.debug { "Updating timezone: telegramId=$telegramId, timezone=$timezone" }
        user.timezone = timezone
        botUserRepository.save(user)
    }
}
