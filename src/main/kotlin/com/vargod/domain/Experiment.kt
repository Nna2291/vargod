package com.vargod.domain

import java.time.Instant
import java.util.UUID

data class Experiment(
    val id: UUID,
    val name: String?,
    val status: ExperimentStatus,
    val isPublic: Boolean,
    val userId: UUID,
    val modelTemplateId: UUID,
    val parameterSet: ParameterSet?,
    val result: SimulationResult?,
    val createdAt: Instant,
)

enum class ExperimentStatus { PENDING, RUNNING, COMPLETED, FAILED }
