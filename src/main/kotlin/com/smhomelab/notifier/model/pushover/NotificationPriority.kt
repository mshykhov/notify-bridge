package com.smhomelab.notifier.model.pushover

enum class NotificationPriority(
    val value: Int,
    val displayName: String,
) {
    LOWEST(-2, "Без звука"),
    LOW(-1, "Тихо"),
    NORMAL(0, "Обычно"),
    HIGH(1, "Важно"),
    EMERGENCY(2, "Экстренно"),
}
