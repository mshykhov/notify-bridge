package com.smhomelab.notifier.pushover

import com.smhomelab.notifier.api.NotificationPriority
import com.smhomelab.notifier.api.PushoverSound
import com.smhomelab.notifier.config.PushoverProperties
import com.smhomelab.notifier.model.pushover.LimitsMonitorCallback
import com.smhomelab.notifier.model.pushover.PushoverLimits
import com.smhomelab.notifier.model.pushover.PushoverRequest
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.annotation.PostConstruct
import org.springframework.stereotype.Service

private val logger = KotlinLogging.logger {}

@Service
class PushoverService(
    private val client: PushoverClient,
    private val properties: PushoverProperties,
) {
    private var limitsMonitor: LimitsMonitorCallback? = null

    @PostConstruct
    fun logStatus() {
        if (properties.enabled) {
            val maskedToken = properties.apiToken.take(4) + "..."
            logger.info { "Pushover enabled, token: $maskedToken" }
        } else {
            logger.warn { "Pushover disabled" }
        }
    }

    fun setLimitsMonitor(callback: LimitsMonitorCallback) {
        this.limitsMonitor = callback
    }

    fun isEnabled(): Boolean = properties.enabled

    fun fetchLimits(): PushoverLimits? = client.fetchLimits()

    fun validateUserKey(userKey: String): Boolean = client.validateUserKey(userKey)

    suspend fun send(
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
        val success = client.sendMessage(request).status == 1
        if (success) {
            limitsMonitor?.onMessageSent()
        }
        return success
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
