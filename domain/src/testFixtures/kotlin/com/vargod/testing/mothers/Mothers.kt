package com.vargod.testing.mothers

import com.vargod.domain.Experiment
import com.vargod.domain.ModelTemplate
import com.vargod.domain.ParameterSet
import com.vargod.domain.User
import com.vargod.domain.Validator
import com.vargod.domain.dto.CreateExperimentRequest
import com.vargod.testing.builders.aCreateExperimentRequest
import com.vargod.testing.builders.aModelTemplate
import com.vargod.testing.builders.aParameterSet
import com.vargod.testing.builders.aSimulationResult
import com.vargod.testing.builders.aUser
import com.vargod.testing.builders.aValidator
import com.vargod.testing.builders.anExperiment
import java.util.UUID

/*
 * Object Mother (фабрика тестовых объектов): именованные, узнаваемые в предметной
 * области экземпляры — «администратор», «завершённый публичный эксперимент»,
 * «шаблон логистической модели с валидаторами». Тест говорит, КАКОЙ объект
 * ему нужен, а не КАК его собрать. Внутри матери используют Data Builder'ы,
 * поэтому результат можно «докрутить» под конкретный тест.
 */

object UserMother {
    fun researcher(email: String = "alice@vargod.test"): User = aUser().withEmail(email).asResearcher().build()
    fun anotherResearcher(): User = aUser().withEmail("bob@vargod.test").asResearcher().build()
    fun admin(email: String = "admin@vargod.test"): User = aUser().withEmail(email).asAdmin().build()

    /** Пользователь из legacy-бэкфилла V3: хеш '!INVALID' не является BCrypt. */
    fun legacyWithInvalidHash(): User = aUser().withEmail("legacy@vargod.local").withPasswordHash("!INVALID").build()
}

object ValidatorMother {
    /** P > 0 (строго) — популяция должна быть положительной. */
    fun positive(field: String): Validator = aValidator().range(field, min = 0.0, minExclusive = true).build()

    /** Значение-доля в [0, 1]. */
    fun fraction(field: String): Validator = aValidator().range(field, min = 0.0, max = 1.0).build()

    /** S + I + R = N (SUM_EQUALS_FIELD). */
    fun sirPopulationConservation(): Validator = aValidator().constraint(
        "SUM_EQUALS_FIELD",
        mapOf("fields" to listOf("S", "I", "R"), "expectedField" to "N", "tolerance" to 1e-6),
    ).build()

    /** Сумма долей = 1 (SUM_EQUALS_VALUE). */
    fun fractionsSumToOne(vararg fields: String): Validator = aValidator().constraint(
        "SUM_EQUALS_VALUE",
        mapOf("fields" to fields.toList(), "expected" to 1.0),
    ).build()

    /** Строго возрастающий набор полей (ORDERED). */
    fun ascending(vararg fields: String): Validator = aValidator().constraint(
        "ORDERED",
        mapOf("fields" to fields.toList(), "direction" to "ASCENDING"),
    ).build()

    /** RANGE без min/max — конфигурационная ошибка. */
    fun rangeWithoutBounds(field: String = "P"): Validator = aValidator().range(field).build()

    /** CONSTRAINT без rules["type"] — конфигурационная ошибка. */
    fun constraintWithoutType(): Validator = aValidator().constraintWithoutType(mapOf("fields" to listOf("a", "b"))).build()

    /** CONSTRAINT с незарегистрированным типом правила. */
    fun constraintOfUnknownType(): Validator = aValidator().constraint("NO_SUCH_RULE").build()
}

object ModelTemplateMother {
    /** Логистический рост с типовыми валидаторами: P > 0, r > 0, K > 0. */
    fun logistic(id: UUID = UUID.randomUUID()): ModelTemplate = aModelTemplate()
        .withId(id)
        .withCode("LOGISTIC")
        .named("Логистический рост")
        .withValidators(ValidatorMother.positive("P"), ValidatorMother.positive("r"), ValidatorMother.positive("K"))
        .build()

    /** Логистическая модель без валидаторов. */
    fun logisticWithoutValidators(id: UUID = UUID.randomUUID()): ModelTemplate =
        aModelTemplate().withId(id).withCode("LOGISTIC").build()

    /** SIR с проверкой сохранения численности S + I + R = N. */
    fun sir(id: UUID = UUID.randomUUID()): ModelTemplate = aModelTemplate()
        .withId(id)
        .withCode("SIR")
        .named("SIR-эпидемия")
        .withValidators(ValidatorMother.positive("N"), ValidatorMother.sirPopulationConservation())
        .build()

    /** Жёсткая модель Hodgkin-Huxley с подсказками решателя (V7). */
    fun hodgkinHuxley(id: UUID = UUID.randomUUID()): ModelTemplate = aModelTemplate()
        .withId(id)
        .withCode("HODGKIN_HUXLEY")
        .named("Hodgkin-Huxley")
        .withSolverHints(maxStep = 0.01, relTol = 1e-6, absTol = 1e-9)
        .build()

    /** Шаблон с кодом, для которого нет ОДУ-модели в реестре. */
    fun unknownModel(): ModelTemplate = aModelTemplate().withCode("NO_SUCH_MODEL").build()
}

object ParameterSetMother {
    /** P0 = 10, r = 0.5, K = 100 на отрезке [0; 10] с шагом 1 — 11 точек. */
    fun logistic(): ParameterSet = aParameterSet()
        .withValues("P" to 10.0, "r" to 0.5, "K" to 100.0)
        .withTimeRange(0.0, 10.0)
        .withStep(1.0)
        .build()

    /** Классический пример SIR: N = 1000, один заражённый. */
    fun sir(): ParameterSet = aParameterSet()
        .withValues("S" to 999.0, "I" to 1.0, "R" to 0.0, "beta" to 0.3, "gamma" to 0.1, "N" to 1000.0)
        .withTimeRange(0.0, 20.0)
        .withStep(1.0)
        .build()
}

object ExperimentMother {
    fun pendingLogistic(owner: UUID, modelId: UUID): Experiment =
        anExperiment().ownedBy(owner).forModel(modelId).pending().withParameterSet(ParameterSetMother.logistic()).build()

    fun running(owner: UUID = UUID.randomUUID()): Experiment = anExperiment().ownedBy(owner).running().build()

    fun completed(owner: UUID = UUID.randomUUID()): Experiment =
        anExperiment().ownedBy(owner).completed(aSimulationResult().build()).build()

    fun failed(owner: UUID = UUID.randomUUID()): Experiment = anExperiment().ownedBy(owner).failed().build()

    fun privateOf(owner: UUID, modelId: UUID = UUID.randomUUID()): Experiment =
        anExperiment().ownedBy(owner).forModel(modelId).asPrivate().build()

    fun publicOf(owner: UUID, modelId: UUID = UUID.randomUUID()): Experiment =
        anExperiment().ownedBy(owner).forModel(modelId).asPublic().completed().build()

    /** Повреждённая запись: эксперимент без набора параметров. */
    fun withoutParameters(owner: UUID = UUID.randomUUID()): Experiment =
        anExperiment().ownedBy(owner).pending().withoutParameterSet().build()
}

object CreateExperimentRequestMother {
    /** Корректный запрос для логистической модели (проходит валидаторы ModelTemplateMother.logistic). */
    fun validLogistic(modelId: UUID): CreateExperimentRequest = aCreateExperimentRequest()
        .named("Логистический рост: базовый сценарий")
        .forModel(modelId)
        .withParameters("P" to 10.0, "r" to 0.5, "K" to 100.0)
        .withTimeRange(0.0, 10.0)
        .withStep(1.0)
        .build()

    /** Отрицательная начальная популяция — нарушает валидатор P > 0. */
    fun negativePopulation(modelId: UUID): CreateExperimentRequest = aCreateExperimentRequest()
        .forModel(modelId)
        .withParameters("P" to -5.0, "r" to 0.5, "K" to 100.0)
        .build()
}
