package com.smhomelab.notifier.config

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator
import org.springframework.security.oauth2.core.OAuth2Error
import org.springframework.security.oauth2.core.OAuth2TokenValidator
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.JwtDecoders
import org.springframework.security.oauth2.jwt.JwtValidators
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder
import org.springframework.security.web.SecurityFilterChain

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@EnableConfigurationProperties(Auth0Properties::class)
class SecurityConfig(
    private val auth0Properties: Auth0Properties,
) {
    @Bean
    @ConditionalOnProperty(
        prefix = "notifier.auth0",
        name = ["enabled"],
        havingValue = "true",
        matchIfMissing = true,
    )
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .authorizeHttpRequests { auth ->
                auth
                    .requestMatchers("/actuator/**")
                    .permitAll()
                    .requestMatchers("/api/**")
                    .authenticated()
                    .anyRequest()
                    .permitAll()
            }.oauth2ResourceServer { oauth2 ->
                oauth2.jwt { jwt ->
                    jwt.decoder(jwtDecoder())
                }
            }.csrf { it.disable() }

        return http.build()
    }

    @Bean
    @ConditionalOnProperty(
        prefix = "notifier.auth0",
        name = ["enabled"],
        havingValue = "false",
    )
    fun disabledSecurityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .authorizeHttpRequests { auth ->
                auth.anyRequest().permitAll()
            }.csrf { it.disable() }

        return http.build()
    }

    @Bean
    @ConditionalOnProperty(
        prefix = "notifier.auth0",
        name = ["enabled"],
        havingValue = "true",
        matchIfMissing = true,
    )
    fun jwtDecoder(): JwtDecoder {
        val jwtDecoder = JwtDecoders.fromIssuerLocation(auth0Properties.issuer) as NimbusJwtDecoder

        val audienceValidator = AudienceValidator(auth0Properties.audience)
        val issuerValidator = JwtValidators.createDefaultWithIssuer(auth0Properties.issuer)

        jwtDecoder.setJwtValidator(
            DelegatingOAuth2TokenValidator(issuerValidator, audienceValidator),
        )

        return jwtDecoder
    }
}

class AudienceValidator(
    private val audience: String,
) : OAuth2TokenValidator<Jwt> {
    override fun validate(token: Jwt): OAuth2TokenValidatorResult =
        if (token.audience.contains(audience)) {
            OAuth2TokenValidatorResult.success()
        } else {
            OAuth2TokenValidatorResult.failure(
                OAuth2Error("invalid_token", "Required audience not found", null),
            )
        }
}
