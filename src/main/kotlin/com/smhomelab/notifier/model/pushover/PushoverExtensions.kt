package com.smhomelab.notifier.model.pushover

import com.smhomelab.notifier.api.NotificationPriority
import com.smhomelab.notifier.api.PushoverSound

val NotificationPriority.botDisplayName: String
    get() = when (this) {
        NotificationPriority.LOWEST -> "Без звука"
        NotificationPriority.LOW -> "Тихо"
        NotificationPriority.NORMAL -> "Обычно"
        NotificationPriority.HIGH -> "Важно"
        NotificationPriority.EMERGENCY -> "Экстренно"
    }

val PushoverSound.botDisplayName: String
    get() = name.lowercase().replaceFirstChar { it.uppercase() }.replace('_', ' ')
