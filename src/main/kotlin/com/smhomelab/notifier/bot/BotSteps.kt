package com.smhomelab.notifier.bot

import com.smhomelab.notifier.persistence.model.UserRole

enum class BotSteps(
    val step: String,
    val requiredRole: UserRole = UserRole.USER
) {
    // User
    GET_QUIET_START("get_quiet_start"),
    GET_QUIET_END("get_quiet_end"),

    // Admin
    GET_USER_ID("get_user_id", UserRole.ADMIN),
    GET_USERNAME("get_username", UserRole.ADMIN);
}
