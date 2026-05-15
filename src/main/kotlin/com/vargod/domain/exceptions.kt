package com.vargod.domain

import java.util.UUID

sealed class DomainException(message: String) : RuntimeException(message)

class NotFoundException(entity: String, id: UUID) :
    DomainException("$entity not found: id=$id")

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
