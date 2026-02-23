package com.smhomelab.notifier.api.http

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import org.springframework.web.reactive.function.client.bodyToMono
import java.io.IOException

private val logger = KotlinLogging.logger {}

open class RestClientImpl(
    override val webClient: WebClient,
) : RestClient {
    override suspend fun <T : Any> get(
        path: String,
        responseType: Class<T>,
        headers: Map<String, String>,
    ): ClientResponse<T> =
        executeRequest {
            executeWebClientRequest(
                webClient.get().uri(path),
                headers,
                responseType,
            )
        }

    override suspend fun <T : Any, R : Any> post(
        path: String,
        body: T,
        responseType: Class<R>,
        headers: Map<String, String>,
    ): ClientResponse<R> =
        executeRequest {
            executeWebClientRequest(
                webClient.post().uri(path).bodyValue(body),
                headers,
                responseType,
            )
        }

    override suspend fun <T : Any, R : Any> put(
        path: String,
        body: T,
        responseType: Class<R>,
        headers: Map<String, String>,
    ): ClientResponse<R> =
        executeRequest {
            executeWebClientRequest(
                webClient.put().uri(path).bodyValue(body),
                headers,
                responseType,
            )
        }

    override suspend fun delete(
        path: String,
        headers: Map<String, String>,
    ): ClientResponse<Unit> =
        executeRequest {
            val spec = webClient.delete().uri(path)
            headers.forEach { (key, value) -> spec.header(key, value) }
            spec.retrieve().bodyToMono(Void::class.java).awaitSingleOrNull()
            Unit
        }

    @Suppress("UNCHECKED_CAST")
    private suspend fun <T : Any> executeWebClientRequest(
        spec: WebClient.RequestHeadersSpec<*>,
        headers: Map<String, String>,
        responseType: Class<T>,
    ): T {
        headers.forEach { (key, value) -> spec.header(key, value) }

        if (responseType == Unit::class.java) {
            spec.retrieve().bodyToMono(Void::class.java).awaitSingleOrNull()
            return Unit as T
        }

        val response =
            spec
                .retrieve()
                .bodyToMono(responseType)
                .awaitSingleOrNull()

        return response ?: throw ClientException(
            statusCode = 0,
            error = "EMPTY_RESPONSE",
            message = "Empty response body",
        )
    }

    protected open suspend fun <T> executeRequest(request: suspend () -> T): ClientResponse<T> =
        try {
            ClientResponse.Success(request())
        } catch (ex: WebClientResponseException) {
            logger.warn(ex) { "HTTP error: ${ex.statusCode} ${ex.responseBodyAsString}" }
            handleHttpError(ex)
        } catch (ex: IOException) {
            logger.error(ex) { "Network error: ${ex.message}" }
            throw NetworkException("Network communication failed", ex)
        } catch (ex: ClientException) {
            throw ex
        } catch (ex: Exception) {
            logger.error(ex) { "Unexpected error: ${ex.message}" }
            throw ClientException(
                statusCode = 0,
                error = "UNKNOWN_ERROR",
                message = ex.message ?: "Unknown error occurred",
                cause = ex,
            )
        }

    protected open fun <T> handleHttpError(ex: WebClientResponseException): ClientResponse<T> {
        val statusCode = ex.statusCode.value()
        val errorBody = tryParseErrorResponse(ex)

        return ClientResponse.Error(
            statusCode = statusCode,
            error = errorBody?.error ?: "HTTP_$statusCode",
            message = errorBody?.message ?: ex.message ?: "HTTP error",
            details = errorBody?.details,
        )
    }

    private fun tryParseErrorResponse(ex: WebClientResponseException): ErrorResponseDto? =
        try {
            ex.getResponseBodyAs(ErrorResponseDto::class.java)
        } catch (parseEx: Exception) {
            null
        }

    private data class ErrorResponseDto(
        val error: String,
        val message: String,
        val details: Map<String, String>? = null,
    )
}
