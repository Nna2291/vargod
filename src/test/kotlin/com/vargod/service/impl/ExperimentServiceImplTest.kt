package com.vargod.service.impl

import com.vargod.domain.AuthorizationException
import com.vargod.domain.Experiment
import com.vargod.domain.ExperimentStatus
import com.vargod.domain.IllegalExperimentStateException
import com.vargod.domain.NotFoundException
import com.vargod.domain.SimulationResult
import com.vargod.domain.ValidationException
import com.vargod.domain.ValidationResult
import com.vargod.domain.ValidationStatus
import com.vargod.domain.dto.CreateExperimentRequest
import com.vargod.engine.SimulationEngine
import com.vargod.engine.ValidationEngine
import com.vargod.repository.ExperimentRepository
import com.vargod.repository.ModelTemplateRepository
import com.vargod.testfixtures.Fixtures
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import io.mockk.verifyOrder
import io.mockk.verifySequence
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID

class ExperimentServiceImplTest {

    private val experimentRepo = mockk<ExperimentRepository>()
    private val modelRepo = mockk<ModelTemplateRepository>()
    private val simulationEngine = mockk<SimulationEngine>()
    private val validationEngine = mockk<ValidationEngine>()
    private val service = ExperimentServiceImpl(experimentRepo, modelRepo, simulationEngine, validationEngine)

    private fun pass(v: java.util.UUID) = ValidationResult(ValidationStatus.PASS, null, v)
    private fun fail(v: java.util.UUID, msg: String) = ValidationResult(ValidationStatus.FAIL, msg, v)

    @Test
    fun `create saves new pending experiment when validation passes`() {
        val userId = UUID.randomUUID()
        val model = Fixtures.model(validators = emptyList())
        every { modelRepo.findById(model.id) } returns model
        every { validationEngine.validate(any(), emptyList()) } returns emptyList()

        val saved = slot<Experiment>()
        every { experimentRepo.save(capture(saved)) } answers { saved.captured }

        val req = CreateExperimentRequest(
            name = "my exp",
            modelTemplateId = model.id,
            parameters = mapOf("P" to 10.0),
            tStart = 0.0,
            tEnd = 1.0,
            step = 0.1,
        )
        val result = service.create(req, userId)
        assertEquals(ExperimentStatus.PENDING, result.status)
        assertEquals(userId, result.userId)
        assertEquals(model.id, result.modelTemplateId)
        assertEquals(mapOf("P" to 10.0), result.parameterSet!!.values)
        assertEquals(false, result.isPublic)
        verifySequence {
            modelRepo.findById(model.id)
            validationEngine.validate(mapOf("P" to 10.0), emptyList())
            experimentRepo.save(any())
        }
    }

    @Test
    fun `create throws NotFoundException when model is missing`() {
        val req = CreateExperimentRequest(null, UUID.randomUUID(), mapOf("P" to 1.0), 0.0, 1.0, 0.1)
        every { modelRepo.findById(req.modelTemplateId) } returns null
        assertThrows(NotFoundException::class.java) { service.create(req, UUID.randomUUID()) }
        verify(exactly = 0) { validationEngine.validate(any(), any()) }
        verify(exactly = 0) { experimentRepo.save(any()) }
    }

    @Test
    fun `create rethrows ValidationException on FAIL results and does not save`() {
        val model = Fixtures.model()
        val req = CreateExperimentRequest(null, model.id, mapOf("P" to -1.0), 0.0, 1.0, 0.1)
        every { modelRepo.findById(model.id) } returns model
        val validatorId = UUID.randomUUID()
        every { validationEngine.validate(any(), any()) } returns listOf(fail(validatorId, "P < 0"))
        val ex = assertThrows(ValidationException::class.java) { service.create(req, UUID.randomUUID()) }
        assertTrue(ex.results.first().status == ValidationStatus.FAIL)
        verify(exactly = 0) { experimentRepo.save(any()) }
    }

    @Test
    fun `create rejects invalid time range`() {
        val req = CreateExperimentRequest(null, UUID.randomUUID(), mapOf("P" to 1.0), 1.0, 1.0, 0.1)
        assertThrows(IllegalArgumentException::class.java) { service.create(req, UUID.randomUUID()) }
    }

    @Test
    fun `create rejects non-positive step`() {
        val req = CreateExperimentRequest(null, UUID.randomUUID(), mapOf("P" to 1.0), 0.0, 1.0, 0.0)
        assertThrows(IllegalArgumentException::class.java) { service.create(req, UUID.randomUUID()) }
    }

    @Test
    fun `getById returns when present`() {
        val exp = Fixtures.experiment()
        every { experimentRepo.findById(exp.id) } returns exp
        assertSame(exp, service.getById(exp.id))
    }

    @Test
    fun `getById throws when absent`() {
        val id = UUID.randomUUID()
        every { experimentRepo.findById(id) } returns null
        assertThrows(NotFoundException::class.java) { service.getById(id) }
    }

    @Test
    fun `listByUser delegates to repository`() {
        val userId = UUID.randomUUID()
        every { experimentRepo.findAllByUserId(userId) } returns emptyList()
        assertEquals(emptyList<Experiment>(), service.listByUser(userId))
    }

    @Test
    fun `listPublic delegates to repository`() {
        every { experimentRepo.findAllPublic() } returns emptyList()
        assertEquals(emptyList<Experiment>(), service.listPublic())
    }

    @Test
    fun `runSimulation flows PENDING - RUNNING - COMPLETED and saves result`() {
        val exp = Fixtures.experiment(status = ExperimentStatus.PENDING)
        val model = Fixtures.model(id = exp.modelTemplateId)
        val simResult = SimulationResult(
            id = UUID.randomUUID(),
            timePoints = listOf(0.0, 1.0),
            variables = mapOf("P" to listOf(10.0, 12.0)),
            createdAt = Instant.parse("2026-05-13T11:00:00Z"),
        )
        every { experimentRepo.findById(exp.id) } returns exp
        every { modelRepo.findById(exp.modelTemplateId) } returns model
        every { simulationEngine.solve(model, exp.parameterSet!!) } returns simResult

        val savedStates = mutableListOf<ExperimentStatus>()
        every { experimentRepo.save(any()) } answers {
            val arg = firstArg<Experiment>()
            savedStates.add(arg.status)
            arg
        }

        val result = service.runSimulation(exp.id)
        assertSame(simResult, result)
        assertEquals(listOf(ExperimentStatus.RUNNING, ExperimentStatus.COMPLETED), savedStates)
        verifyOrder {
            experimentRepo.save(match { it.status == ExperimentStatus.RUNNING })
            simulationEngine.solve(model, exp.parameterSet!!)
            experimentRepo.save(match { it.status == ExperimentStatus.COMPLETED && it.result == simResult })
        }
    }

    @Test
    fun `runSimulation marks experiment FAILED when engine throws`() {
        val exp = Fixtures.experiment(status = ExperimentStatus.PENDING)
        val model = Fixtures.model(id = exp.modelTemplateId)
        every { experimentRepo.findById(exp.id) } returns exp
        every { modelRepo.findById(exp.modelTemplateId) } returns model
        every { simulationEngine.solve(any(), any()) } throws RuntimeException("boom")

        val savedStates = mutableListOf<ExperimentStatus>()
        every { experimentRepo.save(any()) } answers {
            val arg = firstArg<Experiment>()
            savedStates.add(arg.status)
            arg
        }

        assertThrows(RuntimeException::class.java) { service.runSimulation(exp.id) }
        assertEquals(listOf(ExperimentStatus.RUNNING, ExperimentStatus.FAILED), savedStates)
    }

    @Test
    fun `runSimulation refuses non-PENDING experiment`() {
        val exp = Fixtures.experiment(status = ExperimentStatus.COMPLETED)
        every { experimentRepo.findById(exp.id) } returns exp
        assertThrows(IllegalExperimentStateException::class.java) { service.runSimulation(exp.id) }
        verify(exactly = 0) { simulationEngine.solve(any(), any()) }
        verify(exactly = 0) { experimentRepo.save(any()) }
    }

    @Test
    fun `runSimulation refuses experiment without parameter set`() {
        val exp = Fixtures.experiment(parameterSet = null)
        every { experimentRepo.findById(exp.id) } returns exp
        assertThrows(IllegalExperimentStateException::class.java) { service.runSimulation(exp.id) }
    }

    @Test
    fun `runSimulation throws NotFoundException when model is missing`() {
        val exp = Fixtures.experiment()
        every { experimentRepo.findById(exp.id) } returns exp
        every { modelRepo.findById(exp.modelTemplateId) } returns null
        assertThrows(NotFoundException::class.java) { service.runSimulation(exp.id) }
    }

    @Test
    fun `publish sets isPublic true for the owner`() {
        val owner = UUID.randomUUID()
        val exp = Fixtures.experiment(userId = owner, isPublic = false)
        every { experimentRepo.findById(exp.id) } returns exp
        val saved = slot<Experiment>()
        every { experimentRepo.save(capture(saved)) } answers { saved.captured }
        val result = service.publish(exp.id, owner)
        assertTrue(result.isPublic)
        assertTrue(saved.captured.isPublic)
    }

    @Test
    fun `publish is idempotent when already public`() {
        val owner = UUID.randomUUID()
        val exp = Fixtures.experiment(userId = owner, isPublic = true)
        every { experimentRepo.findById(exp.id) } returns exp
        val result = service.publish(exp.id, owner)
        assertSame(exp, result)
        verify(exactly = 0) { experimentRepo.save(any()) }
    }

    @Test
    fun `publish throws AuthorizationException for non-owner`() {
        val owner = UUID.randomUUID()
        val intruder = UUID.randomUUID()
        val exp = Fixtures.experiment(userId = owner)
        every { experimentRepo.findById(exp.id) } returns exp
        assertThrows(AuthorizationException::class.java) { service.publish(exp.id, intruder) }
        verify(exactly = 0) { experimentRepo.save(any()) }
    }
}
