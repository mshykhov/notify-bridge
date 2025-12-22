package com.smhomelab.notifier.config

import com.smhomelab.notifier.api.model.ErrorResponse
import com.smhomelab.notifier.exception.ChannelDisabledException
import com.smhomelab.notifier.exception.ConfigurationMissingException
import com.smhomelab.notifier.exception.SendFailedException
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.security.access.AccessDeniedException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

private val logger = KotlinLogging.logger {}

@RestControllerAdvice(basePackages = ["com.smhomelab.notifier.controller"])
class ApiExceptionHandler {
    @ExceptionHandler(IllegalArgumentException::class)
    fun handleValidationError(
        ex: IllegalArgumentException,
        request: HttpServletRequest,
    ): ResponseEntity<ErrorResponse> {
        logger.warn { "Validation error: ${ex.message}" }
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(
                ErrorResponse(
                    error = "VALIDATION_ERROR",
                    message = ex.message ?: "Invalid request",
                    path = request.requestURI,
                ),
            )
    }

    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleParseError(
        ex: HttpMessageNotReadableException,
        request: HttpServletRequest,
    ): ResponseEntity<ErrorResponse> {
        logger.warn { "Parse error: ${ex.message}" }
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(
                ErrorResponse(
                    error = "PARSE_ERROR",
                    message = "Invalid JSON request body",
                    path = request.requestURI,
                ),
            )
    }

    @ExceptionHandler(ChannelDisabledException::class)
    fun handleChannelDisabled(
        ex: ChannelDisabledException,
        request: HttpServletRequest,
    ): ResponseEntity<ErrorResponse> {
        logger.warn { "Channel disabled: ${ex.channel}" }
        return ResponseEntity
            .status(HttpStatus.SERVICE_UNAVAILABLE)
            .body(
                ErrorResponse(
                    error = "CHANNEL_DISABLED",
                    message = ex.message,
                    details = mapOf("channel" to ex.channel),
                    path = request.requestURI,
                ),
            )
    }

    @ExceptionHandler(ConfigurationMissingException::class)
    fun handleConfigurationMissing(
        ex: ConfigurationMissingException,
        request: HttpServletRequest,
    ): ResponseEntity<ErrorResponse> {
        logger.warn { "Configuration missing: ${ex.message}" }
        return ResponseEntity
            .status(HttpStatus.SERVICE_UNAVAILABLE)
            .body(
                ErrorResponse(
                    error = "CONFIGURATION_MISSING",
                    message = ex.message,
                    path = request.requestURI,
                ),
            )
    }

    @ExceptionHandler(SendFailedException::class)
    fun handleSendFailed(
        ex: SendFailedException,
        request: HttpServletRequest,
    ): ResponseEntity<ErrorResponse> {
        logger.error { "Send failed: ${ex.channel} - ${ex.message}" }
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(
                ErrorResponse(
                    error = "SEND_FAILED",
                    message = ex.message,
                    details = mapOf("channel" to ex.channel),
                    path = request.requestURI,
                ),
            )
    }

    @ExceptionHandler(AccessDeniedException::class)
    fun handleAccessDenied(
        ex: AccessDeniedException,
        request: HttpServletRequest,
    ): ResponseEntity<ErrorResponse> {
        logger.warn { "Access denied: ${ex.message}" }
        return ResponseEntity
            .status(HttpStatus.FORBIDDEN)
            .body(
                ErrorResponse(
                    error = "INSUFFICIENT_SCOPE",
                    message = "Missing required scope for this endpoint",
                    path = request.requestURI,
                ),
            )
    }

    @ExceptionHandler(Exception::class)
    fun handleGenericError(
        ex: Exception,
        request: HttpServletRequest,
    ): ResponseEntity<ErrorResponse> {
        logger.error(ex) { "Unhandled error: ${ex.message}" }
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(
                ErrorResponse(
                    error = "INTERNAL_ERROR",
                    message = "An unexpected error occurred",
                    path = request.requestURI,
                ),
            )
    }
}
