package com.smhomelab.notifier.service

import com.smhomelab.notifier.config.HealthchecksProperties
import com.smhomelab.notifier.config.PushoverProperties
import com.smhomelab.notifier.pushover.PushoverClient
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import java.util.concurrent.atomic.AtomicInteger
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
    private val pushoverFailCount = AtomicInteger(0)

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

    private fun checkDatabase(): HealthCheck = try {
        dataSource.connection.use { conn ->
            conn.createStatement().use { stmt ->
                stmt.executeQuery("SELECT 1").use { }
            }
        }
        HealthCheck("database", true)
    } catch (e: Exception) {
        HealthCheck("database", false, e.message)
    }

    private fun checkPushoverApi(): HealthCheck {
        val result = try {
            pushoverClient.fetchLimits()
        } catch (e: Exception) {
            log.debug("Pushover API check exception: ${e.message}")
            null
        }

        if (result != null) {
            val previousFails = pushoverFailCount.getAndSet(0)
            if (previousFails > 0) {
                log.info("Pushover API recovered after $previousFails failed attempts")
            }
            return HealthCheck("pushover", true)
        }

        val failCount = pushoverFailCount.incrementAndGet()
        val threshold = properties.pushoverFailThreshold
        return if (failCount >= threshold) {
            HealthCheck("pushover", false, "API unreachable ($failCount consecutive failures)")
        } else {
            log.warn("Pushover API check failed ($failCount/$threshold), not failing healthcheck yet")
            HealthCheck("pushover", true)
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
