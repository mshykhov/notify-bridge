package com.smhomelab.notifier.persistence.model

import jakarta.persistence.*
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import java.time.Instant

@Entity
@Table(name = "bot_user")
class BotUserEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "telegram_id", unique = true, nullable = false)
    val telegramId: Long,

    @Column(length = 32)
    var username: String? = null,

    @Column(name = "first_name", length = 64)
    var firstName: String? = null,

    @Column(name = "last_name", length = 64)
    var lastName: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(length = 16, nullable = false)
    var role: UserRole = UserRole.USER,

    @Column(name = "pushover_user_key", length = 30)
    var pushoverUserKey: String? = null,

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant? = null,

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant? = null
)
