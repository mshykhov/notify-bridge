package com.smhomelab.notifier.telegram.ratelimit

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "telegram.rate-limit")
data class RateLimitProperties(
    val enabled: Boolean = true,
    val global: LimitConfig = LimitConfig(requestsPerSecond = 30.0, burst = 5),
    val perChat: LimitConfig = LimitConfig(requestsPerSecond = 1.0, burst = 2),
    val retry: RetryConfig = RetryConfig(),
)

data class LimitConfig(
    val requestsPerSecond: Double = 30.0,
    val burst: Int = 5,
)

data class RetryConfig(
    val maxAttempts: Int = 3,
    val baseDelayMs: Long = 1000,
)
