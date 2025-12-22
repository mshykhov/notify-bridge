package com.smhomelab.notifier.api.model

import com.smhomelab.notifier.api.NotificationPriority
import com.smhomelab.notifier.api.PushoverSound

data class PushoverNotificationRequest(
    val message: String,
    val title: String? = null,
    val priority: NotificationPriority = NotificationPriority.NORMAL,
    val sound: PushoverSound? = null,
    val url: String? = null,
    val urlTitle: String? = null,
    val html: Boolean = false,
    val ttl: Int? = null,
) {
    init {
        require(message.isNotBlank()) { "Message cannot be blank" }
        require(message.length <= MAX_MESSAGE_LENGTH) {
            "Message exceeds $MAX_MESSAGE_LENGTH characters"
        }
        title?.let {
            require(it.length <= MAX_TITLE_LENGTH) {
                "Title exceeds $MAX_TITLE_LENGTH characters"
            }
        }
        url?.let {
            require(it.length <= MAX_URL_LENGTH) {
                "URL exceeds $MAX_URL_LENGTH characters"
            }
        }
        urlTitle?.let {
            require(it.length <= MAX_URL_TITLE_LENGTH) {
                "URL title exceeds $MAX_URL_TITLE_LENGTH characters"
            }
        }
    }

    companion object {
        const val MAX_MESSAGE_LENGTH = 1024
        const val MAX_TITLE_LENGTH = 250
        const val MAX_URL_LENGTH = 512
        const val MAX_URL_TITLE_LENGTH = 100
    }
}
