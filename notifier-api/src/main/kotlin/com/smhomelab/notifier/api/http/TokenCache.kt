package com.smhomelab.notifier.api.http

import java.time.Instant
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

class TokenCache(
    private val expirationBufferSeconds: Long = 60,
) {
    private val lock = ReentrantReadWriteLock()
    private var token: String? = null
    private var expiresAt: Instant? = null

    fun get(): String? =
        lock.read {
            val currentToken = token
            val expiration = expiresAt
            if (currentToken != null && expiration != null && Instant.now().isBefore(expiration)) {
                currentToken
            } else {
                null
            }
        }

    fun set(
        newToken: String,
        expiresInSeconds: Long,
    ) {
        lock.write {
            token = newToken
            expiresAt = Instant.now().plusSeconds(expiresInSeconds - expirationBufferSeconds)
        }
    }

    fun clear() {
        lock.write {
            token = null
            expiresAt = null
        }
    }
}
