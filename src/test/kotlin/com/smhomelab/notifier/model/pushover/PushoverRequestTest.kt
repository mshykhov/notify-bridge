package com.smhomelab.notifier.model.pushover

import com.smhomelab.notifier.api.NotificationPriority
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals

class PushoverRequestTest {
    @Test
    fun `valid request is created successfully`() {
        assertDoesNotThrow {
            PushoverRequest(userKey = "test-key", message = "Hello")
        }
    }

    @Test
    fun `message exceeding max length throws`() {
        val longMessage = "a".repeat(PushoverRequest.MAX_MESSAGE_LENGTH + 1)
        assertThrows<IllegalArgumentException> {
            PushoverRequest(userKey = "key", message = longMessage)
        }
    }

    @Test
    fun `title exceeding max length throws`() {
        val longTitle = "a".repeat(PushoverRequest.MAX_TITLE_LENGTH + 1)
        assertThrows<IllegalArgumentException> {
            PushoverRequest(userKey = "key", message = "msg", title = longTitle)
        }
    }

    @Test
    fun `url exceeding max length throws`() {
        val longUrl = "a".repeat(PushoverRequest.MAX_URL_LENGTH + 1)
        assertThrows<IllegalArgumentException> {
            PushoverRequest(userKey = "key", message = "msg", url = longUrl)
        }
    }

    @Test
    fun `url title exceeding max length throws`() {
        val longUrlTitle = "a".repeat(PushoverRequest.MAX_URL_TITLE_LENGTH + 1)
        assertThrows<IllegalArgumentException> {
            PushoverRequest(userKey = "key", message = "msg", urlTitle = longUrlTitle)
        }
    }

    @Test
    fun `html and monospace together throws`() {
        assertThrows<IllegalArgumentException> {
            PushoverRequest(userKey = "key", message = "msg", html = true, monospace = true)
        }
    }

    @Test
    fun `emergency priority without retry throws`() {
        assertThrows<IllegalArgumentException> {
            PushoverRequest(
                userKey = "key",
                message = "msg",
                priority = NotificationPriority.EMERGENCY,
                expire = 300,
            )
        }
    }

    @Test
    fun `emergency priority without expire throws`() {
        assertThrows<IllegalArgumentException> {
            PushoverRequest(
                userKey = "key",
                message = "msg",
                priority = NotificationPriority.EMERGENCY,
                retry = 30,
            )
        }
    }

    @Test
    fun `emergency priority with valid retry and expire succeeds`() {
        assertDoesNotThrow {
            PushoverRequest(
                userKey = "key",
                message = "msg",
                priority = NotificationPriority.EMERGENCY,
                retry = 30,
                expire = 300,
            )
        }
    }

    @Test
    fun `emergency priority with retry below minimum throws`() {
        assertThrows<IllegalArgumentException> {
            PushoverRequest(
                userKey = "key",
                message = "msg",
                priority = NotificationPriority.EMERGENCY,
                retry = 10,
                expire = 300,
            )
        }
    }

    @Test
    fun `emergency priority with expire above maximum throws`() {
        assertThrows<IllegalArgumentException> {
            PushoverRequest(
                userKey = "key",
                message = "msg",
                priority = NotificationPriority.EMERGENCY,
                retry = 30,
                expire = PushoverRequest.MAX_EXPIRE_SECONDS + 1,
            )
        }
    }

    @Test
    fun `default priority is NORMAL`() {
        val request = PushoverRequest(userKey = "key", message = "msg")
        assertEquals(NotificationPriority.NORMAL, request.priority)
    }
}
