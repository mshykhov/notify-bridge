package com.smhomelab.notifier.api.model

import com.smhomelab.notifier.api.NotificationPriority
import com.smhomelab.notifier.api.PushoverSound
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals

class PushoverNotificationRequestTest {
    @Test
    fun `valid request is created successfully`() {
        assertDoesNotThrow {
            PushoverNotificationRequest(message = "Hello")
        }
    }

    @Test
    fun `blank message throws`() {
        assertThrows<IllegalArgumentException> {
            PushoverNotificationRequest(message = "   ")
        }
    }

    @Test
    fun `message exceeding max length throws`() {
        val longMessage = "a".repeat(PushoverNotificationRequest.MAX_MESSAGE_LENGTH + 1)
        assertThrows<IllegalArgumentException> {
            PushoverNotificationRequest(message = longMessage)
        }
    }

    @Test
    fun `title exceeding max length throws`() {
        val longTitle = "a".repeat(PushoverNotificationRequest.MAX_TITLE_LENGTH + 1)
        assertThrows<IllegalArgumentException> {
            PushoverNotificationRequest(message = "msg", title = longTitle)
        }
    }

    @Test
    fun `url exceeding max length throws`() {
        val longUrl = "a".repeat(PushoverNotificationRequest.MAX_URL_LENGTH + 1)
        assertThrows<IllegalArgumentException> {
            PushoverNotificationRequest(message = "msg", url = longUrl)
        }
    }

    @Test
    fun `url title exceeding max length throws`() {
        val longUrlTitle = "a".repeat(PushoverNotificationRequest.MAX_URL_TITLE_LENGTH + 1)
        assertThrows<IllegalArgumentException> {
            PushoverNotificationRequest(message = "msg", urlTitle = longUrlTitle)
        }
    }

    @Test
    fun `default priority is NORMAL`() {
        val request = PushoverNotificationRequest(message = "msg")
        assertEquals(NotificationPriority.NORMAL, request.priority)
    }

    @Test
    fun `request with all fields succeeds`() {
        assertDoesNotThrow {
            PushoverNotificationRequest(
                message = "alert",
                title = "Title",
                priority = NotificationPriority.HIGH,
                sound = PushoverSound.SIREN,
                url = "https://example.com",
                urlTitle = "Example",
                html = true,
                ttl = 3600,
            )
        }
    }
}
