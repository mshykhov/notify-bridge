package com.smhomelab.notifier.pushover

import com.smhomelab.notifier.config.PushoverProperties
import com.smhomelab.notifier.model.pushover.PushoverLimits
import com.smhomelab.notifier.model.pushover.PushoverRequest
import com.smhomelab.notifier.model.pushover.PushoverResponse
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.util.LinkedMultiValueMap
import org.springframework.web.client.RestTemplate
import java.time.Instant

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

        logger.debug {
            buildString {
                appendLine("Sending Pushover notification:")
                appendLine("  user:     ${request.userKey.take(4)}...")
                appendLine("  title:    ${request.title ?: "-"}")
                appendLine("  priority: ${request.priority.displayName} (${request.priority.value})")
                appendLine("  sound:    ${request.sound?.displayName ?: "default"}")
                request.ttl?.let { appendLine("  ttl:      ${it}s") }
                request.retry?.let { appendLine("  retry:    ${it}s") }
                request.expire?.let { appendLine("  expire:   ${it}s") }
                append("  message:  ${request.message}")
            }
        }

        val body = buildRequestBody(request)
        var lastError: String? = null

        repeat(properties.maxRetries) { attempt ->
            try {
                val response = doSendMessage(body)
                if (response.status == 1) {
                    if (attempt > 0) {
                        logger.info { "Pushover message sent after ${attempt + 1} attempts" }
                    } else {
                        logger.debug { "Pushover message sent" }
                    }
                    return response
                }
                lastError = response.errors?.joinToString() ?: "Unknown API error"
            } catch (e: Exception) {
                lastError = e.message ?: "Unknown error"
            }

            if (attempt < properties.maxRetries - 1) {
                val delay = (attempt + 1) * properties.retryDelayMs
                logger.warn { "Pushover send failed (${attempt + 1}/${properties.maxRetries}): $lastError. Retry in ${delay}ms" }
                Thread.sleep(delay)
            }
        }

        logger.error { "Failed to send Pushover message after ${properties.maxRetries} attempts: $lastError" }
        return PushoverResponse(status = 0, request = null, errors = listOf(lastError ?: "Max retries exceeded"))
    }

    private fun buildRequestBody(request: PushoverRequest): LinkedMultiValueMap<String, String> =
        LinkedMultiValueMap<String, String>().apply {
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

    private fun doSendMessage(body: LinkedMultiValueMap<String, String>): PushoverResponse {
        val headers = HttpHeaders().apply {
            contentType = MediaType.APPLICATION_FORM_URLENCODED
        }
        return restTemplate.postForObject(
            MESSAGES_URL,
            HttpEntity(body, headers),
            PushoverResponse::class.java,
        ) ?: PushoverResponse(status = 0, request = null)
    }

    fun fetchLimits(): PushoverLimits? {
        if (!properties.enabled) {
            logger.debug { "Pushover is disabled, cannot fetch limits" }
            return null
        }

        return try {
            val response = restTemplate.getForEntity(
                "$LIMITS_URL?token=${properties.apiToken}",
                LimitsResponse::class.java,
            )
            response.body?.let { body ->
                PushoverLimits(
                    limit = body.limit,
                    remaining = body.remaining,
                    resetAt = Instant.ofEpochSecond(body.reset),
                ).also {
                    logger.debug { "Fetched Pushover limits: ${it.remaining}/${it.limit}" }
                }
            }
        } catch (e: Exception) {
            logger.error { "Failed to fetch Pushover limits: ${e.message}" }
            null
        }
    }

    private data class LimitsResponse(
        val limit: Int,
        val remaining: Int,
        val reset: Long,
    )

    fun validateUserKey(userKey: String): Boolean {
        if (!properties.enabled) {
            logger.debug { "Pushover is disabled, skipping validation" }
            return true
        }

        val headers = HttpHeaders().apply {
            contentType = MediaType.APPLICATION_FORM_URLENCODED
        }

        val body = LinkedMultiValueMap<String, String>().apply {
            add("token", properties.apiToken)
            add("user", userKey)
        }

        return try {
            val response = restTemplate.postForObject(
                VALIDATE_URL,
                HttpEntity(body, headers),
                ValidateResponse::class.java,
            )
            val valid = response?.status == 1
            logger.debug { "Pushover key validation: ${if (valid) "valid" else "invalid"}" }
            valid
        } catch (e: org.springframework.web.client.HttpClientErrorException) {
            logger.debug { "Pushover key invalid: ${e.statusCode}" }
            false
        } catch (e: Exception) {
            logger.error { "Failed to validate Pushover key: ${e.message}" }
            false
        }
    }

    private data class ValidateResponse(
        val status: Int,
        val errors: List<String>? = null,
    )

    companion object {
        private const val MESSAGES_URL = "https://api.pushover.net/1/messages.json"
        private const val LIMITS_URL = "https://api.pushover.net/1/apps/limits.json"
        private const val VALIDATE_URL = "https://api.pushover.net/1/users/validate.json"
    }
}
