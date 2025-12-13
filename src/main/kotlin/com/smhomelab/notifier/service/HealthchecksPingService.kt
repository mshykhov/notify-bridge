package com.smhomelab.notifier.service

import com.smhomelab.notifier.config.HealthchecksProperties
import com.smhomelab.notifier.config.PushoverProperties
import com.smhomelab.notifier.pushover.PushoverClient
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import javax.sql.DataSource

@Service
@ConditionalOnProperty(prefix = "notifier.healthchecks", name = ["enabled"], havingValue = "true")
class HealthchecksPingService(
    private val properties: HealthchecksProperties,
    private val pushoverProperties: PushoverProperties,
    private val pushoverClient: PushoverClient,
    private val dataSource: DataSource,
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val restTemplate = RestTemplate()

    @Scheduled(fixedRate = 60_000)
    fun ping() {
        val checks = mutableListOf<HealthCheck>()

        // Check Database
        checks.add(checkDatabase())

        // Check Pushover API (only if enabled)
        if (pushoverProperties.enabled) {
            checks.add(checkPushoverApi())
        }

        val failed = checks.filter { !it.healthy }

        if (failed.isEmpty()) {
            sendPing()
        } else {
            val reasons = failed.joinToString(", ") { "${it.name}: ${it.reason}" }
            log.warn("Health checks failed: $reasons")
            sendFail(reasons)
        }
    }

    private fun checkDatabase(): HealthCheck {
        return try {
            dataSource.connection.use { conn ->
                conn.createStatement().use { stmt ->
                    stmt.executeQuery("SELECT 1").use { }
                }
            }
            HealthCheck("database", true)
        } catch (e: Exception) {
            HealthCheck("database", false, e.message)
        }
    }

    private fun checkPushoverApi(): HealthCheck {
        return try {
            val limits = pushoverClient.fetchLimits()
            if (limits != null) {
                HealthCheck("pushover", true)
            } else {
                HealthCheck("pushover", false, "API unreachable")
            }
        } catch (e: Exception) {
            HealthCheck("pushover", false, e.message)
        }
    }

    private fun sendPing() {
        try {
            restTemplate.getForEntity(properties.pingUrl, String::class.java)
            log.debug("Healthchecks ping sent")
        } catch (e: Exception) {
            log.warn("Healthchecks ping failed: ${e.message}")
        }
    }

    private fun sendFail(reason: String) {
        try {
            restTemplate.postForEntity(
                "${properties.pingUrl}/fail",
                reason,
                String::class.java,
            )
            log.debug("Healthchecks fail sent: $reason")
        } catch (e: Exception) {
            log.warn("Healthchecks fail notification failed: ${e.message}")
        }
    }

    private data class HealthCheck(
        val name: String,
        val healthy: Boolean,
        val reason: String? = null,
    )
}
