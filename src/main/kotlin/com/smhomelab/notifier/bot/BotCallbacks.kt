package com.smhomelab.notifier.bot

import com.smhomelab.notifier.persistence.model.UserRole

enum class BotCallbacks(
    val callback: String,
    val requiredRole: UserRole = UserRole.USER
) {
    // User - Settings
    SETTINGS_NOTIFICATIONS("settings:notifications"),
    SETTINGS_QUIET_HOURS("settings:quiet_hours"),
    SETTINGS_CANCEL("settings:cancel"),
    NOTIFY_ALL("notify:all"),
    NOTIFY_IMPORTANT("notify:important"),
    NOTIFY_OFF("notify:off"),

    // Admin
    ADMIN_ADD_BY_ID("admin:add:by_id", UserRole.ADMIN),
    ADMIN_ADD_BY_USERNAME("admin:add:by_username", UserRole.ADMIN),
    ADMIN_CANCEL("admin:cancel", UserRole.ADMIN),
    ROLE_USER("role:USER", UserRole.ADMIN),
    ROLE_ADMIN("role:ADMIN", UserRole.ADMIN),
    REMOVE_USER("remove:user", UserRole.ADMIN),
    REMOVE_INVITE("remove:invite", UserRole.ADMIN);
}
