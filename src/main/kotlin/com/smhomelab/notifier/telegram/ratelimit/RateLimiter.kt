package com.smhomelab.notifier.telegram.ratelimit

import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import java.util.concurrent.ConcurrentHashMap

class RateLimiter(
    private val tokensPerSecond: Double,
    private val maxTokens: Int,
) {
    private var tokens: Double = maxTokens.toDouble()
    private var lastRefillTime: Long = System.nanoTime()
    private val mutex = Mutex()

    suspend fun acquire() {
        mutex.lock()
        try {
            refill()
            while (tokens < 1.0) {
                val waitMs = ((1.0 - tokens) / tokensPerSecond * 1000).toLong().coerceAtLeast(10)
                mutex.unlock()
                delay(waitMs)
                mutex.lock()
                refill()
            }
            tokens -= 1.0
        } finally {
            mutex.unlock()
        }
    }

    private fun refill() {
        val now = System.nanoTime()
        val elapsed = (now - lastRefillTime) / 1_000_000_000.0
        tokens = (tokens + elapsed * tokensPerSecond).coerceAtMost(maxTokens.toDouble())
        lastRefillTime = now
    }
}

class ChatRateLimiters(
    private val tokensPerSecond: Double,
    private val maxTokens: Int,
) {
    private val limiters = ConcurrentHashMap<Long, TimestampedLimiter>()

    suspend fun acquire(chatId: Long) {
        val entry = limiters.computeIfAbsent(chatId) {
            TimestampedLimiter(RateLimiter(tokensPerSecond, maxTokens))
        }
        entry.touch()
        entry.limiter.acquire()
    }

    fun cleanup(maxSize: Int = 1000) {
        if (limiters.size <= maxSize) return

        val toRemove = limiters.size - maxSize
        limiters.entries
            .sortedBy { it.value.lastAccessTime }
            .take(toRemove)
            .forEach { limiters.remove(it.key) }
    }

    fun size(): Int = limiters.size

    private class TimestampedLimiter(
        val limiter: RateLimiter,
    ) {
        @Volatile
        var lastAccessTime: Long = System.currentTimeMillis()
            private set

        fun touch() {
            lastAccessTime = System.currentTimeMillis()
        }
    }
}
