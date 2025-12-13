package com.smhomelab.notifier.persistence.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import java.time.Instant

@Entity
@Table(name = "user_pushover_config")
class UserPushoverConfig(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    @Column(name = "user_id", unique = true, nullable = false)
    val userId: Long,
    @Column(name = "user_key", length = 30, nullable = false)
    var userKey: String,
    @Column(nullable = false)
    var enabled: Boolean = true,
    @Column(name = "default_priority", nullable = false)
    var defaultPriority: Int = 0,
    @Column(length = 50)
    var sound: String? = null,
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant? = null,
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant? = null,
)
