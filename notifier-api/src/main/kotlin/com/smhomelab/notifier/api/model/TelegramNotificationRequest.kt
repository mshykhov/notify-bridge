package com.smhomelab.notifier.api.model

data class TelegramNotificationRequest(
    val message: String,
    val parseMode: String? = null,
    val disableNotification: Boolean = false,
) {
    init {
        require(message.isNotBlank()) { "Message cannot be blank" }
        require(message.length <= MAX_MESSAGE_LENGTH) {
            "Message exceeds $MAX_MESSAGE_LENGTH characters"
        }
        parseMode?.let {
            require(it in VALID_PARSE_MODES) {
                "Invalid parseMode: $it. Valid values: $VALID_PARSE_MODES"
            }
        }
    }

    companion object {
        const val MAX_MESSAGE_LENGTH = 4096
        val VALID_PARSE_MODES = setOf("HTML", "Markdown", "MarkdownV2")
    }
}
