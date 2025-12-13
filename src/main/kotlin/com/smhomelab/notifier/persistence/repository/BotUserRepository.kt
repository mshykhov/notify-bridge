package com.smhomelab.notifier.persistence.repository

import com.smhomelab.notifier.persistence.model.BotUserEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
interface BotUserRepository : JpaRepository<BotUserEntity, Long> {
    fun findByTelegramId(telegramId: Long): BotUserEntity?

    fun findByUsernameIgnoreCase(username: String): BotUserEntity?

    fun existsByTelegramId(telegramId: Long): Boolean

    fun existsByUsernameIgnoreCase(username: String): Boolean

    @Modifying
    @Transactional
    fun deleteByTelegramId(telegramId: Long)
}
