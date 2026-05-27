package com.vargod.repository.jdbc

import com.vargod.domain.ExperimentStatus
import com.vargod.domain.SimulationResult
import com.vargod.testfixtures.Fixtures
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID

class JdbcExperimentRepositoryIT : JdbcRepositoryTestBase() {

    @Test
    fun `save inserts new experiment and findById returns equivalent entity`() {
        val model = Fixtures.model()
        seedModelTemplate(model)

        val parameterSet = Fixtures.parameterSet(values = mapOf("alpha" to 1.0, "beta" to 0.5))
        val experiment = Fixtures.experiment(modelTemplateId = model.id, parameterSet = parameterSet)

        val saved = experimentRepository.save(experiment)
        assertEquals(experiment, saved)

        val loaded = experimentRepository.findById(experiment.id)
        assertNotNull(loaded)
        assertEquals(experiment.id, loaded!!.id)
        assertEquals(experiment.name, loaded.name)
        assertEquals(ExperimentStatus.PENDING, loaded.status)
        assertEquals(experiment.userId, loaded.userId)
        assertEquals(experiment.modelTemplateId, loaded.modelTemplateId)
        assertEquals(experiment.createdAt, loaded.createdAt)
        assertEquals(parameterSet.id, loaded.parameterSet!!.id)
        assertEquals(parameterSet.values, loaded.parameterSet!!.values)
        assertEquals(parameterSet.tStart, loaded.parameterSet!!.tStart)
        assertEquals(parameterSet.tEnd, loaded.parameterSet!!.tEnd)
        assertEquals(parameterSet.step, loaded.parameterSet!!.step)
        assertNull(loaded.result)
    }

    @Test
    fun `save twice updates status and persists simulation result without duplicating children`() {
        val model = Fixtures.model()
        seedModelTemplate(model)
        val parameterSet = Fixtures.parameterSet()
        val initial = Fixtures.experiment(modelTemplateId = model.id, parameterSet = parameterSet)
        experimentRepository.save(initial)

        val simResult = SimulationResult(
            id = UUID.randomUUID(),
            timePoints = listOf(0.0, 1.0, 2.0),
            variables = mapOf("S" to listOf(1.0, 0.8, 0.6), "I" to listOf(0.0, 0.2, 0.4)),
            createdAt = Instant.parse("2026-05-14T09:00:00Z"),
        )
        val completed = initial.copy(status = ExperimentStatus.COMPLETED, result = simResult)
        experimentRepository.save(completed)

        val loaded = experimentRepository.findById(initial.id)
        assertNotNull(loaded)
        assertEquals(ExperimentStatus.COMPLETED, loaded!!.status)
        assertEquals(simResult.timePoints, loaded.result!!.timePoints)
        assertEquals(simResult.variables, loaded.result!!.variables)
        assertEquals(simResult.createdAt, loaded.result!!.createdAt)
        assertEquals(1, rowCount("parameter_sets"))
        assertEquals(1, rowCount("simulation_results"))
        assertEquals(1, rowCount("experiments"))
    }

    @Test
    fun `deleteById removes experiment and cascades parameter set and result`() {
        val model = Fixtures.model()
        seedModelTemplate(model)
        val simResult = SimulationResult(
            id = UUID.randomUUID(),
            timePoints = listOf(0.0, 1.0),
            variables = mapOf("P" to listOf(10.0, 12.0)),
            createdAt = Instant.parse("2026-05-14T10:00:00Z"),
        )
        val experiment = Fixtures.experiment(
            modelTemplateId = model.id,
            parameterSet = Fixtures.parameterSet(),
            result = simResult,
        )
        experimentRepository.save(experiment)
        assertEquals(1, rowCount("experiments"))
        assertEquals(1, rowCount("parameter_sets"))
        assertEquals(1, rowCount("simulation_results"))

        experimentRepository.deleteById(experiment.id)

        assertNull(experimentRepository.findById(experiment.id))
        assertEquals(0, rowCount("experiments"))
        assertEquals(0, rowCount("parameter_sets"))
        assertEquals(0, rowCount("simulation_results"))
    }
}
