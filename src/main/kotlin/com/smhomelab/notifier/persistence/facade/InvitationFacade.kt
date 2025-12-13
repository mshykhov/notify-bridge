package com.smhomelab.notifier.persistence.facade

import com.smhomelab.notifier.persistence.model.BotUserInvitation
import com.smhomelab.notifier.persistence.model.UserRole
import com.smhomelab.notifier.persistence.repository.BotUserInvitationRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
@Transactional
class InvitationFacade(
    private val invitationRepository: BotUserInvitationRepository,
) {
    fun create(username: String, role: UserRole, createdBy: Long): BotUserInvitation =
        invitationRepository.save(
            BotUserInvitation(
                username = username.lowercase(),
                role = role,
                createdBy = createdBy,
            ),
        )

    fun findByUsername(username: String): BotUserInvitation? =
        invitationRepository.findByUsernameIgnoreCase(username.lowercase())

    fun existsByUsername(username: String): Boolean =
        invitationRepository.existsByUsernameIgnoreCase(username.lowercase())

    fun delete(username: String) {
        invitationRepository.deleteByUsernameIgnoreCase(username.lowercase())
    }

    fun findAll(): List<BotUserInvitation> =
        invitationRepository.findAll()
}
