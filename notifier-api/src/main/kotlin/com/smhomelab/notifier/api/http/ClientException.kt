package com.smhomelab.notifier.api.http

open class ClientException(
    val statusCode: Int = 0,
    val error: String,
    override val message: String,
    val details: Map<String, String>? = null,
    override val cause: Throwable? = null,
) : RuntimeException(message, cause)

class AuthenticationException(
    message: String,
    cause: Throwable? = null,
) : ClientException(
        statusCode = 401,
        error = "AUTHENTICATION_FAILED",
        message = message,
        cause = cause,
    )

class RateLimitException(
    message: String,
    val retryAfterSeconds: Long? = null,
) : ClientException(
        statusCode = 429,
        error = "RATE_LIMIT_EXCEEDED",
        message = message,
        details = retryAfterSeconds?.let { mapOf("retryAfter" to it.toString()) },
    )

class ServerException(
    statusCode: Int,
    message: String,
) : ClientException(
        statusCode = statusCode,
        error = "SERVER_ERROR",
        message = message,
    )

class NetworkException(
    message: String,
    cause: Throwable? = null,
) : ClientException(
        statusCode = 0,
        error = "NETWORK_ERROR",
        message = message,
        cause = cause,
    )
