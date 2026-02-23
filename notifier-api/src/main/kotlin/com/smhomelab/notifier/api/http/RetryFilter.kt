package com.smhomelab.notifier.api.http

import com.smhomelab.notifier.api.config.NotifierRetryProperties
import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.resilience4j.reactor.retry.RetryOperator
import io.github.resilience4j.retry.Retry
import io.github.resilience4j.retry.RetryConfig
import org.springframework.web.reactive.function.client.ClientRequest
import org.springframework.web.reactive.function.client.ClientResponse
import org.springframework.web.reactive.function.client.ExchangeFilterFunction
import org.springframework.web.reactive.function.client.ExchangeFunction
import org.springframework.web.reactive.function.client.WebClientResponseException
import reactor.core.publisher.Mono

private val logger = KotlinLogging.logger {}

class RetryFilter(
    private val properties: NotifierRetryProperties,
) : ExchangeFilterFunction {
    private val retry: Retry = createRetry()

    override fun filter(
        request: ClientRequest,
        next: ExchangeFunction,
    ): Mono<ClientResponse> =
        next
            .exchange(request)
            .flatMap { response ->
                if (properties.retryableStatusCodes.contains(response.statusCode().value())) {
                    response
                        .bodyToMono(ByteArray::class.java)
                        .defaultIfEmpty(ByteArray(0))
                        .flatMap { body ->
                            Mono.error(
                                WebClientResponseException.create(
                                    response.statusCode().value(),
                                    response.statusCode().toString(),
                                    response.headers().asHttpHeaders(),
                                    body,
                                    null,
                                ),
                            )
                        }
                } else {
                    Mono.just(response)
                }
            }.transformDeferred(RetryOperator.of(retry))
            .doOnError { ex ->
                if (ex is WebClientResponseException) {
                    logger.warn {
                        "Request failed after ${properties.maxAttempts} retries: " +
                            "${request.method()} ${request.url()} - ${ex.statusCode}"
                    }
                }
            }

    private fun createRetry(): Retry {
        val intervalFunction =
            io.github.resilience4j.core.IntervalFunction.ofExponentialBackoff(
                properties.baseDelay.toMillis(),
                properties.multiplier,
            )

        val config =
            RetryConfig
                .custom<ClientResponse>()
                .maxAttempts(properties.maxAttempts)
                .intervalFunction(intervalFunction)
                .retryOnException { throwable ->
                    when (throwable) {
                        is WebClientResponseException ->
                            properties.retryableStatusCodes.contains(throwable.statusCode.value())

                        else -> false
                    }
                }.build()

        return Retry.of("notifier-client", config)
    }
}
