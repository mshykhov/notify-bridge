package com.smhomelab.notifier.api.http

import com.fasterxml.jackson.annotation.JsonProperty
import com.smhomelab.notifier.api.config.NotifierOAuth2Properties
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.reactor.awaitSingle
import org.springframework.http.MediaType
import org.springframework.util.LinkedMultiValueMap
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono

private val logger = KotlinLogging.logger {}

class ClientCredentialsTokenManager(
    private val properties: NotifierOAuth2Properties,
    private val tokenWebClient: WebClient,
) : OAuth2TokenManager {
    private val cache = TokenCache()

    override suspend fun getAccessToken(): String {
        cache.get()?.let { return it }
        return fetchNewToken()
    }

    override suspend fun refreshToken(): String {
        cache.clear()
        return fetchNewToken()
    }

    private suspend fun fetchNewToken(): String {
        logger.debug { "Fetching new OAuth2 token from ${properties.tokenUri}" }

        try {
            val formData =
                LinkedMultiValueMap<String, String>().apply {
                    add("grant_type", "client_credentials")
                    add("client_id", properties.clientId)
                    add("client_secret", properties.clientSecret)
                    if (properties.audience.isNotBlank()) {
                        add("audience", properties.audience)
                    }
                    if (properties.scopes.isNotEmpty()) {
                        add("scope", properties.scopes.joinToString(" "))
                    }
                }

            val response =
                tokenWebClient
                    .post()
                    .uri(properties.tokenUri)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(BodyInserters.fromFormData(formData))
                    .retrieve()
                    .bodyToMono<TokenResponse>()
                    .awaitSingle()

            logger.debug { "Successfully obtained OAuth2 token, expires in ${response.expiresIn}s" }

            cache.set(response.accessToken, response.expiresIn)
            return response.accessToken
        } catch (ex: Exception) {
            logger.error(ex) { "Failed to obtain OAuth2 token from ${properties.tokenUri}" }
            throw AuthenticationException("Failed to obtain access token: ${ex.message}", ex)
        }
    }

    private data class TokenResponse(
        @JsonProperty("access_token")
        val accessToken: String,
        @JsonProperty("token_type")
        val tokenType: String,
        @JsonProperty("expires_in")
        val expiresIn: Long,
    )
}
