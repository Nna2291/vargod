package com.vargod.engine

import com.vargod.domain.ValidationResult
import com.vargod.domain.Validator

interface ValidationEngine {
    fun validate(values: Map<String, Any>, validators: List<Validator>): List<ValidationResult>
}
