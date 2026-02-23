package com.smhomelab.notifier.api.http

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.web.reactive.function.client.ClientRequest
import org.springframework.web.reactive.function.client.ClientResponse
import org.springframework.web.reactive.function.client.ExchangeFilterFunction
import org.springframework.web.reactive.function.client.ExchangeFunction
import reactor.core.publisher.Mono

private val logger = KotlinLogging.logger {}

class LoggingFilter : ExchangeFilterFunction {
    override fun filter(
        request: ClientRequest,
        next: ExchangeFunction,
    ): Mono<ClientResponse> {
        val startTime = System.currentTimeMillis()

        logger.debug { ">>> ${request.method()} ${request.url()}" }

        return next
            .exchange(request)
            .doOnSuccess { response ->
                val duration = System.currentTimeMillis() - startTime
                logger.debug {
                    "<<< ${request.method()} ${request.url()} - ${response.statusCode()} (${duration}ms)"
                }
            }.doOnError { error ->
                val duration = System.currentTimeMillis() - startTime
                logger.error(error) {
                    "<<< ${request.method()} ${request.url()} - ERROR (${duration}ms)"
                }
            }
    }
}
