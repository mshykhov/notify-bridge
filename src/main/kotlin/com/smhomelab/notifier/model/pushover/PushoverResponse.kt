package com.smhomelab.notifier.model.pushover

data class PushoverResponse(
    val status: Int,
    val request: String?,
    val errors: List<String>? = null,
)
