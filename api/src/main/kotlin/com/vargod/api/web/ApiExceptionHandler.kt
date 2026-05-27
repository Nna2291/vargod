package com.vargod.api.web

import com.vargod.api.web.dto.ApiError
import com.vargod.domain.AuthorizationException
import com.vargod.domain.EmailAlreadyTakenException
import com.vargod.domain.IllegalExperimentStateException
import com.vargod.domain.InvalidCredentialsException
import com.vargod.domain.NotFoundException
import com.vargod.domain.PersistenceException
import com.vargod.domain.ValidationException
import com.vargod.domain.ValidationStatus
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.core.AuthenticationException
import org.springframework.web.bind.MissingRequestHeaderException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

/**
 * Единая точка обработки исключений REST API — аналог catch-блоков MainLoop в CLI.
 * Все исключительные ситуации логируются и преобразуются в корректные HTTP-ответы.
 *
 * Семантическое разделение 401 vs 403:
 *  - 401 UNAUTHORIZED — не аутентифицирован или ошибочные учётные данные;
 *  - 403 FORBIDDEN — аутентифицирован, но прав на действие нет (не владелец, не ADMIN).
 */
@RestControllerAdvice
class ApiExceptionHandler {

    private val log = LoggerFactory.getLogger(ApiExceptionHandler::class.java)

    @ExceptionHandler(NotFoundException::class)
    fun handleNotFound(e: NotFoundException): ResponseEntity<ApiError> {
        log.warn("404 NOT_FOUND: {}", e.message)
        return response(HttpStatus.NOT_FOUND, "NOT_FOUND", e.message)
    }

    @ExceptionHandler(ValidationException::class)
    fun handleValidation(e: ValidationException): ResponseEntity<ApiError> {
        log.warn("422 VALIDATION_FAILED: {}", e.message)
        val details = e.results
            .filter { it.status == ValidationStatus.FAIL }
            .map { it.message ?: "validator ${it.validatorId}" }
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
            .body(ApiError("VALIDATION_FAILED", e.message ?: "validation failed", details))
    }

    @ExceptionHandler(InvalidCredentialsException::class)
    fun handleUnauthorized(e: InvalidCredentialsException): ResponseEntity<ApiError> {
        log.warn("401 UNAUTHORIZED: {}", e.message)
        return response(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", e.message)
    }

    @ExceptionHandler(AuthenticationException::class)
    fun handleSpringAuth(e: AuthenticationException): ResponseEntity<ApiError> {
        log.warn("401 UNAUTHORIZED (spring): {}", e.message)
        return response(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", e.message)
    }

    @ExceptionHandler(AuthorizationException::class)
    fun handleForbidden(e: AuthorizationException): ResponseEntity<ApiError> {
        log.warn("403 FORBIDDEN: {}", e.message)
        return response(HttpStatus.FORBIDDEN, "FORBIDDEN", e.message)
    }

    @ExceptionHandler(AccessDeniedException::class)
    fun handleSpringAccessDenied(e: AccessDeniedException): ResponseEntity<ApiError> {
        log.warn("403 FORBIDDEN (spring): {}", e.message)
        return response(HttpStatus.FORBIDDEN, "FORBIDDEN", e.message)
    }

    @ExceptionHandler(EmailAlreadyTakenException::class)
    fun handleEmailTaken(e: EmailAlreadyTakenException): ResponseEntity<ApiError> {
        log.warn("409 EMAIL_TAKEN: {}", e.message)
        return response(HttpStatus.CONFLICT, "EMAIL_TAKEN", e.message)
    }

    @ExceptionHandler(IllegalExperimentStateException::class)
    fun handleConflict(e: IllegalExperimentStateException): ResponseEntity<ApiError> {
        log.warn("409 ILLEGAL_STATE: {}", e.message)
        return response(HttpStatus.CONFLICT, "ILLEGAL_STATE", e.message)
    }

    @ExceptionHandler(IllegalArgumentException::class, MissingRequestHeaderException::class)
    fun handleBadRequest(e: Exception): ResponseEntity<ApiError> {
        log.warn("400 BAD_REQUEST: {}", e.message)
        return response(HttpStatus.BAD_REQUEST, "BAD_REQUEST", e.message)
    }

    @ExceptionHandler(PersistenceException::class)
    fun handlePersistence(e: PersistenceException): ResponseEntity<ApiError> {
        log.error("500 PERSISTENCE_ERROR: {}", e.message, e)
        return response(HttpStatus.INTERNAL_SERVER_ERROR, "PERSISTENCE_ERROR", e.message)
    }

    @ExceptionHandler(Exception::class)
    fun handleGeneric(e: Exception): ResponseEntity<ApiError> {
        log.error("500 INTERNAL_ERROR: {}", e.message, e)
        return response(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", e.message)
    }

    private fun response(status: HttpStatus, code: String, message: String?): ResponseEntity<ApiError> =
        ResponseEntity.status(status).body(ApiError(code, message ?: code))
}
