package com.smhomelab.notifier.model

import jakarta.persistence.*
import org.hibernate.annotations.CreationTimestamp
import java.time.Instant

@Entity
@Table(name = "bot_user_invitation")
class BotUserInvitation(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(unique = true, nullable = false, length = 32)
    val username: String,

    @Enumerated(EnumType.STRING)
    @Column(length = 16, nullable = false)
    val role: UserRole = UserRole.USER,

    @Column(name = "created_by", nullable = false)
    val createdBy: Long,

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    val createdAt: Instant? = null
)
