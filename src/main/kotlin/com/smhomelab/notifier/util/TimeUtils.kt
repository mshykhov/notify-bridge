package com.smhomelab.notifier.util

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object TimeUtils {
    private const val FULL_FORMAT = "dd.MM.yyyy HH:mm:ss"
    private const val SHORT_FORMAT = "HH:mm:ss"

    fun formatFull(instant: Instant, zoneId: ZoneId): String {
        val formatted = DateTimeFormatter.ofPattern(FULL_FORMAT)
            .withZone(zoneId)
            .format(instant)
        return if (zoneId == ZoneId.of("UTC")) "$formatted UTC" else formatted
    }

    fun formatShort(instant: Instant, zoneId: ZoneId): String {
        val formatted = DateTimeFormatter.ofPattern(SHORT_FORMAT)
            .withZone(zoneId)
            .format(instant)
        return if (zoneId == ZoneId.of("UTC")) "$formatted UTC" else formatted
    }
}
