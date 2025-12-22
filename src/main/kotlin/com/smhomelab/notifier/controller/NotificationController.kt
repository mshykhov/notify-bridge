package com.smhomelab.notifier.controller

import com.smhomelab.notifier.api.model.LimitsResponse
import com.smhomelab.notifier.api.model.NotificationResponse
import com.smhomelab.notifier.api.model.PushoverNotificationRequest
import com.smhomelab.notifier.api.model.TelegramNotificationRequest
import com.smhomelab.notifier.service.NotificationApiService
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

private val logger = KotlinLogging.logger {}

@RestController
@RequestMapping("/api/v1/notifications")
class NotificationController(
    private val notificationService: NotificationApiService,
) {
    @PostMapping("/telegram")
    @PreAuthorize("hasAuthority('SCOPE_send:telegram')")
    suspend fun sendTelegram(
        @RequestBody request: TelegramNotificationRequest,
    ): ResponseEntity<NotificationResponse> {
        logger.info { "API: POST /telegram" }
        val response = notificationService.sendTelegram(request)
        return ResponseEntity.ok(response)
    }

    @PostMapping("/pushover")
    @PreAuthorize("hasAuthority('SCOPE_send:pushover')")
    suspend fun sendPushover(
        @RequestBody request: PushoverNotificationRequest,
    ): ResponseEntity<NotificationResponse> {
        logger.info { "API: POST /pushover" }
        val response = notificationService.sendPushover(request)
        return ResponseEntity.ok(response)
    }

    @GetMapping("/limits")
    @PreAuthorize("hasAuthority('SCOPE_read:limits')")
    fun getLimits(): ResponseEntity<LimitsResponse> {
        logger.info { "API: GET /limits" }
        val response = notificationService.getLimits()
        return ResponseEntity.ok(response)
    }
}
