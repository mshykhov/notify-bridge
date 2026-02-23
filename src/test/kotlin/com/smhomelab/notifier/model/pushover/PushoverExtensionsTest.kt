package com.smhomelab.notifier.model.pushover

import com.smhomelab.notifier.api.NotificationPriority
import com.smhomelab.notifier.api.PushoverSound
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class PushoverExtensionsTest {

    @Test
    fun `botDisplayName returns Russian text for all priorities`() {
        assertEquals("Без звука", NotificationPriority.LOWEST.botDisplayName)
        assertEquals("Тихо", NotificationPriority.LOW.botDisplayName)
        assertEquals("Обычно", NotificationPriority.NORMAL.botDisplayName)
        assertEquals("Важно", NotificationPriority.HIGH.botDisplayName)
        assertEquals("Экстренно", NotificationPriority.EMERGENCY.botDisplayName)
    }

    @Test
    fun `all priorities have non-empty botDisplayName`() {
        NotificationPriority.entries.forEach { priority ->
            assert(priority.botDisplayName.isNotBlank()) {
                "botDisplayName is blank for $priority"
            }
        }
    }

    @Test
    fun `all sounds have non-empty botDisplayName`() {
        PushoverSound.entries.forEach { sound ->
            assert(sound.botDisplayName.isNotBlank()) {
                "botDisplayName is blank for $sound"
            }
        }
    }
}
