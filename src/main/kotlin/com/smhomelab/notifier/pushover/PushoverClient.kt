package com.smhomelab.notifier.pushover

import com.smhomelab.notifier.config.PushoverProperties
import com.smhomelab.notifier.model.pushover.PushoverRequest
import com.smhomelab.notifier.model.pushover.PushoverResponse
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.util.LinkedMultiValueMap
import org.springframework.web.client.RestTemplate

private val logger = KotlinLogging.logger {}

@Component
class PushoverClient(
    private val properties: PushoverProperties,
) {
    private val restTemplate = RestTemplate()

    fun sendMessage(request: PushoverRequest): PushoverResponse {
        if (!properties.enabled) {
            logger.debug { "Pushover is disabled, skipping message" }
            return PushoverResponse(status = 0, request = null)
        }

        logger.debug { "Sending Pushover: userKey=${request.userKey.take(4)}..., priority=${request.priority}" }

        val headers = HttpHeaders().apply {
            contentType = MediaType.APPLICATION_FORM_URLENCODED
        }

        val body = LinkedMultiValueMap<String, String>().apply {
            add("token", properties.apiToken)
            add("user", request.userKey)
            add("message", request.message)
            add("priority", request.priority.value.toString())
            request.title?.let { add("title", it) }
            request.sound?.let { add("sound", it.apiValue) }
            request.url?.let { add("url", it) }
            request.urlTitle?.let { add("url_title", it) }
            request.device?.let { add("device", it) }
            request.timestamp?.let { add("timestamp", it.toString()) }
            request.ttl?.let { add("ttl", it.toString()) }
            request.retry?.let { add("retry", it.toString()) }
            request.expire?.let { add("expire", it.toString()) }
            request.callback?.let { add("callback", it) }
            request.tags?.let { add("tags", it) }
            if (request.html) add("html", "1")
            if (request.monospace) add("monospace", "1")
        }

        return try {
            val response = restTemplate.postForObject(
                API_URL,
                HttpEntity(body, headers),
                PushoverResponse::class.java,
            )
            logger.debug { "Pushover message sent: ${response?.status}" }
            response ?: PushoverResponse(status = 0, request = null)
        } catch (e: Exception) {
            logger.error { "Failed to send Pushover message: ${e.message}" }
            PushoverResponse(status = 0, request = null, errors = listOf(e.message ?: "Unknown error"))
        }
    }

    companion object {
        private const val API_URL = "https://api.pushover.net/1/messages.json"
    }
}
