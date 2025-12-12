package com.smhomelab.notifier.service

import com.smhomelab.notifier.config.AdminProperties
import com.smhomelab.notifier.model.BotUserEntity
import com.smhomelab.notifier.model.BotUserInvitation
import com.smhomelab.notifier.model.UserRole
import com.smhomelab.notifier.repository.BotUserRepository
import io.github.dehuckakpyt.telegrambot.TelegramBot
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class BotUserService(
    private val botUserRepository: BotUserRepository,
    private val invitationService: InvitationService,
    private val botCommandMenuService: BotCommandMenuService,
    private val adminProperties: AdminProperties,
    private val telegramBot: TelegramBot
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    fun getAllUsers(): List<BotUserEntity> =
        botUserRepository.findAll()
            .filter { it.telegramId != adminProperties.masterAdminId }

    fun existsByTelegramId(telegramId: Long): Boolean =
        botUserRepository.existsByTelegramId(telegramId)

    fun existsByUsername(username: String): Boolean =
        botUserRepository.existsByUsernameIgnoreCase(username.lowercase())

    fun create(
        telegramId: Long,
        role: UserRole,
        username: String? = null,
        firstName: String? = null,
        lastName: String? = null
    ): BotUserEntity {
        val user = botUserRepository.save(
            BotUserEntity(
                telegramId = telegramId,
                role = role,
                username = username,
                firstName = firstName,
                lastName = lastName
            )
        )

        scope.launch { botCommandMenuService.updateCommandsForUser(telegramId, role) }
        scope.launch { notifyUserAboutAccess(telegramId) }

        log.info("User created: telegramId=$telegramId, role=$role")
        return user
    }

    fun activateFromInvitation(
        invitation: BotUserInvitation,
        telegramId: Long,
        firstName: String?,
        lastName: String?
    ): BotUserEntity {
        val user = botUserRepository.save(
            BotUserEntity(
                telegramId = telegramId,
                role = invitation.role,
                username = invitation.username,
                firstName = firstName,
                lastName = lastName
            )
        )

        invitationService.delete(invitation.username)
        scope.launch { botCommandMenuService.updateCommandsForUser(telegramId, invitation.role) }

        log.info("User activated from invitation: @${invitation.username}, telegramId=$telegramId")
        return user
    }

    fun delete(telegramId: Long) {
        botUserRepository.deleteByTelegramId(telegramId)
        scope.launch { botCommandMenuService.resetCommandsForUser(telegramId) }
        log.info("User deleted: telegramId=$telegramId")
    }

    fun updateUserInfo(telegramId: Long, username: String?, firstName: String?, lastName: String?) {
        val user = botUserRepository.findByTelegramId(telegramId) ?: return
        user.apply {
            this.username = username
            this.firstName = firstName
            this.lastName = lastName
        }
        botUserRepository.save(user)
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
