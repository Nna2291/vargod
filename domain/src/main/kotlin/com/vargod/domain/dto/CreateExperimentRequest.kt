package com.vargod.domain.dto

import java.util.UUID

data class CreateExperimentRequest(
    val name: String?,
    val modelTemplateId: UUID,
    val parameters: Map<String, Double>,
    val tStart: Double,
    val tEnd: Double,
    val step: Double,
)
