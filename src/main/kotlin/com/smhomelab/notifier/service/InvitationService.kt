package com.smhomelab.notifier.service

import com.smhomelab.notifier.persistence.facade.InvitationFacade
import com.smhomelab.notifier.persistence.model.BotUserInvitation
import com.smhomelab.notifier.persistence.model.UserRole
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service

private val logger = KotlinLogging.logger {}

@Service
class InvitationService(
    private val invitationFacade: InvitationFacade,
) {
    fun create(username: String, role: UserRole, createdBy: Long): BotUserInvitation {
        val invitation = invitationFacade.create(username, role, createdBy)
        logger.info { "Invitation created: @$username, role=$role, by=$createdBy" }
        return invitation
    }

    fun findByUsername(username: String): BotUserInvitation? =
        invitationFacade.findByUsername(username)

    fun existsByUsername(username: String): Boolean =
        invitationFacade.existsByUsername(username)

    fun delete(username: String) {
        invitationFacade.delete(username)
        logger.info { "Invitation deleted: @$username" }
    }

    fun getAll(): List<BotUserInvitation> =
        invitationFacade.findAll()
}
