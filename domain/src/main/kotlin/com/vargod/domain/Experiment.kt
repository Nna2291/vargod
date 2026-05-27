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
    /**
     * Снимок валидаторов модели на момент создания эксперимента (P2).
     * Дефолт emptyList() — для старых сериализаций и тестов, которые
     * не передают snapshot явно.
     */
    val validatorSnapshot: List<Validator> = emptyList(),
)

enum class ExperimentStatus { PENDING, RUNNING, COMPLETED, FAILED }
