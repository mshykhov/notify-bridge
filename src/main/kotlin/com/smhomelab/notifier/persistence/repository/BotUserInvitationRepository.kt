package com.smhomelab.notifier.persistence.repository

import com.smhomelab.notifier.persistence.model.BotUserInvitation
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
interface BotUserInvitationRepository : JpaRepository<BotUserInvitation, Long> {
    fun findByUsernameIgnoreCase(username: String): BotUserInvitation?

    fun existsByUsernameIgnoreCase(username: String): Boolean

    @Modifying
    @Transactional
    fun deleteByUsernameIgnoreCase(username: String)
}
