package com.smhomelab.notifier.persistence.repository

import com.smhomelab.notifier.persistence.model.UserPushoverConfig
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
interface UserPushoverConfigRepository : JpaRepository<UserPushoverConfig, Long> {
    fun findByUserId(userId: Long): UserPushoverConfig?

    fun existsByUserId(userId: Long): Boolean

    @Modifying
    @Transactional
    fun deleteByUserId(userId: Long)
}
