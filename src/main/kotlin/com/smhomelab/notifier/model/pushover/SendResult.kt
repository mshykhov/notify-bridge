package com.smhomelab.notifier.model.pushover

sealed interface SendResult {
    data class Sent(
        val priority: NotificationPriority,
    ) : SendResult

    data object Failed : SendResult

    data object Disabled : SendResult

    data object UserDisabled : SendResult
}
