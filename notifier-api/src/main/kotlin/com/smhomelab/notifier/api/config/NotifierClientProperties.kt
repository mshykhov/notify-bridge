package com.smhomelab.notifier.api.config

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

@ConfigurationProperties(prefix = "smhomelab.notifier")
data class NotifierClientProperties(
    val enabled: Boolean = true,
    val baseUrl: String = "",
    val connectTimeout: Duration = Duration.ofSeconds(10),
    val readTimeout: Duration = Duration.ofSeconds(30),
    val oauth2: NotifierOAuth2Properties = NotifierOAuth2Properties(),
    val retry: NotifierRetryProperties = NotifierRetryProperties(),
)

data class NotifierOAuth2Properties(
    val enabled: Boolean = true,
    val tokenUri: String = "",
    val clientId: String = "",
    val clientSecret: String = "",
    val audience: String = "",
    val scopes: Set<String> = setOf("send:telegram", "send:pushover", "read:limits"),
)

data class NotifierRetryProperties(
    val enabled: Boolean = true,
    val maxAttempts: Int = 3,
    val baseDelay: Duration = Duration.ofMillis(500),
    val multiplier: Double = 2.0,
    val retryableStatusCodes: Set<Int> = setOf(408, 429, 500, 502, 503, 504),
)
