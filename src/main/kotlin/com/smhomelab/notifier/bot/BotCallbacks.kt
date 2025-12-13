package com.smhomelab.notifier.bot

import com.smhomelab.notifier.persistence.model.UserRole

enum class BotCallbacks(
    val callback: String,
    val requiredRole: UserRole = UserRole.USER,
) {
    // User - Settings
    SETTINGS_PUSHOVER("settings:pushover"),
    PUSHOVER_CONFIGURE("pushover:configure"),
    PUSHOVER_TEST("pushover:test"),
    PUSHOVER_REMOVE("pushover:remove"),
    SETTINGS_BACK("settings:back"),

    // Admin
    ADMIN_ADD_BY_ID("admin:add:by_id", UserRole.ADMIN),
    ADMIN_ADD_BY_USERNAME("admin:add:by_username", UserRole.ADMIN),
    ADMIN_CANCEL("admin:cancel", UserRole.ADMIN),
    ROLE_USER("role:USER", UserRole.ADMIN),
    ROLE_ADMIN("role:ADMIN", UserRole.ADMIN),
    REMOVE_USER("remove:user", UserRole.ADMIN),
    REMOVE_INVITE("remove:invite", UserRole.ADMIN),
}
