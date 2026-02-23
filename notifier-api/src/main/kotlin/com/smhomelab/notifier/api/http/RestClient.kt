package com.smhomelab.notifier.api.http

import org.springframework.web.reactive.function.client.WebClient

interface RestClient {
    val webClient: WebClient

    suspend fun <T : Any> get(
        path: String,
        responseType: Class<T>,
        headers: Map<String, String> = emptyMap(),
    ): ClientResponse<T>

    suspend fun <T : Any, R : Any> post(
        path: String,
        body: T,
        responseType: Class<R>,
        headers: Map<String, String> = emptyMap(),
    ): ClientResponse<R>

    suspend fun <T : Any, R : Any> put(
        path: String,
        body: T,
        responseType: Class<R>,
        headers: Map<String, String> = emptyMap(),
    ): ClientResponse<R>

    suspend fun delete(
        path: String,
        headers: Map<String, String> = emptyMap(),
    ): ClientResponse<Unit>
}

suspend inline fun <reified T : Any> RestClient.get(
    path: String,
    headers: Map<String, String> = emptyMap(),
): ClientResponse<T> = get(path, T::class.java, headers)

suspend inline fun <T : Any, reified R : Any> RestClient.post(
    path: String,
    body: T,
    headers: Map<String, String> = emptyMap(),
): ClientResponse<R> = post(path, body, R::class.java, headers)

suspend inline fun <T : Any, reified R : Any> RestClient.put(
    path: String,
    body: T,
    headers: Map<String, String> = emptyMap(),
): ClientResponse<R> = put(path, body, R::class.java, headers)
