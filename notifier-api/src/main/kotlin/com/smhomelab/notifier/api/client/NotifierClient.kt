package com.smhomelab.notifier.api.client

import com.smhomelab.notifier.api.model.LimitsResponse
import com.smhomelab.notifier.api.model.NotificationResponse
import com.smhomelab.notifier.api.model.PushoverNotificationRequest
import com.smhomelab.notifier.api.model.TelegramNotificationRequest

interface NotifierClient {
    suspend fun sendTelegram(request: TelegramNotificationRequest): NotificationResponse

    suspend fun sendPushover(request: PushoverNotificationRequest): NotificationResponse

    suspend fun getLimits(): LimitsResponse
}
