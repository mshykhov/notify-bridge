package com.smhomelab.notifier.telegram.ratelimit

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RateLimiterTest {

    @Test
    fun `first acquire succeeds immediately`() = runBlocking {
        val limiter = RateLimiter(tokensPerSecond = 1.0, maxTokens = 1)
        val start = System.currentTimeMillis()
        limiter.acquire()
        val elapsed = System.currentTimeMillis() - start
        assertTrue(elapsed < 100, "First acquire should be near-instant, took ${elapsed}ms")
    }

    @Test
    fun `acquire respects rate limit`() = runBlocking {
        val limiter = RateLimiter(tokensPerSecond = 10.0, maxTokens = 1)
        limiter.acquire()

        val start = System.currentTimeMillis()
        limiter.acquire()
        val elapsed = System.currentTimeMillis() - start

        assertTrue(elapsed >= 50, "Second acquire should wait for token refill, took ${elapsed}ms")
    }

    @Test
    fun `burst tokens allow multiple immediate acquires`() = runBlocking {
        val limiter = RateLimiter(tokensPerSecond = 1.0, maxTokens = 5)
        val start = System.currentTimeMillis()

        repeat(5) { limiter.acquire() }
        val elapsed = System.currentTimeMillis() - start

        assertTrue(elapsed < 200, "Burst of 5 should be fast with maxTokens=5, took ${elapsed}ms")
    }
}

class ChatRateLimitersTest {

    @Test
    fun `different chats get independent limiters`() = runBlocking {
        val limiters = ChatRateLimiters(tokensPerSecond = 10.0, maxTokens = 1)

        val start = System.currentTimeMillis()
        val jobs = (1L..5L).map { chatId ->
            async { limiters.acquire(chatId) }
        }
        jobs.awaitAll()
        val elapsed = System.currentTimeMillis() - start

        assertTrue(elapsed < 200, "Independent chats should not block each other, took ${elapsed}ms")
        assertEquals(5, limiters.size())
    }

    @Test
    fun `cleanup removes oldest entries`() = runBlocking {
        val limiters = ChatRateLimiters(tokensPerSecond = 10.0, maxTokens = 1)

        (1L..10L).forEach { limiters.acquire(it) }
        assertEquals(10, limiters.size())

        limiters.cleanup(maxSize = 5)
        assertEquals(5, limiters.size())
    }

    @Test
    fun `cleanup does nothing when under max size`() = runBlocking {
        val limiters = ChatRateLimiters(tokensPerSecond = 10.0, maxTokens = 1)

        (1L..3L).forEach { limiters.acquire(it) }
        limiters.cleanup(maxSize = 5)

        assertEquals(3, limiters.size())
    }
}
