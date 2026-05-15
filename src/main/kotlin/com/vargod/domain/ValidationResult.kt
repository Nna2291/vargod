package com.vargod.domain

import java.util.UUID

data class ValidationResult(
    val status: ValidationStatus,
    val message: String?,
    val validatorId: UUID,
)

enum class ValidationStatus { PASS, FAIL }
