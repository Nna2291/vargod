package com.vargod.domain

import java.time.Instant
import java.util.UUID

data class SimulationResult(
    val id: UUID,
    val timePoints: List<Double>,
    val variables: Map<String, List<Double>>,
    val createdAt: Instant,
)
