package com.vargod.domain

import java.util.UUID

sealed class DomainException(message: String) : RuntimeException(message)

class NotFoundException(entity: String, id: UUID) :
    DomainException("$entity not found: id=$id")

/**
 * Аутентификация: пользователь не найден или пароль не совпал. 401 Unauthorized.
 * Намеренно НЕ раскрываем, что именно не совпало — защита от user-enumeration.
 */
class InvalidCredentialsException(message: String = "Invalid email or password") :
    DomainException(message)

/**
 * Попытка зарегистрировать email, который уже занят. 409 Conflict.
 */
class EmailAlreadyTakenException(email: String) :
    DomainException("Email already taken: $email")

/**
 * Авторизация (а не аутентификация): пользователь определён, но не имеет права
 * на действие — например, не владелец эксперимента. 403 Forbidden.
 */
class AuthorizationException(message: String) : DomainException(message)

class ValidationException(val results: List<ValidationResult>) :
    DomainException(
        "Validation failed: " + results
            .filter { it.status == ValidationStatus.FAIL }
            .joinToString("; ") { it.message ?: "validator ${it.validatorId} failed" }
    )

class IllegalExperimentStateException(message: String) : DomainException(message)

class PersistenceException(message: String, cause: Throwable? = null) : DomainException(message) {
    init { if (cause != null) initCause(cause) }
}
