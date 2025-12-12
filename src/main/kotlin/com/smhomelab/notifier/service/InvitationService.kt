package com.smhomelab.notifier.service

import com.smhomelab.notifier.persistence.facade.InvitationFacade
import com.smhomelab.notifier.persistence.model.BotUserInvitation
import com.smhomelab.notifier.persistence.model.UserRole
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class InvitationService(
    private val invitationFacade: InvitationFacade
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun create(username: String, role: UserRole, createdBy: Long): BotUserInvitation {
        val invitation = invitationFacade.create(username, role, createdBy)
        log.info("Invitation created: @$username, role=$role, by=$createdBy")
        return invitation
    }

    fun findByUsername(username: String): BotUserInvitation? =
        invitationFacade.findByUsername(username)

    fun existsByUsername(username: String): Boolean =
        invitationFacade.existsByUsername(username)

    fun delete(username: String) {
        invitationFacade.delete(username)
        log.info("Invitation deleted: @$username")
    }

    fun getAll(): List<BotUserInvitation> =
        invitationFacade.findAll()
}
