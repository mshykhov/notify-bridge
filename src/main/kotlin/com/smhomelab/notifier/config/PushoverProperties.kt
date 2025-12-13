package com.smhomelab.notifier.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "notifier.pushover")
data class PushoverProperties(
    val enabled: Boolean = false,
    val apiToken: String = "",
)
