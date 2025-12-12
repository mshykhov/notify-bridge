package com.smhomelab.notifier.service

import com.smhomelab.notifier.config.AdminProperties
import com.smhomelab.notifier.model.BotUserEntity
import com.smhomelab.notifier.model.UserRole
import com.smhomelab.notifier.repository.BotUserRepository
import org.springframework.stereotype.Service

@Service
class AuthorizationService(
    private val botUserRepository: BotUserRepository,
    private val adminProperties: AdminProperties
) {
    fun isAuthorized(telegramId: Long, requiredRole: UserRole?): Boolean {
        if (telegramId == adminProperties.masterAdminId) return true
        if (requiredRole == null) return true

        val user = botUserRepository.findByTelegramId(telegramId) ?: return false
        return user.role.hasPermission(requiredRole)
    }

    fun getUser(telegramId: Long): BotUserEntity? =
        botUserRepository.findByTelegramId(telegramId)

    fun isMasterAdmin(telegramId: Long): Boolean =
        telegramId == adminProperties.masterAdminId
}
