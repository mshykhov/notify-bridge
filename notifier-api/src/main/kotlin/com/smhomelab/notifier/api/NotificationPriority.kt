package com.smhomelab.notifier.api

enum class NotificationPriority(
    val value: Int,
    val displayName: String,
) {
    LOWEST(-2, "Lowest"),
    LOW(-1, "Low"),
    NORMAL(0, "Normal"),
    HIGH(1, "High"),
    EMERGENCY(2, "Emergency"),
}
