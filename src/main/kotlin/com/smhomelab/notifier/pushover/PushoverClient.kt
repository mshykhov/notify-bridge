package com.smhomelab.notifier.pushover

import com.smhomelab.notifier.config.PushoverProperties
import org.slf4j.LoggerFactory
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.util.LinkedMultiValueMap
import org.springframework.web.client.RestTemplate

@Component
class PushoverClient(
    private val properties: PushoverProperties
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val restTemplate = RestTemplate()

    fun sendMessage(request: PushoverRequest): PushoverResponse {
        if (!properties.enabled) {
            log.debug("Pushover is disabled, skipping message")
            return PushoverResponse(status = 0, request = null)
        }

        val headers = HttpHeaders().apply {
            contentType = MediaType.APPLICATION_FORM_URLENCODED
        }

        val body = LinkedMultiValueMap<String, String>().apply {
            add("token", properties.apiToken)
            add("user", request.userKey)
            add("message", request.message)
            request.title?.let { add("title", it) }
            request.priority?.let { add("priority", it.toString()) }
            request.sound?.let { add("sound", it) }
            request.url?.let { add("url", it) }
            request.urlTitle?.let { add("url_title", it) }
            if (request.html) add("html", "1")
        }

        return try {
            val response = restTemplate.postForObject(
                API_URL,
                HttpEntity(body, headers),
                PushoverResponse::class.java
            )
            log.debug("Pushover message sent: ${response?.status}")
            response ?: PushoverResponse(status = 0, request = null)
        } catch (e: Exception) {
            log.error("Failed to send Pushover message: ${e.message}")
            PushoverResponse(status = 0, request = null, errors = listOf(e.message ?: "Unknown error"))
        }
    }

    companion object {
        private const val API_URL = "https://api.pushover.net/1/messages.json"
    }
}

data class PushoverRequest(
    val userKey: String,
    val message: String,
    val title: String? = null,
    val priority: Int? = null,
    val sound: String? = null,
    val url: String? = null,
    val urlTitle: String? = null,
    val html: Boolean = false
)

data class PushoverResponse(
    val status: Int,
    val request: String?,
    val errors: List<String>? = null
)
