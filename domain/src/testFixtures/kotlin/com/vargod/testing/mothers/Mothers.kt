package com.vargod.testing.mothers

import com.vargod.domain.Experiment
import com.vargod.domain.ModelTemplate
import com.vargod.domain.ParameterSet
import com.vargod.domain.Validator
import com.vargod.domain.dto.CreateExperimentRequest
import com.vargod.testing.builders.aCreateExperimentRequest
import com.vargod.testing.builders.aModelTemplate
import com.vargod.testing.builders.aParameterSet
import com.vargod.testing.builders.aSimulationResult
import com.vargod.testing.builders.aValidator
import com.vargod.testing.builders.anExperiment
import java.util.UUID

/*
 * Object Mother (фабрика тестовых объектов): именованные, узнаваемые в предметной
 * области экземпляры — «публичный эксперимент», «шаблон логистической модели».
 * Тест говорит, КАКОЙ объект ему нужен, а не КАК его собрать. Внутри матери
 * используют Data Builder'ы.
 */

object ValidatorMother {
    /** Строго положительное значение поля (например, популяция P > 0). */
    fun positive(field: String): Validator = aValidator().range(field, min = 0.0, minExclusive = true).build()
}

object ModelTemplateMother {
    /** Логистический рост с типовыми валидаторами: P > 0, r > 0, K > 0. */
    fun logistic(id: UUID = UUID.randomUUID()): ModelTemplate = aModelTemplate()
        .withId(id)
        .withCode("LOGISTIC")
        .named("Логистический рост")
        .withValidators(ValidatorMother.positive("P"), ValidatorMother.positive("r"), ValidatorMother.positive("K"))
        .build()
}

object ParameterSetMother {
    /** P0 = 10, r = 0.5, K = 100 на отрезке [0; 10] с шагом 1. */
    fun logistic(): ParameterSet = aParameterSet()
        .withValues("P" to 10.0, "r" to 0.5, "K" to 100.0)
        .withTimeRange(0.0, 10.0)
        .withStep(1.0)
        .build()
}

object ExperimentMother {
    fun pendingLogistic(owner: UUID, modelId: UUID): Experiment =
        anExperiment().ownedBy(owner).forModel(modelId).pending().withParameterSet(ParameterSetMother.logistic()).build()

    fun completed(owner: UUID = UUID.randomUUID()): Experiment =
        anExperiment().ownedBy(owner).completed(aSimulationResult().build()).build()

    fun privateOf(owner: UUID): Experiment = anExperiment().ownedBy(owner).asPrivate().build()

    fun publicOf(owner: UUID): Experiment = anExperiment().ownedBy(owner).asPublic().completed().build()
}

object CreateExperimentRequestMother {
    /** Корректный запрос для логистической модели. */
    fun validLogistic(modelId: UUID): CreateExperimentRequest = aCreateExperimentRequest()
        .named("Логистический рост: базовый сценарий")
        .forModel(modelId)
        .withParameters("P" to 10.0, "r" to 0.5, "K" to 100.0)
        .build()

    /** Отрицательная начальная популяция — нарушает валидатор P > 0. */
    fun negativePopulation(modelId: UUID): CreateExperimentRequest = aCreateExperimentRequest()
        .forModel(modelId)
        .withParameters("P" to -5.0, "r" to 0.5, "K" to 100.0)
        .build()
}
