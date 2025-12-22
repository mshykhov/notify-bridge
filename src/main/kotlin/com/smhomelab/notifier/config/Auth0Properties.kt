package com.smhomelab.notifier.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "notifier.auth0")
data class Auth0Properties(
    val enabled: Boolean = true,
    val issuer: String = "",
    val audience: String = "",
)
