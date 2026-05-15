package com.vargod.repository.jdbc

import com.vargod.domain.Experiment
import com.vargod.domain.ExperimentStatus
import com.vargod.domain.ModelTemplate
import com.vargod.domain.ParameterSet
import com.vargod.domain.SimulationResult
import com.vargod.domain.Validator
import com.vargod.domain.ValidatorTargetKind
import com.vargod.domain.ValidatorType
import java.sql.ResultSet
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

internal fun ResultSet.readUuid(column: String): UUID = getObject(column, UUID::class.java)
    ?: error("expected non-null UUID column '$column'")

internal fun ResultSet.readInstant(column: String) =
    getObject(column, OffsetDateTime::class.java).toInstant()

internal fun OffsetDateTimeUtc(instant: java.time.Instant): OffsetDateTime =
    instant.atOffset(ZoneOffset.UTC)

internal fun ResultSet.readExperimentRow(): ExperimentRow = ExperimentRow(
    id = readUuid("id"),
    name = getString("name"),
    status = ExperimentStatus.valueOf(getString("status")),
    isPublic = getBoolean("is_public"),
    userId = readUuid("user_id"),
    modelTemplateId = readUuid("model_template_id"),
    createdAt = readInstant("created_at"),
)

internal data class ExperimentRow(
    val id: UUID,
    val name: String?,
    val status: ExperimentStatus,
    val isPublic: Boolean,
    val userId: UUID,
    val modelTemplateId: UUID,
    val createdAt: java.time.Instant,
) {
    fun assemble(parameterSet: ParameterSet?, result: SimulationResult?): Experiment = Experiment(
        id = id,
        name = name,
        status = status,
        isPublic = isPublic,
        userId = userId,
        modelTemplateId = modelTemplateId,
        parameterSet = parameterSet,
        result = result,
        createdAt = createdAt,
    )
}

internal fun ResultSet.readParameterSet(codec: JsonCodec): ParameterSet = ParameterSet(
    id = readUuid("id"),
    values = codec.decodeDoubleMap(getString("values")), // column name is case-insensitive at ResultSet API level
    tStart = getDouble("t_start"),
    tEnd = getDouble("t_end"),
    step = getDouble("step"),
)

internal fun ResultSet.readSimulationResult(codec: JsonCodec): SimulationResult {
    val (times, vars) = codec.decodeSimulationData(getString("data"))
    return SimulationResult(
        id = readUuid("id"),
        timePoints = times,
        variables = vars,
        createdAt = readInstant("created_at"),
    )
}

internal fun ResultSet.readModelTemplate(validators: List<Validator>): ModelTemplate = ModelTemplate(
    id = readUuid("id"),
    code = getString("code"),
    name = getString("name"),
    description = getString("description"),
    solverType = getString("solver_type"),
    validators = validators,
)

internal fun ResultSet.readValidator(codec: JsonCodec): Validator = Validator(
    id = readUuid("id"),
    name = getString("name"),
    checkType = ValidatorType.valueOf(getString("check_type")),
    targetKind = ValidatorTargetKind.valueOf(getString("target_kind")),
    targetField = getString("target_field"),
    rules = codec.decodeAnyMap(getString("rules")),
)
