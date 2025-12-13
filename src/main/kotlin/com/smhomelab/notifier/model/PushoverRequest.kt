package com.smhomelab.notifier.model

data class PushoverRequest(
    val userKey: String,
    val message: String,
    val title: String? = null,
    val priority: Int? = null,
    val sound: String? = null,
    val url: String? = null,
    val urlTitle: String? = null,
    val html: Boolean = false,
)
