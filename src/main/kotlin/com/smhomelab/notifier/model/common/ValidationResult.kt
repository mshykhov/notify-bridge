package com.smhomelab.notifier.model.common

sealed class ValidationResult<out T> {
    data class Valid<T>(
        val value: T,
    ) : ValidationResult<T>()

    data class Invalid(
        val error: String,
    ) : ValidationResult<Nothing>()
}
