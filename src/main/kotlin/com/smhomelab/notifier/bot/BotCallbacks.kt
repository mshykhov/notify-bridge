package com.smhomelab.notifier.bot

import com.smhomelab.notifier.persistence.model.UserRole

enum class BotCallbacks(
    val callback: String,
    val requiredRole: UserRole = UserRole.USER,
) {
    // User - Settings
    SETTINGS_PUSHOVER("settings:pushover"),
    SETTINGS_TIMEZONE("settings:timezone"),
    TIMEZONE_CONFIGURE("timezone:configure"),
    TIMEZONE_CANCEL("timezone:cancel"),
    PUSHOVER_CONFIGURE("pushover:configure"),
    PUSHOVER_CANCEL("pushover:cancel"),
    PUSHOVER_TOGGLE("pushover:toggle"),
    PUSHOVER_TEST_MENU("pushover:test:menu"),
    PUSHOVER_TEST_LOWEST("pushover:test:lowest"),
    PUSHOVER_TEST_LOW("pushover:test:low"),
    PUSHOVER_TEST_NORMAL("pushover:test:normal"),
    PUSHOVER_TEST_HIGH("pushover:test:high"),
    PUSHOVER_TEST_EMERGENCY("pushover:test:emergency"),
    PUSHOVER_REMOVE("pushover:remove"),
    PUSHOVER_REMOVE_CONFIRM("pushover:remove:confirm"),
    SETTINGS_BACK("settings:back"),

    // Admin
    ADMIN_ADD_BY_ID("admin:add:by_id", UserRole.ADMIN),
    ADMIN_ADD_BY_USERNAME("admin:add:by_username", UserRole.ADMIN),
    ADMIN_CANCEL("admin:cancel", UserRole.ADMIN),
    ADMIN_REFRESH("admin:refresh", UserRole.ADMIN),
    ROLE_USER("role:USER", UserRole.ADMIN),
    ROLE_ADMIN("role:ADMIN", UserRole.ADMIN),
    REMOVE_USER("remove:user", UserRole.ADMIN),
    REMOVE_INVITE("remove:invite", UserRole.ADMIN),
}
