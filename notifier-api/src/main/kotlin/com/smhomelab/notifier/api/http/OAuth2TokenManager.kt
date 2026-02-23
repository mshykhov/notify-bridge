package com.smhomelab.notifier.api.http

interface OAuth2TokenManager {
    suspend fun getAccessToken(): String

    suspend fun refreshToken(): String
}
