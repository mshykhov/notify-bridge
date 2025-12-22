package com.smhomelab.notifier.api.model

data class LimitsResponse(
    val pushover: PushoverLimitsInfo,
    val telegram: TelegramLimitsInfo,
)

data class PushoverLimitsInfo(
    val enabled: Boolean,
    val limit: Int? = null,
    val remaining: Int? = null,
    val resetAt: String? = null,
)

data class TelegramLimitsInfo(
    val enabled: Boolean,
    val rateLimitEnabled: Boolean,
    val globalRequestsPerSecond: Double,
    val perChatRequestsPerSecond: Double,
)
