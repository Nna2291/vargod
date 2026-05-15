package com.vargod.domain

import java.util.UUID

data class ModelTemplate(
    val id: UUID,
    val code: String,
    val name: String,
    val description: String?,
    val solverType: String,
    val validators: List<Validator>,
)
