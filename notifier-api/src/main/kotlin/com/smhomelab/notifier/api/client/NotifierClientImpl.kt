package com.smhomelab.notifier.api.client

import com.smhomelab.notifier.api.http.RestClientImpl
import com.smhomelab.notifier.api.http.get
import com.smhomelab.notifier.api.http.post
import com.smhomelab.notifier.api.model.HealthResponse
import com.smhomelab.notifier.api.model.LimitsResponse
import com.smhomelab.notifier.api.model.NotificationResponse
import com.smhomelab.notifier.api.model.PushoverNotificationRequest
import com.smhomelab.notifier.api.model.TelegramNotificationRequest
import org.springframework.web.reactive.function.client.WebClient

open class NotifierClientImpl(
    webClient: WebClient,
) : RestClientImpl(webClient),
    NotifierClient {
    override suspend fun sendTelegram(request: TelegramNotificationRequest): NotificationResponse =
        post<TelegramNotificationRequest, NotificationResponse>(
            path = "/api/v1/notifications/telegram",
            body = request,
        ).getOrThrow()

    override suspend fun sendPushover(request: PushoverNotificationRequest): NotificationResponse =
        post<PushoverNotificationRequest, NotificationResponse>(
            path = "/api/v1/notifications/pushover",
            body = request,
        ).getOrThrow()

    override suspend fun getLimits(): LimitsResponse =
        get<LimitsResponse>(
            path = "/api/v1/notifications/limits",
        ).getOrThrow()

    override suspend fun health(): HealthResponse =
        get<HealthResponse>(
            path = "/actuator/health",
        ).getOrThrow()

    override suspend fun ping() {
        get<Unit>(path = "/api/ping").getOrThrow()
    }
}
