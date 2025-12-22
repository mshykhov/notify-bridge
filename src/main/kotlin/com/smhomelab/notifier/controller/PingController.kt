package com.smhomelab.notifier.controller

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class PingController {
    @GetMapping("/api/ping")
    fun ping(): ResponseEntity<Void> = ResponseEntity.ok().build()
}
