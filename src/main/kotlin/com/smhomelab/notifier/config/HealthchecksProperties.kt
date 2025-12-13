package com.smhomelab.notifier.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "notifier.healthchecks")
data class HealthchecksProperties(
    val enabled: Boolean = false,
    val pingUrl: String = "",
)
