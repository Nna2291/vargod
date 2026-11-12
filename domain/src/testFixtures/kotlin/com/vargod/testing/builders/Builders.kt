package com.vargod.testing.builders

import com.vargod.domain.Experiment
import com.vargod.domain.ExperimentStatus
import com.vargod.domain.ModelTemplate
import com.vargod.domain.ParameterSet
import com.vargod.domain.SimulationResult
import com.vargod.domain.Validator
import com.vargod.domain.ValidatorTargetKind
import com.vargod.domain.ValidatorType
import com.vargod.domain.dto.CreateExperimentRequest
import com.vargod.testing.TestTime
import java.time.Instant
import java.util.UUID

/*
 * Test Data Builder (Nat Pryce): у каждого поля есть разумное значение по умолчанию,
 * тест переопределяет только то, что важно для проверяемого поведения:
 *
 *     val experiment = anExperiment().ownedBy(owner).asPublic().build()
 *
 * Так тест не ломается при добавлении нового поля в сущность (устойчивость
 * к рефакторингу), а из кода видно, какие данные значимы для теста.
 * Фабричные функции anExperiment(), aModelTemplate() и т.п. — точка входа в builder.
 */

private fun shortId(): String = UUID.randomUUID().toString().take(8)

fun aParameterSet() = ParameterSetBuilder()
fun aSimulationResult() = SimulationResultBuilder()
fun anExperiment() = ExperimentBuilder()
fun aModelTemplate() = ModelTemplateBuilder()
fun aValidator() = ValidatorBuilder()
fun aCreateExperimentRequest() = CreateExperimentRequestBuilder()

class ParameterSetBuilder {
    private var id: UUID = UUID.randomUUID()
    private var values: Map<String, Double> = mapOf("P" to 10.0, "r" to 0.5, "K" to 100.0)
    private var tStart: Double = 0.0
    private var tEnd: Double = 10.0
    private var step: Double = 1.0

    fun withId(id: UUID) = apply { this.id = id }
    fun withValues(values: Map<String, Double>) = apply { this.values = values }
    fun withValues(vararg values: Pair<String, Double>) = apply { this.values = mapOf(*values) }
    fun withTimeRange(tStart: Double, tEnd: Double) = apply { this.tStart = tStart; this.tEnd = tEnd }
    fun withStep(step: Double) = apply { this.step = step }

    fun build() = ParameterSet(id, values, tStart, tEnd, step)
}

class SimulationResultBuilder {
    private var id: UUID = UUID.randomUUID()
    private var timePoints: List<Double> = listOf(0.0, 1.0, 2.0)
    private var variables: Map<String, List<Double>> = mapOf("P" to listOf(10.0, 15.6, 23.2))
    private var createdAt: Instant = TestTime.NOW

    fun withId(id: UUID) = apply { this.id = id }
    fun withTimePoints(vararg t: Double) = apply { this.timePoints = t.toList() }
    fun withSeries(name: String, vararg values: Double) = apply { this.variables = this.variables + (name to values.toList()) }
    fun withVariables(variables: Map<String, List<Double>>) = apply { this.variables = variables }
    fun createdAt(instant: Instant) = apply { this.createdAt = instant }

    fun build() = SimulationResult(id, timePoints, variables, createdAt)
}

class ExperimentBuilder {
    private var id: UUID = UUID.randomUUID()
    private var name: String? = "experiment-${shortId()}"
    private var status: ExperimentStatus = ExperimentStatus.PENDING
    private var isPublic: Boolean = false
    private var userId: UUID = UUID.randomUUID()
    private var modelTemplateId: UUID = UUID.randomUUID()
    private var parameterSet: ParameterSet? = aParameterSet().build()
    private var result: SimulationResult? = null
    private var createdAt: Instant = TestTime.NOW
    private var validatorSnapshot: List<Validator> = emptyList()

    fun withId(id: UUID) = apply { this.id = id }
    fun named(name: String?) = apply { this.name = name }
    fun withStatus(status: ExperimentStatus) = apply { this.status = status }
    fun pending() = withStatus(ExperimentStatus.PENDING)
    fun running() = withStatus(ExperimentStatus.RUNNING)
    fun failed() = withStatus(ExperimentStatus.FAILED)
    fun completed(result: SimulationResult = aSimulationResult().build()) =
        apply { this.status = ExperimentStatus.COMPLETED; this.result = result }
    fun asPublic() = apply { this.isPublic = true }
    fun asPrivate() = apply { this.isPublic = false }
    fun ownedBy(userId: UUID) = apply { this.userId = userId }
    fun forModel(modelTemplateId: UUID) = apply { this.modelTemplateId = modelTemplateId }
    fun withParameterSet(parameterSet: ParameterSet?) = apply { this.parameterSet = parameterSet }
    fun withoutParameterSet() = withParameterSet(null)
    fun withResult(result: SimulationResult?) = apply { this.result = result }
    fun createdAt(instant: Instant) = apply { this.createdAt = instant }
    fun withValidatorSnapshot(validators: List<Validator>) = apply { this.validatorSnapshot = validators }

    fun build() = Experiment(
        id = id,
        name = name,
        status = status,
        isPublic = isPublic,
        userId = userId,
        modelTemplateId = modelTemplateId,
        parameterSet = parameterSet,
        result = result,
        createdAt = createdAt,
        validatorSnapshot = validatorSnapshot,
    )
}

class ModelTemplateBuilder {
    private var id: UUID = UUID.randomUUID()
    private var code: String = "LOGISTIC"
    private var name: String = "Logistic growth"
    private var description: String? = "dP/dt = rP(1 - P/K)"
    private var solverType: String = "RK45"
    private var validators: List<Validator> = emptyList()
    private var recommendedMaxStep: Double? = null
    private var recommendedRelTol: Double? = null
    private var recommendedAbsTol: Double? = null

    fun withId(id: UUID) = apply { this.id = id }
    fun withCode(code: String) = apply { this.code = code; this.name = "Model $code" }
    fun named(name: String) = apply { this.name = name }
    fun withDescription(description: String?) = apply { this.description = description }
    fun withSolverType(solverType: String) = apply { this.solverType = solverType }
    fun withValidators(vararg validators: Validator) = apply { this.validators = validators.toList() }
    fun withValidators(validators: List<Validator>) = apply { this.validators = validators }
    fun withSolverHints(maxStep: Double?, relTol: Double?, absTol: Double?) = apply {
        this.recommendedMaxStep = maxStep
        this.recommendedRelTol = relTol
        this.recommendedAbsTol = absTol
    }

    fun build() = ModelTemplate(
        id = id,
        code = code,
        name = name,
        description = description,
        solverType = solverType,
        validators = validators,
        recommendedMaxStep = recommendedMaxStep,
        recommendedRelTol = recommendedRelTol,
        recommendedAbsTol = recommendedAbsTol,
    )
}

class ValidatorBuilder {
    private var id: UUID = UUID.randomUUID()
    private var name: String = "validator-${shortId()}"
    private var checkType: ValidatorType = ValidatorType.RANGE
    private var targetKind: ValidatorTargetKind = ValidatorTargetKind.PARAMETER
    private var targetField: String = "P"
    private var rules: Map<String, Any> = mapOf("min" to 0.0)

    fun withId(id: UUID) = apply { this.id = id }
    fun named(name: String) = apply { this.name = name }
    fun targeting(field: String) = apply { this.targetField = field }
    fun withTargetKind(kind: ValidatorTargetKind) = apply { this.targetKind = kind }
    fun withRules(rules: Map<String, Any>) = apply { this.rules = rules }

    /** RANGE-валидатор поля [field]; null-граница не записывается в rules. */
    fun range(
        field: String,
        min: Double? = null,
        max: Double? = null,
        minExclusive: Boolean = false,
        maxExclusive: Boolean = false,
    ) = apply {
        this.checkType = ValidatorType.RANGE
        this.targetField = field
        this.name = "range[$field]"
        this.rules = buildMap {
            min?.let { put("min", it) }
            max?.let { put("max", it) }
            if (minExclusive) put("minExclusive", true)
            if (maxExclusive) put("maxExclusive", true)
        }
    }

    /** CONSTRAINT-валидатор: тип правила кладётся в rules["type"], как это делает DataSeeder. */
    fun constraint(type: String, params: Map<String, Any> = emptyMap()) = apply {
        this.checkType = ValidatorType.CONSTRAINT
        this.targetField = "*"
        this.name = "constraint[$type]"
        this.rules = mapOf("type" to type) + params
    }

    /** CONSTRAINT-валидатор без rules["type"] — заведомо некорректная конфигурация. */
    fun constraintWithoutType(params: Map<String, Any> = emptyMap()) = apply {
        this.checkType = ValidatorType.CONSTRAINT
        this.targetField = "*"
        this.rules = params
    }

    fun build() = Validator(id, name, checkType, targetKind, targetField, rules)
}

class CreateExperimentRequestBuilder {
    private var name: String? = "request-${shortId()}"
    private var modelTemplateId: UUID = UUID.randomUUID()
    private var parameters: Map<String, Double> = mapOf("P" to 10.0, "r" to 0.5, "K" to 100.0)
    private var tStart: Double = 0.0
    private var tEnd: Double = 10.0
    private var step: Double = 1.0

    fun named(name: String?) = apply { this.name = name }
    fun forModel(modelTemplateId: UUID) = apply { this.modelTemplateId = modelTemplateId }
    fun withParameters(parameters: Map<String, Double>) = apply { this.parameters = parameters }
    fun withParameters(vararg parameters: Pair<String, Double>) = apply { this.parameters = mapOf(*parameters) }
    fun withParameter(name: String, value: Double) = apply { this.parameters = this.parameters + (name to value) }
    fun withTimeRange(tStart: Double, tEnd: Double) = apply { this.tStart = tStart; this.tEnd = tEnd }
    fun withStep(step: Double) = apply { this.step = step }

    fun build() = CreateExperimentRequest(name, modelTemplateId, parameters, tStart, tEnd, step)
}
