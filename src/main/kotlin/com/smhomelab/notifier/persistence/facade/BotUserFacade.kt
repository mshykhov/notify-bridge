package com.smhomelab.notifier.persistence.facade

import com.smhomelab.notifier.persistence.model.BotUserEntity
import com.smhomelab.notifier.persistence.model.UserRole
import com.smhomelab.notifier.persistence.repository.BotUserRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
@Transactional
class BotUserFacade(
    private val botUserRepository: BotUserRepository,
) {
    fun findByTelegramId(telegramId: Long): BotUserEntity? =
        botUserRepository.findByTelegramId(telegramId)

    fun existsByTelegramId(telegramId: Long): Boolean =
        botUserRepository.existsByTelegramId(telegramId)

    fun existsByUsername(username: String): Boolean =
        botUserRepository.existsByUsernameIgnoreCase(username.lowercase())

    fun findAll(): List<BotUserEntity> =
        botUserRepository.findAll()

    fun create(
        telegramId: Long,
        role: UserRole,
        username: String? = null,
        firstName: String? = null,
        lastName: String? = null,
    ): BotUserEntity = botUserRepository.save(
        BotUserEntity(
            telegramId = telegramId,
            role = role,
            username = username,
            firstName = firstName,
            lastName = lastName,
        ),
    )

    fun delete(telegramId: Long) {
        botUserRepository.deleteByTelegramId(telegramId)
    }

    fun updateUserInfo(telegramId: Long, username: String?, firstName: String?, lastName: String?) {
        val user = botUserRepository.findByTelegramId(telegramId) ?: return
        user.username = username
        user.firstName = firstName
        user.lastName = lastName
        botUserRepository.save(user)
    }

    fun updateUserInfoIfChanged(telegramId: Long, username: String?, firstName: String?, lastName: String?) {
        val user = botUserRepository.findByTelegramId(telegramId) ?: return
        if (user.username == username && user.firstName == firstName && user.lastName == lastName) {
            return
        }
        user.username = username
        user.firstName = firstName
        user.lastName = lastName
        botUserRepository.save(user)
    }
}
