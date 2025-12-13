package com.smhomelab.notifier.pushover

import com.smhomelab.notifier.config.PushoverProperties
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.annotation.PostConstruct
import org.springframework.stereotype.Service

private val logger = KotlinLogging.logger {}

@Service
class PushoverService(
    private val client: PushoverClient,
    private val properties: PushoverProperties,
) {
    @PostConstruct
    fun logStatus() {
        if (properties.enabled) {
            val maskedToken = properties.apiToken.take(4) + "..."
            logger.info { "Pushover enabled, token: $maskedToken" }
        } else {
            logger.warn { "Pushover disabled" }
        }
    }

    fun isEnabled(): Boolean = properties.enabled

    fun send(userKey: String, message: String, title: String? = null): Boolean {
        val response = client.sendMessage(
            PushoverRequest(userKey = userKey, message = message, title = title),
        )
        return response.status == 1
    }

    fun sendWithPriority(
        userKey: String,
        message: String,
        title: String? = null,
        priority: Priority = Priority.NORMAL,
    ): Boolean {
        val response = client.sendMessage(
            PushoverRequest(userKey = userKey, message = message, title = title, priority = priority.value),
        )
        return response.status == 1
    }

    enum class Priority(
        val value: Int,
    ) {
        LOWEST(-2),
        LOW(-1),
        NORMAL(0),
        HIGH(1),
        EMERGENCY(2),
    }
}
