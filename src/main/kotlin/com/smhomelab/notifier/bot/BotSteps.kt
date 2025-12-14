package com.smhomelab.notifier.bot

import com.smhomelab.notifier.persistence.model.UserRole

enum class BotSteps(
    val step: String,
    val requiredRole: UserRole = UserRole.USER,
) {
    // User - Pushover
    GET_PUSHOVER_KEY("get_pushover_key"),

    // User - Timezone
    GET_TIMEZONE("get_timezone"),

    // Admin
    GET_USER_ID("get_user_id", UserRole.ADMIN),
    GET_USERNAME("get_username", UserRole.ADMIN),
}
