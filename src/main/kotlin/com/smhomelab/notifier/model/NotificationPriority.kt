package com.smhomelab.notifier.model

enum class NotificationPriority(
    val value: Int,
) {
    LOWEST(-2),
    LOW(-1),
    NORMAL(0),
    HIGH(1),
    EMERGENCY(2),
}
