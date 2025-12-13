package com.smhomelab.notifier.service

import com.smhomelab.notifier.config.AdminProperties
import com.smhomelab.notifier.persistence.facade.BotUserFacade
import com.smhomelab.notifier.persistence.model.BotUserEntity
import com.smhomelab.notifier.persistence.model.UserRole
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service

private val logger = KotlinLogging.logger {}

@Service
class AuthorizationService(
    private val botUserFacade: BotUserFacade,
    private val adminProperties: AdminProperties,
) {
    fun isAuthorized(telegramId: Long, requiredRole: UserRole?): Boolean {
        if (telegramId == adminProperties.masterAdminId) return true
        if (requiredRole == null) return true

        val user = botUserFacade.findByTelegramId(telegramId)
        if (user == null) {
            logger.debug { "Authorization denied: user not found telegramId=$telegramId" }
            return false
        }
        val authorized = user.role.hasPermission(requiredRole)
        if (!authorized) {
            logger.debug { "Authorization denied: telegramId=$telegramId, role=${user.role}, required=$requiredRole" }
        }
        return authorized
    }

    fun getUser(telegramId: Long): BotUserEntity? =
        botUserFacade.findByTelegramId(telegramId)

    fun isMasterAdmin(telegramId: Long): Boolean =
        telegramId == adminProperties.masterAdminId
}
