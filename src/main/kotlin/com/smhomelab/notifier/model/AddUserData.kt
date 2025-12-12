package com.smhomelab.notifier.model

sealed class AddUserData {
    data class ById(val telegramId: Long) : AddUserData()
    data class ByUsername(val username: String) : AddUserData()
}
