package com.smhomelab.notifier.api.model

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class TelegramNotificationRequestTest {
    @Test
    fun `valid request is created successfully`() {
        assertDoesNotThrow {
            TelegramNotificationRequest(message = "Hello")
        }
    }

    @Test
    fun `blank message throws`() {
        assertThrows<IllegalArgumentException> {
            TelegramNotificationRequest(message = "   ")
        }
    }

    @Test
    fun `empty message throws`() {
        assertThrows<IllegalArgumentException> {
            TelegramNotificationRequest(message = "")
        }
    }

    @Test
    fun `message exceeding max length throws`() {
        val longMessage = "a".repeat(TelegramNotificationRequest.MAX_MESSAGE_LENGTH + 1)
        assertThrows<IllegalArgumentException> {
            TelegramNotificationRequest(message = longMessage)
        }
    }

    @Test
    fun `message at max length succeeds`() {
        val maxMessage = "a".repeat(TelegramNotificationRequest.MAX_MESSAGE_LENGTH)
        assertDoesNotThrow {
            TelegramNotificationRequest(message = maxMessage)
        }
    }

    @ParameterizedTest
    @ValueSource(strings = ["HTML", "Markdown", "MarkdownV2"])
    fun `valid parse modes are accepted`(parseMode: String) {
        assertDoesNotThrow {
            TelegramNotificationRequest(message = "test", parseMode = parseMode)
        }
    }

    @Test
    fun `invalid parse mode throws`() {
        assertThrows<IllegalArgumentException> {
            TelegramNotificationRequest(message = "test", parseMode = "xml")
        }
    }

    @Test
    fun `null parse mode is accepted`() {
        assertDoesNotThrow {
            TelegramNotificationRequest(message = "test", parseMode = null)
        }
    }
}
