package com.smhomelab.notifier.pushover

import com.smhomelab.notifier.config.PushoverProperties
import com.smhomelab.notifier.model.pushover.NotificationPriority
import com.smhomelab.notifier.model.pushover.PushoverRequest
import com.smhomelab.notifier.model.pushover.PushoverSound
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

    fun send(
        userKey: String,
        message: String,
        title: String? = null,
        priority: NotificationPriority = NotificationPriority.NORMAL,
        sound: PushoverSound? = null,
        url: String? = null,
        urlTitle: String? = null,
        html: Boolean = false,
        ttl: Int? = null,
    ): Boolean {
        val effectiveSound = sound ?: getDefaultSoundForPriority(priority)
        val request = PushoverRequest(
            userKey = userKey,
            message = message,
            title = title,
            priority = priority,
            sound = effectiveSound,
            url = url,
            urlTitle = urlTitle,
            html = html,
            ttl = ttl,
            retry = if (priority == NotificationPriority.EMERGENCY) DEFAULT_EMERGENCY_RETRY else null,
            expire = if (priority == NotificationPriority.EMERGENCY) DEFAULT_EMERGENCY_EXPIRE else null,
        )
        return client.sendMessage(request).status == 1
    }

    fun getDefaultSoundForPriority(priority: NotificationPriority): PushoverSound? =
        when (priority) {
            NotificationPriority.EMERGENCY -> PushoverSound.SIREN
            else -> null
        }

    companion object {
        const val DEFAULT_EMERGENCY_RETRY = 30
        const val DEFAULT_EMERGENCY_EXPIRE = 300
    }
}
