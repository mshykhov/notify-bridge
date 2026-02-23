package com.smhomelab.notifier.persistence.facade

import com.smhomelab.notifier.persistence.model.BotUserInvitation
import com.smhomelab.notifier.persistence.model.UserRole
import com.smhomelab.notifier.persistence.repository.BotUserInvitationRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class InvitationFacade(
    private val invitationRepository: BotUserInvitationRepository,
) {
    @Transactional
    fun create(username: String, role: UserRole, createdBy: Long): BotUserInvitation =
        invitationRepository.save(
            BotUserInvitation(
                username = username.lowercase(),
                role = role,
                createdBy = createdBy,
            ),
        )

    @Transactional(readOnly = true)
    fun findByUsername(username: String): BotUserInvitation? =
        invitationRepository.findByUsernameIgnoreCase(username.lowercase())

    @Transactional(readOnly = true)
    fun existsByUsername(username: String): Boolean =
        invitationRepository.existsByUsernameIgnoreCase(username.lowercase())

    @Transactional
    fun delete(username: String) {
        invitationRepository.deleteByUsernameIgnoreCase(username.lowercase())
    }

    @Transactional(readOnly = true)
    fun findAll(): List<BotUserInvitation> =
        invitationRepository.findAll()
}
