package com.smhomelab.notifier.api.http

sealed class ClientResponse<out T> {
    data class Success<T>(
        val data: T,
    ) : ClientResponse<T>()

    data class Error(
        val statusCode: Int,
        val error: String,
        val message: String,
        val details: Map<String, String>? = null,
    ) : ClientResponse<Nothing>()

    fun getOrThrow(): T =
        when (this) {
            is Success -> data
            is Error -> throw ClientException(
                statusCode = statusCode,
                error = error,
                message = message,
                details = details,
            )
        }

    fun getOrNull(): T? =
        when (this) {
            is Success -> data
            is Error -> null
        }

    inline fun <R> map(transform: (T) -> R): ClientResponse<R> =
        when (this) {
            is Success -> Success(transform(data))
            is Error -> this
        }

    inline fun <R> flatMap(transform: (T) -> ClientResponse<R>): ClientResponse<R> =
        when (this) {
            is Success -> transform(data)
            is Error -> this
        }

    inline fun onSuccess(action: (T) -> Unit): ClientResponse<T> {
        if (this is Success) action(data)
        return this
    }

    inline fun onError(action: (Error) -> Unit): ClientResponse<T> {
        if (this is Error) action(this)
        return this
    }
}
