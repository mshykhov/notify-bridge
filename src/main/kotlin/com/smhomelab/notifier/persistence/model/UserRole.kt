package com.smhomelab.notifier.persistence.model

enum class UserRole(
    val level: Int,
) {
    USER(1),
    ADMIN(100),
    ;

    fun hasPermission(required: UserRole): Boolean = level >= required.level
}
