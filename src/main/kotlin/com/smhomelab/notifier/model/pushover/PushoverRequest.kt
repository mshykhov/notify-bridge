package com.smhomelab.notifier.model.pushover

import com.smhomelab.notifier.api.NotificationPriority
import com.smhomelab.notifier.api.PushoverSound

data class PushoverRequest(
    val userKey: String,
    val message: String,
    val title: String? = null,
    val priority: NotificationPriority = NotificationPriority.NORMAL,
    val sound: PushoverSound? = null,
    val url: String? = null,
    val urlTitle: String? = null,
    val html: Boolean = false,
    val monospace: Boolean = false,
    val device: String? = null,
    val timestamp: Long? = null,
    val ttl: Int? = null,
    val retry: Int? = null,
    val expire: Int? = null,
    val callback: String? = null,
    val tags: String? = null,
) {
    init {
        require(message.length <= MAX_MESSAGE_LENGTH) { "Message exceeds $MAX_MESSAGE_LENGTH chars" }
        title?.let { require(it.length <= MAX_TITLE_LENGTH) { "Title exceeds $MAX_TITLE_LENGTH chars" } }
        url?.let { require(it.length <= MAX_URL_LENGTH) { "URL exceeds $MAX_URL_LENGTH chars" } }
        urlTitle?.let { require(it.length <= MAX_URL_TITLE_LENGTH) { "URL title exceeds $MAX_URL_TITLE_LENGTH chars" } }
        require(!(html && monospace)) { "Cannot use both html and monospace" }

        if (priority == NotificationPriority.EMERGENCY) {
            requireNotNull(retry) { "retry required for emergency priority" }
            requireNotNull(expire) { "expire required for emergency priority" }
            require(retry >= MIN_RETRY_SECONDS) { "retry must be >= $MIN_RETRY_SECONDS seconds" }
            require(expire <= MAX_EXPIRE_SECONDS) { "expire must be <= $MAX_EXPIRE_SECONDS seconds" }
        }
    }

    companion object {
        const val MAX_MESSAGE_LENGTH = 1024
        const val MAX_TITLE_LENGTH = 250
        const val MAX_URL_LENGTH = 512
        const val MAX_URL_TITLE_LENGTH = 100
        const val MIN_RETRY_SECONDS = 30
        const val MAX_EXPIRE_SECONDS = 10800
    }
}
