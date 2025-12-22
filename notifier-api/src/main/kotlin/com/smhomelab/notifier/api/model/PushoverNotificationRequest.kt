package com.smhomelab.notifier.api.model

import com.smhomelab.notifier.api.NotificationPriority
import com.smhomelab.notifier.api.PushoverSound

data class PushoverNotificationRequest(
    val message: String,
    val title: String? = null,
    val priority: String = "normal",
    val sound: String? = null,
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
        require(priority.lowercase() in VALID_PRIORITIES) {
            "Invalid priority: $priority. Valid values: $VALID_PRIORITIES"
        }
        sound?.let {
            require(PushoverSound.fromValue(it) != null) {
                "Invalid sound: $it. Valid values: ${PushoverSound.entries.map { s -> s.value }}"
            }
        }
    }

    fun getPriority(): NotificationPriority = when (priority.lowercase()) {
        "lowest" -> NotificationPriority.LOWEST
        "low" -> NotificationPriority.LOW
        "high" -> NotificationPriority.HIGH
        "emergency" -> NotificationPriority.EMERGENCY
        else -> NotificationPriority.NORMAL // validated in init, covers "normal"
    }

    fun getSound(): PushoverSound? = sound?.let { PushoverSound.fromValue(it) }

    companion object {
        const val MAX_MESSAGE_LENGTH = 1024
        const val MAX_TITLE_LENGTH = 250
        const val MAX_URL_LENGTH = 512
        const val MAX_URL_TITLE_LENGTH = 100
        val VALID_PRIORITIES = setOf("lowest", "low", "normal", "high", "emergency")
    }
}
