package com.smhomelab.notifier.persistence.model

sealed class AddUserData {
    data class ById(val telegramId: Long) : AddUserData()
    data class ByUsername(val username: String) : AddUserData()
}
