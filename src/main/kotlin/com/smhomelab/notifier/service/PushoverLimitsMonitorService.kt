package com.smhomelab.notifier.service

import com.smhomelab.notifier.config.AdminProperties
import com.smhomelab.notifier.model.pushover.LimitThreshold
import com.smhomelab.notifier.model.pushover.PushoverLimits
import com.smhomelab.notifier.pushover.PushoverService
import com.smhomelab.notifier.telegram.TelegramMessageService
import com.smhomelab.notifier.util.TimeUtils
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.annotation.PostConstruct
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.time.Instant
import java.time.ZoneId
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicReference

private val logger = KotlinLogging.logger {}

@Service
class PushoverLimitsMonitorService(
    private val pushoverService: PushoverService,
    private val adminProperties: AdminProperties,
    private val telegramMessageService: TelegramMessageService,
) {
    private val notifiedThresholds = ConcurrentHashMap.newKeySet<LimitThreshold>()
    private var lastResetTimestamp: Long = 0
    private val cachedLimits = AtomicReference<CachedLimits?>(null)
    private val scope = CoroutineScope(Dispatchers.IO)

    @PostConstruct
    fun init() {
        pushoverService.setLimitsMonitor { checkAndNotifyIfNeeded() }
        logger.debug { "PushoverLimitsMonitorService registered with PushoverService" }
    }

    @Scheduled(fixedRate = 600_000, initialDelay = 60_000)
    fun scheduledCheck() {
        if (!pushoverService.isEnabled()) return
        logger.debug { "Scheduled limits check" }
        checkAndNotifyIfNeeded()
    }

    fun getLimits(): PushoverLimits? = cachedLimits.get()?.limits ?: fetchAndCache()

    fun refreshLimits(): PushoverLimits? = fetchAndCache()

    fun getLastFetchedAt(): Instant? = cachedLimits.get()?.fetchedAt

    private fun checkAndNotifyIfNeeded() {
        val cached = cachedLimits.get()
        if (cached != null && !cached.isStale()) {
            logger.debug { "Skipping limits check, cache is fresh" }
            return
        }

        val limits = fetchAndCache() ?: return
        checkResetCycle(limits)

        val currentThreshold = limits.currentThreshold() ?: return

        if (notifiedThresholds.contains(currentThreshold)) return

        notifiedThresholds.add(currentThreshold)
        LimitThreshold.entries
            .filter { it.percent > currentThreshold.percent }
            .forEach { notifiedThresholds.add(it) }

        notifyAdmin(limits, currentThreshold)
    }

    private fun checkResetCycle(limits: PushoverLimits) {
        if (limits.resetAt.epochSecond != lastResetTimestamp) {
            lastResetTimestamp = limits.resetAt.epochSecond
            notifiedThresholds.clear()
            logger.debug { "Reset cycle detected, cleared notified thresholds" }
        }
    }

    private fun notifyAdmin(limits: PushoverLimits, threshold: LimitThreshold) {
        val message = buildLimitAlertMessage(limits, threshold)
        logger.warn { "Pushover limit alert: ${limits.remaining}/${limits.limit} (${limits.remainingPercent}%)" }

        scope.launch {
            try {
                telegramMessageService.send(
                    chatId = adminProperties.masterAdminId,
                    text = message,
                    parseMode = "HTML",
                )
            } catch (e: Exception) {
                logger.error { "Failed to send limit alert to admin: ${e.message}" }
            }
        }
    }

    private fun buildLimitAlertMessage(limits: PushoverLimits, threshold: LimitThreshold): String {
        val resetTime = TimeUtils.formatFull(limits.resetAt, ZoneId.of("UTC"))

        return buildString {
            appendLine("${threshold.emoji} <b>Pushover: осталось ${limits.remainingPercent}%</b>")
            appendLine()
            appendLine("📈 Использовано: <b>${limits.used}</b> из ${limits.limit}")
            appendLine("📉 Осталось: <b>${limits.remaining}</b>")
            appendLine("🔄 Сброс: <b>$resetTime</b>")
        }
    }

    private fun fetchAndCache(): PushoverLimits? {
        val limits = pushoverService.fetchLimits() ?: return null
        cachedLimits.set(CachedLimits(limits, Instant.now()))
        return limits
    }

    fun formatStatusMessage(limits: PushoverLimits?, fetchedAt: Instant?, zoneId: ZoneId): String {
        if (limits == null) {
            return "📊 <b>Pushover</b>: <i>нет данных</i>"
        }

        val statusEmoji = limits.currentThreshold()?.emoji ?: "✅"
        val resetTime = TimeUtils.formatFull(limits.resetAt, zoneId)
        val updatedTime = fetchedAt?.let { TimeUtils.formatFull(it, zoneId) } ?: "—"

        return buildString {
            appendLine("📊 <b>Pushover</b>")
            appendLine()
            appendLine("$statusEmoji Осталось: <b>${limits.remaining}</b> из ${limits.limit}")
            appendLine("📈 Использовано: <b>${limits.used}</b> (${limits.usagePercent}%)")
            appendLine("🔄 Сброс: <b>$resetTime</b>")
            append("🕐 Обновлено: <b>$updatedTime</b>")
        }
    }

    private data class CachedLimits(
        val limits: PushoverLimits,
        val fetchedAt: Instant,
    ) {
        fun isStale(): Boolean =
            Instant.now().epochSecond - fetchedAt.epochSecond > CACHE_TTL_SECONDS
    }

    companion object {
        private const val CACHE_TTL_SECONDS = 60L
    }
}
