package com.smhomelab.notifier.service

import org.springframework.boot.info.BuildProperties
import org.springframework.stereotype.Service

@Service
class AppInfoService(
    private val buildProperties: BuildProperties?,
) {
    fun getVersion(): String = buildProperties?.version ?: "N/A"
}
