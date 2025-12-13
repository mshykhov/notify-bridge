package com.smhomelab.notifier.service

import com.smhomelab.notifier.config.AdminProperties
import com.smhomelab.notifier.persistence.facade.BotUserFacade
import com.smhomelab.notifier.persistence.model.BotUserEntity
import com.smhomelab.notifier.persistence.model.UserRole
import org.springframework.stereotype.Service

@Service
class AuthorizationService(
    private val botUserFacade: BotUserFacade,
    private val adminProperties: AdminProperties,
) {
    fun isAuthorized(telegramId: Long, requiredRole: UserRole?): Boolean {
        if (telegramId == adminProperties.masterAdminId) return true
        if (requiredRole == null) return true

        val user = botUserFacade.findByTelegramId(telegramId) ?: return false
        return user.role.hasPermission(requiredRole)
    }

    fun getUser(telegramId: Long): BotUserEntity? =
        botUserFacade.findByTelegramId(telegramId)

    fun isMasterAdmin(telegramId: Long): Boolean =
        telegramId == adminProperties.masterAdminId
}
