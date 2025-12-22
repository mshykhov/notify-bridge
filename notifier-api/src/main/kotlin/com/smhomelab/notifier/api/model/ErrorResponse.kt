package com.smhomelab.notifier.api.model

import java.time.Instant

data class ErrorResponse(
    val error: String,
    val message: String,
    val details: Map<String, String>? = null,
    val timestamp: String = Instant.now().toString(),
    val path: String? = null,
)
