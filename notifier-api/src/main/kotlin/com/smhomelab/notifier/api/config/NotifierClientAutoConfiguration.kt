package com.smhomelab.notifier.api.config

import com.smhomelab.notifier.api.client.NotifierClient
import com.smhomelab.notifier.api.client.NotifierClientImpl
import com.smhomelab.notifier.api.http.AuthenticationFilter
import com.smhomelab.notifier.api.http.ClientCredentialsTokenManager
import com.smhomelab.notifier.api.http.LoggingFilter
import com.smhomelab.notifier.api.http.OAuth2TokenManager
import com.smhomelab.notifier.api.http.RetryFilter
import io.netty.channel.ChannelOption
import io.netty.handler.timeout.ReadTimeoutHandler
import io.netty.handler.timeout.WriteTimeoutHandler
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.web.reactive.function.client.WebClient
import reactor.netty.http.client.HttpClient
import java.util.concurrent.TimeUnit

@AutoConfiguration
@EnableConfigurationProperties(NotifierClientProperties::class)
@ConditionalOnProperty(
    prefix = "smhomelab.notifier",
    name = ["enabled"],
    havingValue = "true",
    matchIfMissing = true,
)
class NotifierClientAutoConfiguration(
    private val properties: NotifierClientProperties,
) {
    @Bean
    @ConditionalOnMissingBean(name = ["notifierTokenManager"])
    @ConditionalOnProperty(
        prefix = "smhomelab.notifier.oauth2",
        name = ["enabled"],
        havingValue = "true",
        matchIfMissing = true,
    )
    fun notifierTokenManager(): OAuth2TokenManager {
        val tokenWebClient =
            WebClient
                .builder()
                .filter(LoggingFilter())
                .build()

        return ClientCredentialsTokenManager(properties.oauth2, tokenWebClient)
    }

    @Bean
    @ConditionalOnMissingBean(name = ["notifierWebClient"])
    fun notifierWebClient(notifierTokenManager: OAuth2TokenManager?): WebClient {
        val httpClient =
            HttpClient
                .create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, properties.connectTimeout.toMillis().toInt())
                .doOnConnected { conn ->
                    conn
                        .addHandlerLast(
                            ReadTimeoutHandler(properties.readTimeout.toSeconds(), TimeUnit.SECONDS),
                        ).addHandlerLast(
                            WriteTimeoutHandler(properties.readTimeout.toSeconds(), TimeUnit.SECONDS),
                        )
                }

        val builder =
            WebClient
                .builder()
                .baseUrl(properties.baseUrl)
                .clientConnector(ReactorClientHttpConnector(httpClient))
                .filter(LoggingFilter())

        if (properties.oauth2.enabled && notifierTokenManager != null) {
            builder.filter(AuthenticationFilter(notifierTokenManager))
        }

        if (properties.retry.enabled) {
            builder.filter(RetryFilter(properties.retry))
        }

        return builder.build()
    }

    @Bean
    @ConditionalOnMissingBean
    fun notifierClient(notifierWebClient: WebClient): NotifierClient = NotifierClientImpl(notifierWebClient)
}
