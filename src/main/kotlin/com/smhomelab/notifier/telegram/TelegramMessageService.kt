package com.smhomelab.notifier.telegram

import com.smhomelab.notifier.telegram.ratelimit.ChatRateLimiters
import com.smhomelab.notifier.telegram.ratelimit.RateLimitProperties
import com.smhomelab.notifier.telegram.ratelimit.RateLimiter
import io.github.dehuckakpyt.telegrambot.TelegramBot
import io.github.dehuckakpyt.telegrambot.exception.api.TelegramBotApiException
import io.github.dehuckakpyt.telegrambot.model.telegram.Message
import io.github.dehuckakpyt.telegrambot.model.telegram.ReplyMarkup
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.delay
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

private val logger = KotlinLogging.logger {}

@Service
@EnableConfigurationProperties(RateLimitProperties::class)
class TelegramMessageService(
    private val telegramBot: TelegramBot,
    private val config: RateLimitProperties,
) {
    private val globalLimiter = RateLimiter(
        tokensPerSecond = config.global.requestsPerSecond,
        maxTokens = config.global.burst,
    )

    private val chatLimiters = ChatRateLimiters(
        tokensPerSecond = config.perChat.requestsPerSecond,
        maxTokens = config.perChat.burst,
    )

    suspend fun send(
        chatId: Long,
        text: String,
        parseMode: String? = null,
        replyMarkup: ReplyMarkup? = null,
        disableNotification: Boolean? = null,
    ): Message = withRateLimitAndRetry(chatId) {
        telegramBot.sendMessage(
            chatId = chatId,
            text = text,
            parseMode = parseMode,
            replyMarkup = replyMarkup,
            disableNotification = disableNotification,
        )
    }

    @Scheduled(fixedRate = 3600_000)
    fun cleanupChatLimiters() {
        val sizeBefore = chatLimiters.size()
        chatLimiters.cleanup(MAX_CHAT_LIMITERS)
        val sizeAfter = chatLimiters.size()
        if (sizeBefore != sizeAfter) {
            logger.debug { "Cleaned up chat limiters: $sizeBefore -> $sizeAfter" }
        }
    }

    private suspend fun <T> withRateLimitAndRetry(
        chatId: Long,
        block: suspend () -> T,
    ): T {
        if (config.enabled) {
            globalLimiter.acquire()
            chatLimiters.acquire(chatId)
        }

        var lastException: TelegramBotApiException? = null
        repeat(config.retry.maxAttempts) { attempt ->
            try {
                return block()
            } catch (e: TelegramBotApiException) {
                val retryAfter = parseRetryAfter(e.message)
                if (retryAfter != null) {
                    logger.warn { "Rate limited for chatId=$chatId, retry after ${retryAfter}s (attempt ${attempt + 1})" }
                    delay(retryAfter * 1000L + 100)
                    lastException = e
                } else {
                    throw e
                }
            }
        }
        throw lastException ?: IllegalStateException("Unexpected retry state")
    }

    private fun parseRetryAfter(message: String?): Int? {
        if (message == null) return null
        return RETRY_AFTER_REGEX
            .find(message)
            ?.groupValues
            ?.get(1)
            ?.toIntOrNull()
    }

    companion object {
        private val RETRY_AFTER_REGEX = Regex("retry after (\\d+)")
        private const val MAX_CHAT_LIMITERS = 1000
    }
}
