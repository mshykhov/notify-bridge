package com.smhomelab.notifier.service

import com.smhomelab.notifier.config.AdminProperties
import com.smhomelab.notifier.persistence.facade.BotUserFacade
import com.smhomelab.notifier.persistence.model.BotUserEntity
import com.smhomelab.notifier.persistence.model.BotUserInvitation
import com.smhomelab.notifier.persistence.model.UserRole
import io.github.dehuckakpyt.telegrambot.TelegramBot
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class UserService(
    private val botUserFacade: BotUserFacade,
    private val invitationService: InvitationService,
    private val botCommandMenuService: BotCommandMenuService,
    private val adminProperties: AdminProperties,
    private val telegramBot: TelegramBot
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun getAllUsers(): List<BotUserEntity> =
        botUserFacade.findAll().filter { it.telegramId != adminProperties.masterAdminId }

    fun ensureMasterAdminExists() {
        val masterAdminId = adminProperties.masterAdminId
        if (!botUserFacade.existsByTelegramId(masterAdminId)) {
            botUserFacade.create(masterAdminId, UserRole.ADMIN)
            log.info("Master admin created: telegramId=$masterAdminId")
        }
    }

    fun existsByTelegramId(telegramId: Long): Boolean =
        botUserFacade.existsByTelegramId(telegramId)

    fun existsByUsername(username: String): Boolean =
        botUserFacade.existsByUsername(username)

    suspend fun createUser(
        telegramId: Long,
        role: UserRole,
        username: String? = null,
        firstName: String? = null,
        lastName: String? = null
    ): BotUserEntity {
        val user = botUserFacade.create(telegramId, role, username, firstName, lastName)
        botCommandMenuService.updateCommandsForUser(user.telegramId, user.role)
        notifyUserAboutAccess(user.telegramId)
        log.info("User created: telegramId=$telegramId, role=$role")
        return user
    }

    suspend fun activateFromInvitation(
        invitation: BotUserInvitation,
        telegramId: Long,
        firstName: String?,
        lastName: String?
    ): BotUserEntity {
        val user = botUserFacade.create(
            telegramId = telegramId,
            role = invitation.role,
            username = invitation.username,
            firstName = firstName,
            lastName = lastName
        )
        invitationService.delete(invitation.username)
        botCommandMenuService.updateCommandsForUser(user.telegramId, user.role)
        log.info("User activated from invitation: @${invitation.username}, telegramId=$telegramId")
        return user
    }

    suspend fun deleteUser(telegramId: Long) {
        botUserFacade.delete(telegramId)
        botCommandMenuService.resetCommandsForUser(telegramId)
        log.info("User deleted: telegramId=$telegramId")
    }

    fun syncUserData(telegramId: Long, username: String?, firstName: String?, lastName: String?) {
        botUserFacade.updateUserInfoIfChanged(telegramId, username, firstName, lastName)
    }

    fun updateUserInfo(telegramId: Long, username: String?, firstName: String?, lastName: String?) {
        botUserFacade.updateUserInfo(telegramId, username, firstName, lastName)
    }

    fun cleanupOrphanedInvitation(username: String?) {
        if (username != null && invitationService.existsByUsername(username)) {
            invitationService.delete(username)
            log.info("Cleaned up orphaned invitation: @$username")
        }
    }

    private suspend fun notifyUserAboutAccess(telegramId: Long) {
        try {
            telegramBot.sendMessage(
                chatId = telegramId,
                text = "Тебе предоставлен доступ к боту. Используй /start"
            )
        } catch (e: Exception) {
            log.debug("Could not notify user $telegramId: ${e.message}")
        }
    }
}
