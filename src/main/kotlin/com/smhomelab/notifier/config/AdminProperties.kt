package com.smhomelab.notifier.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "notifier.admin")
data class AdminProperties(
    val masterAdminId: Long
)
