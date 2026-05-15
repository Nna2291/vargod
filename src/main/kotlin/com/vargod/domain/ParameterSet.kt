package com.vargod.domain

import java.util.UUID

data class ParameterSet(
    val id: UUID,
    val values: Map<String, Double>,
    val tStart: Double,
    val tEnd: Double,
    val step: Double,
)
