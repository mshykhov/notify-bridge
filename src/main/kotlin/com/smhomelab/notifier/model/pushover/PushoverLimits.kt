package com.smhomelab.notifier.model.pushover

import java.time.Instant

data class PushoverLimits(
    val limit: Int,
    val remaining: Int,
    val resetAt: Instant,
) {
    val used: Int get() = limit - remaining
    val usagePercent: Int get() = if (limit > 0) (used * 100) / limit else 0
    val remainingPercent: Int get() = if (limit > 0) (remaining * 100) / limit else 0

    fun isAtThreshold(threshold: LimitThreshold): Boolean =
        remainingPercent <= threshold.percent

    fun currentThreshold(): LimitThreshold? =
        LimitThreshold.entries.sortedBy { it.percent }.firstOrNull { isAtThreshold(it) }
}

enum class LimitThreshold(
    val percent: Int,
    val emoji: String,
) {
    CRITICAL(5, "🚨"),
    LOW(10, "🔴"),
    WARNING(25, "🟠"),
    NOTICE(50, "🟡"),
    ;

    companion object {
        fun fromPercent(percent: Int): LimitThreshold? =
            entries.sortedBy { it.percent }.firstOrNull { percent <= it.percent }
    }
}
