package com.smhomelab.notifier.service

import com.smhomelab.notifier.model.BotUserInvitation
import com.smhomelab.notifier.model.UserRole
import com.smhomelab.notifier.repository.BotUserInvitationRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class InvitationService(
    private val invitationRepository: BotUserInvitationRepository
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun create(username: String, role: UserRole, createdBy: Long): BotUserInvitation {
        val invitation = invitationRepository.save(
            BotUserInvitation(
                username = username.lowercase(),
                role = role,
                createdBy = createdBy
            )
        )
        log.info("Invitation created: @$username, role=$role, by=$createdBy")
        return invitation
    }

    fun findByUsername(username: String): BotUserInvitation? =
        invitationRepository.findByUsernameIgnoreCase(username.lowercase())

    fun existsByUsername(username: String): Boolean =
        invitationRepository.existsByUsernameIgnoreCase(username.lowercase())

    fun delete(username: String) {
        invitationRepository.deleteByUsernameIgnoreCase(username.lowercase())
        log.info("Invitation deleted: @$username")
    }

    fun deleteById(id: Long) {
        invitationRepository.deleteById(id)
        log.info("Invitation deleted: id=$id")
    }

    fun getAll(): List<BotUserInvitation> =
        invitationRepository.findAll()
}
