package com.smhomelab.notifier.service

import com.smhomelab.notifier.api.model.LimitsResponse
import com.smhomelab.notifier.api.model.NotificationResponse
import com.smhomelab.notifier.api.model.PushoverLimitsInfo
import com.smhomelab.notifier.api.model.PushoverNotificationRequest
import com.smhomelab.notifier.api.model.TelegramLimitsInfo
import com.smhomelab.notifier.api.model.TelegramNotificationRequest
import com.smhomelab.notifier.config.AdminProperties
import com.smhomelab.notifier.config.PushoverProperties
import com.smhomelab.notifier.exception.ChannelDisabledException
import com.smhomelab.notifier.exception.ConfigurationMissingException
import com.smhomelab.notifier.exception.SendFailedException
import com.smhomelab.notifier.model.pushover.NotificationPriority
import com.smhomelab.notifier.model.pushover.PushoverSound
import com.smhomelab.notifier.persistence.facade.UserPushoverConfigFacade
import com.smhomelab.notifier.pushover.PushoverService
import com.smhomelab.notifier.telegram.TelegramMessageService
import com.smhomelab.notifier.telegram.ratelimit.RateLimitProperties
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.stereotype.Service

private val logger = KotlinLogging.logger {}

@Service
class NotificationApiService(
    private val adminProperties: AdminProperties,
    private val telegramService: TelegramMessageService,
    private val pushoverService: PushoverService,
    private val pushoverProperties: PushoverProperties,
    private val rateLimitProperties: RateLimitProperties,
    private val pushoverConfigFacade: UserPushoverConfigFacade,
) {
    suspend fun sendTelegram(request: TelegramNotificationRequest): NotificationResponse {
        logger.info { "Sending Telegram notification to master admin" }

        val result = telegramService.send(
            chatId = adminProperties.masterAdminId,
            text = request.message,
            parseMode = request.parseMode,
            disableNotification = request.disableNotification,
        )

        logger.info { "Telegram notification sent: messageId=${result.messageId}" }

        return NotificationResponse(
            success = true,
            channel = "telegram",
            messageId = result.messageId.toString(),
        )
    }

    suspend fun sendPushover(request: PushoverNotificationRequest): NotificationResponse {
        logger.info { "Sending Pushover notification to master admin" }

        if (!pushoverService.isEnabled()) {
            throw ChannelDisabledException("pushover")
        }

        val config = pushoverConfigFacade.findByTelegramId(adminProperties.masterAdminId)
            ?: throw ConfigurationMissingException(
                "Master admin has no Pushover configuration. Please configure via Telegram bot.",
            )

        if (!config.enabled) {
            throw ChannelDisabledException("pushover", "User has disabled Pushover notifications")
        }

        val priority = NotificationPriority.entries.first { it.value == request.priority.value }
        val sound = request.sound?.let { apiSound ->
            PushoverSound.fromApiValue(apiSound.value)
        }

        val success = withContext(Dispatchers.IO) {
            pushoverService.send(
                userKey = config.userKey,
                message = request.message,
                title = request.title,
                priority = priority,
                sound = sound,
                url = request.url,
                urlTitle = request.urlTitle,
                html = request.html,
                ttl = request.ttl,
            )
        }

        if (!success) {
            throw SendFailedException("pushover", "Pushover API returned failure")
        }

        logger.info { "Pushover notification sent: priority=${request.priority.name}" }

        return NotificationResponse(
            success = true,
            channel = "pushover",
        )
    }

    fun getLimits(): LimitsResponse {
        val pushoverLimits = if (pushoverService.isEnabled()) {
            val limits = pushoverService.fetchLimits()
            PushoverLimitsInfo(
                enabled = true,
                limit = limits?.limit,
                remaining = limits?.remaining,
                resetAt = limits?.resetAt?.toString(),
            )
        } else {
            PushoverLimitsInfo(enabled = false)
        }

        val telegramLimits = TelegramLimitsInfo(
            enabled = true,
            rateLimitEnabled = rateLimitProperties.enabled,
            globalRequestsPerSecond = rateLimitProperties.global.requestsPerSecond,
            perChatRequestsPerSecond = rateLimitProperties.perChat.requestsPerSecond,
        )

        return LimitsResponse(
            pushover = pushoverLimits,
            telegram = telegramLimits,
        )
    }
}
