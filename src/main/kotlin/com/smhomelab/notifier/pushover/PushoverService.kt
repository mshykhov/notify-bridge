package com.smhomelab.notifier.pushover

import com.smhomelab.notifier.config.PushoverProperties
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class PushoverService(
    private val client: PushoverClient,
    private val properties: PushoverProperties
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun isEnabled(): Boolean = properties.enabled

    fun send(message: String, title: String? = null): Boolean {
        val response = client.sendMessage(
            PushoverRequest(message = message, title = title)
        )
        return response.status == 1
    }

    fun sendWithPriority(message: String, title: String? = null, priority: Priority = Priority.NORMAL): Boolean {
        val response = client.sendMessage(
            PushoverRequest(message = message, title = title, priority = priority.value)
        )
        return response.status == 1
    }

    fun sendWithUrl(message: String, title: String? = null, url: String, urlTitle: String? = null): Boolean {
        val response = client.sendMessage(
            PushoverRequest(message = message, title = title, url = url, urlTitle = urlTitle)
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
