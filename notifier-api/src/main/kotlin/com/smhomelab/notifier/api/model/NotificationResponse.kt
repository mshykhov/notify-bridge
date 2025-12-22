package com.smhomelab.notifier.api.model

import java.time.Instant

data class NotificationResponse(
    val success: Boolean,
    val channel: String,
    val messageId: String? = null,
    val timestamp: String = Instant.now().toString(),
)
