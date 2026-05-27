package com.vargod.testfixtures

import com.vargod.domain.Experiment
import com.vargod.domain.ExperimentStatus
import com.vargod.domain.ModelTemplate
import com.vargod.domain.ParameterSet
import com.vargod.domain.SimulationResult
import com.vargod.domain.Validator
import com.vargod.domain.ValidatorTargetKind
import com.vargod.domain.ValidatorType
import java.time.Instant
import java.util.UUID

object Fixtures {

    fun model(
        id: UUID = UUID.randomUUID(),
        code: String = "SIR",
        validators: List<Validator> = emptyList(),
    ): ModelTemplate = ModelTemplate(
        id = id,
        code = code,
        name = "Model $code",
        description = null,
        solverType = "RK45",
        validators = validators,
    )

    fun rangeValidator(
        targetField: String,
        min: Double? = null,
        max: Double? = null,
        minExclusive: Boolean = false,
        maxExclusive: Boolean = false,
    ): Validator {
        val rules = buildMap<String, Any> {
            min?.let { put("min", it) }
            max?.let { put("max", it) }
            if (minExclusive) put("minExclusive", true)
            if (maxExclusive) put("maxExclusive", true)
        }
        return Validator(
            id = UUID.randomUUID(),
            name = "range[$targetField]",
            checkType = ValidatorType.RANGE,
            targetKind = ValidatorTargetKind.PARAMETER,
            targetField = targetField,
            rules = rules,
        )
    }

    fun constraintValidator(
        type: String,
        targetField: String = "*",
        extraRules: Map<String, Any> = emptyMap(),
    ): Validator = Validator(
        id = UUID.randomUUID(),
        name = "constraint[$type]",
        checkType = ValidatorType.CONSTRAINT,
        targetKind = ValidatorTargetKind.PARAMETER,
        targetField = targetField,
        rules = mapOf("type" to type) + extraRules,
    )

    fun parameterSet(
        values: Map<String, Double> = mapOf("P" to 10.0),
        tStart: Double = 0.0,
        tEnd: Double = 1.0,
        step: Double = 0.1,
    ): ParameterSet = ParameterSet(
        id = UUID.randomUUID(),
        values = values,
        tStart = tStart,
        tEnd = tEnd,
        step = step,
    )

    fun experiment(
        id: UUID = UUID.randomUUID(),
        userId: UUID = UUID.randomUUID(),
        modelTemplateId: UUID = UUID.randomUUID(),
        status: ExperimentStatus = ExperimentStatus.PENDING,
        isPublic: Boolean = false,
        parameterSet: ParameterSet? = parameterSet(),
        result: SimulationResult? = null,
    ): Experiment = Experiment(
        id = id,
        name = "exp-$id",
        status = status,
        isPublic = isPublic,
        userId = userId,
        modelTemplateId = modelTemplateId,
        parameterSet = parameterSet,
        result = result,
        createdAt = Instant.parse("2026-05-13T10:00:00Z"),
    )
}
