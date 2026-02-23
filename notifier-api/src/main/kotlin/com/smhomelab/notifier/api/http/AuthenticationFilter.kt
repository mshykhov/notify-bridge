package com.smhomelab.notifier.api.http

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.reactor.mono
import org.springframework.web.reactive.function.client.ClientRequest
import org.springframework.web.reactive.function.client.ClientResponse
import org.springframework.web.reactive.function.client.ExchangeFilterFunction
import org.springframework.web.reactive.function.client.ExchangeFunction
import reactor.core.publisher.Mono

private val logger = KotlinLogging.logger {}

class AuthenticationFilter(
    private val tokenManager: OAuth2TokenManager,
) : ExchangeFilterFunction {
    override fun filter(
        request: ClientRequest,
        next: ExchangeFunction,
    ): Mono<ClientResponse> =
        mono { tokenManager.getAccessToken() }
            .flatMap { token ->
                val authenticatedRequest =
                    ClientRequest
                        .from(request)
                        .header("Authorization", "Bearer $token")
                        .build()

                logger.debug { "Added Bearer token to ${request.method()} ${request.url()}" }
                next.exchange(authenticatedRequest)
            }
}
