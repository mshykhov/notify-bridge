package com.smhomelab.notifier.pushover

import com.smhomelab.notifier.config.PushoverProperties
import org.springframework.stereotype.Service

@Service
class PushoverService(
    private val client: PushoverClient,
    private val properties: PushoverProperties
) {
    fun isEnabled(): Boolean = properties.enabled

    fun send(userKey: String, message: String, title: String? = null): Boolean {
        val response = client.sendMessage(
            PushoverRequest(userKey = userKey, message = message, title = title)
        )
        return response.status == 1
    }

    fun sendWithPriority(
        userKey: String,
        message: String,
        title: String? = null,
        priority: Priority = Priority.NORMAL
    ): Boolean {
        val response = client.sendMessage(
            PushoverRequest(userKey = userKey, message = message, title = title, priority = priority.value)
        )
        return response.status == 1
    }

    enum class Priority(val value: Int) {
        LOWEST(-2),
        LOW(-1),
        NORMAL(0),
        HIGH(1),
        EMERGENCY(2)
    }
}
