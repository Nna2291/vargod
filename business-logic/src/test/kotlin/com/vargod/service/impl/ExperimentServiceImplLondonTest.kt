package com.vargod.service.impl

import com.vargod.domain.AuthorizationException
import com.vargod.domain.Experiment
import com.vargod.domain.ExperimentStatus
import com.vargod.domain.IllegalExperimentStateException
import com.vargod.domain.NotFoundException
import com.vargod.domain.PersistenceException
import com.vargod.domain.ValidationException
import com.vargod.domain.ValidationResult
import com.vargod.domain.ValidationStatus
import com.vargod.domain.repository.ExperimentRepository
import com.vargod.domain.repository.ModelTemplateRepository
import com.vargod.engine.SimulationEngine
import com.vargod.engine.ValidationEngine
import com.vargod.testing.BoundaryValues
import com.vargod.testing.EquivalencePartitioning
import com.vargod.testing.ErrorGuessing
import com.vargod.testing.London
import com.vargod.testing.StateTransition
import com.vargod.testing.TestTime
import com.vargod.testing.builders.aCreateExperimentRequest
import com.vargod.testing.builders.aSimulationResult
import com.vargod.testing.builders.anExperiment
import com.vargod.testing.mothers.CreateExperimentRequestMother
import com.vargod.testing.mothers.ExperimentMother
import com.vargod.testing.mothers.ModelTemplateMother
import com.vargod.testing.mothers.ValidatorMother
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.slot
import io.mockk.verify
import io.mockk.verifyOrder
import io.qameta.allure.Epic
import io.qameta.allure.Feature
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import java.util.UUID

@London
@Epic("Business logic")
@Feature("ExperimentService")
@DisplayName("ExperimentServiceImpl — London style (MockK)")
@ExtendWith(MockKExtension::class)
class ExperimentServiceImplLondonTest {

    @MockK private lateinit var experimentRepository: ExperimentRepository
    @MockK private lateinit var modelRepository: ModelTemplateRepository
    @MockK private lateinit var simulationEngine: SimulationEngine
    @MockK private lateinit var validationEngine: ValidationEngine

    private lateinit var sut: ExperimentServiceImpl

    private val owner = UUID.randomUUID()
    private val stranger = UUID.randomUUID()

    @BeforeEach
    fun setUp() {
        sut = ExperimentServiceImpl(
            experimentRepository, modelRepository, simulationEngine, validationEngine, TestTime.FIXED_CLOCK,
        )
    }

    private fun passFor(validatorId: UUID) = ValidationResult(ValidationStatus.PASS, null, validatorId)
    private fun failFor(validatorId: UUID, msg: String) = ValidationResult(ValidationStatus.FAIL, msg, validatorId)

    private fun stubSaveReturnsArgument() {
        every { experimentRepository.save(any()) } answers { firstArg() }
    }

    // ---------------------------------------------------------------- create

    @Test
    @EquivalencePartitioning
    fun `create - valid request is validated against model validators and saved as PENDING`() {
        // Arrange
        val model = ModelTemplateMother.logistic()
        val request = CreateExperimentRequestMother.validLogistic(model.id)
        val saved = slot<Experiment>()
        every { modelRepository.findById(model.id) } returns model
        every { validationEngine.validate(request.parameters, model.validators) } returns model.validators.map { passFor(it.id) }
        every { experimentRepository.save(capture(saved)) } answers { firstArg() }

        // Act
        val result = sut.create(request, owner)

        // Assert
        assertThat(result).isEqualTo(saved.captured)
        assertThat(result.status).isEqualTo(ExperimentStatus.PENDING)
        assertThat(result.userId).isEqualTo(owner)
        assertThat(result.isPublic).isFalse()
        assertThat(result.createdAt).isEqualTo(TestTime.NOW)
        assertThat(result.validatorSnapshot).isEqualTo(model.validators)
        verifyOrder {
            modelRepository.findById(model.id)
            validationEngine.validate(request.parameters, model.validators)
            experimentRepository.save(any())
        }
    }

    @Test
    @EquivalencePartitioning
    fun `create - failed validation throws ValidationException and nothing is saved`() {
        // Arrange
        val model = ModelTemplateMother.logistic()
        val request = CreateExperimentRequestMother.negativePopulation(model.id)
        val failure = failFor(model.validators.first().id, "P must be > 0")
        every { modelRepository.findById(model.id) } returns model
        every { validationEngine.validate(any(), any()) } returns listOf(failure)

        // Act
        val error = assertThrows<ValidationException> { sut.create(request, owner) }

        // Assert
        assertThat(error.results).containsExactly(failure)
        assertThat(error.message).contains("P must be > 0")
        verify(exactly = 0) { experimentRepository.save(any()) }
    }

    @Test
    @EquivalencePartitioning
    fun `create - unknown model throws NotFoundException before validation`() {
        // Arrange
        val request = aCreateExperimentRequest().build()
        every { modelRepository.findById(request.modelTemplateId) } returns null

        // Act
        val error = assertThrows<NotFoundException> { sut.create(request, owner) }

        // Assert
        assertThat(error.message).contains(request.modelTemplateId.toString())
        verify(exactly = 0) { validationEngine.validate(any(), any()) }
        verify(exactly = 0) { experimentRepository.save(any()) }
    }

    @Test
    @BoundaryValues
    fun `create - zero step is rejected without touching repositories`() {
        // Arrange
        val request = aCreateExperimentRequest().withStep(0.0).build()

        // Act
        val error = assertThrows<IllegalArgumentException> { sut.create(request, owner) }

        // Assert
        assertThat(error.message).contains("step")
        verify(exactly = 0) { modelRepository.findById(any()) }
    }

    // ---------------------------------------------------------------- getById

    @Test
    @EquivalencePartitioning
    fun `getById - returns experiment found by repository`() {
        // Arrange
        val experiment = anExperiment().build()
        every { experimentRepository.findById(experiment.id) } returns experiment

        // Act
        val result = sut.getById(experiment.id)

        // Assert
        assertThat(result).isSameAs(experiment)
    }

    @Test
    @EquivalencePartitioning
    fun `getById - missing experiment throws NotFoundException`() {
        // Arrange
        val id = UUID.randomUUID()
        every { experimentRepository.findById(id) } returns null

        // Act
        val error = assertThrows<NotFoundException> { sut.getById(id) }

        // Assert
        assertThat(error.message).isEqualTo("Experiment not found: id=$id")
    }

    // ---------------------------------------------------------------- listByUser / listPublic

    @Test
    @EquivalencePartitioning
    fun `listByUser - delegates to repository with the same user id`() {
        // Arrange
        val experiments = listOf(anExperiment().ownedBy(owner).build(), anExperiment().ownedBy(owner).build())
        every { experimentRepository.findAllByUserId(owner) } returns experiments

        // Act
        val result = sut.listByUser(owner)

        // Assert
        assertThat(result).isEqualTo(experiments)
        verify(exactly = 1) { experimentRepository.findAllByUserId(owner) }
    }

    @Test
    @ErrorGuessing
    fun `listByUser - repository failure is propagated unchanged`() {
        // Arrange
        val failure = PersistenceException("connection refused")
        every { experimentRepository.findAllByUserId(owner) } throws failure

        // Act
        val error = assertThrows<PersistenceException> { sut.listByUser(owner) }

        // Assert
        assertThat(error).isSameAs(failure)
    }

    @Test
    @EquivalencePartitioning
    fun `listPublic - delegates to repository`() {
        // Arrange
        val published = listOf(ExperimentMother.publicOf(owner))
        every { experimentRepository.findAllPublic() } returns published

        // Act
        val result = sut.listPublic()

        // Assert
        assertThat(result).isEqualTo(published)
    }

    @Test
    @BoundaryValues
    fun `listPublic - no public experiments gives empty list`() {
        // Arrange
        every { experimentRepository.findAllPublic() } returns emptyList()

        // Act
        val result = sut.listPublic()

        // Assert
        assertThat(result).isEmpty()
    }

    // ---------------------------------------------------------------- runSimulation

    @Test
    @StateTransition
    fun `runSimulation - PENDING goes to RUNNING then COMPLETED with result`() {
        // Arrange
        val model = ModelTemplateMother.logistic()
        val experiment = ExperimentMother.pendingLogistic(owner, model.id)
        val simulation = aSimulationResult().build()
        every { experimentRepository.findById(experiment.id) } returns experiment
        every { modelRepository.findById(model.id) } returns model
        every { simulationEngine.solve(model, experiment.parameterSet!!) } returns simulation
        stubSaveReturnsArgument()

        // Act
        val result = sut.runSimulation(experiment.id)

        // Assert
        assertThat(result).isSameAs(simulation)
        verifyOrder {
            experimentRepository.save(match { it.status == ExperimentStatus.RUNNING && it.result == null })
            simulationEngine.solve(model, experiment.parameterSet!!)
            experimentRepository.save(match { it.status == ExperimentStatus.COMPLETED && it.result == simulation })
        }
    }

    @Test
    @StateTransition
    fun `runSimulation - engine failure moves experiment to FAILED and rethrows`() {
        // Arrange
        val model = ModelTemplateMother.logistic()
        val experiment = ExperimentMother.pendingLogistic(owner, model.id)
        val failure = IllegalStateException("integrator diverged")
        every { experimentRepository.findById(experiment.id) } returns experiment
        every { modelRepository.findById(model.id) } returns model
        every { simulationEngine.solve(any(), any()) } throws failure
        stubSaveReturnsArgument()

        // Act
        val error = assertThrows<IllegalStateException> { sut.runSimulation(experiment.id) }

        // Assert
        assertThat(error).isSameAs(failure)
        verifyOrder {
            experimentRepository.save(match { it.status == ExperimentStatus.RUNNING })
            experimentRepository.save(match { it.status == ExperimentStatus.FAILED && it.result == null })
        }
        verify(exactly = 0) { experimentRepository.save(match { it.status == ExperimentStatus.COMPLETED }) }
    }

    @ParameterizedTest(name = "from {0}")
    @EnumSource(value = ExperimentStatus::class, names = ["RUNNING", "COMPLETED", "FAILED"])
    @StateTransition
    fun `runSimulation - only PENDING may be started, other states are rejected`(status: ExperimentStatus) {
        // Arrange
        val experiment = anExperiment().withStatus(status).build()
        every { experimentRepository.findById(experiment.id) } returns experiment

        // Act
        val error = assertThrows<IllegalExperimentStateException> { sut.runSimulation(experiment.id) }

        // Assert
        assertThat(error.message).contains(status.name)
        verify(exactly = 0) { simulationEngine.solve(any(), any()) }
        verify(exactly = 0) { experimentRepository.save(any()) }
    }

    @Test
    @ErrorGuessing
    fun `runSimulation - experiment without parameter set is rejected`() {
        // Arrange
        val experiment = ExperimentMother.withoutParameters(owner)
        every { experimentRepository.findById(experiment.id) } returns experiment

        // Act
        val error = assertThrows<IllegalExperimentStateException> { sut.runSimulation(experiment.id) }

        // Assert
        assertThat(error.message).contains("no parameter set")
        verify(exactly = 0) { experimentRepository.save(any()) }
    }

    @Test
    @ErrorGuessing
    fun `runSimulation - deleted model template gives NotFoundException and status stays PENDING`() {
        // Arrange
        val experiment = anExperiment().pending().build()
        every { experimentRepository.findById(experiment.id) } returns experiment
        every { modelRepository.findById(experiment.modelTemplateId) } returns null

        // Act
        val error = assertThrows<NotFoundException> { sut.runSimulation(experiment.id) }

        // Assert
        assertThat(error.message).contains("ModelTemplate")
        verify(exactly = 0) { experimentRepository.save(any()) }
    }

    // ---------------------------------------------------------------- publish

    @Test
    @StateTransition
    fun `publish - owner makes private experiment public`() {
        // Arrange
        val experiment = ExperimentMother.privateOf(owner)
        every { experimentRepository.findById(experiment.id) } returns experiment
        stubSaveReturnsArgument()

        // Act
        val result = sut.publish(experiment.id, owner)

        // Assert
        assertThat(result.isPublic).isTrue()
        verify(exactly = 1) { experimentRepository.save(experiment.copy(isPublic = true)) }
    }

    @Test
    @StateTransition
    fun `publish - already public experiment is returned as is without saving`() {
        // Arrange
        val experiment = ExperimentMother.publicOf(owner)
        every { experimentRepository.findById(experiment.id) } returns experiment

        // Act
        val result = sut.publish(experiment.id, owner)

        // Assert
        assertThat(result).isSameAs(experiment)
        verify(exactly = 0) { experimentRepository.save(any()) }
    }

    @Test
    @EquivalencePartitioning
    fun `publish - non-owner gets AuthorizationException`() {
        // Arrange
        val experiment = ExperimentMother.privateOf(owner)
        every { experimentRepository.findById(experiment.id) } returns experiment

        // Act
        val error = assertThrows<AuthorizationException> { sut.publish(experiment.id, stranger) }

        // Assert
        assertThat(error.message).contains(stranger.toString())
        verify(exactly = 0) { experimentRepository.save(any()) }
    }

    // ---------------------------------------------------------------- unpublish

    @Test
    @StateTransition
    fun `unpublish - owner makes public experiment private`() {
        // Arrange
        val experiment = ExperimentMother.publicOf(owner)
        every { experimentRepository.findById(experiment.id) } returns experiment
        stubSaveReturnsArgument()

        // Act
        val result = sut.unpublish(experiment.id, owner)

        // Assert
        assertThat(result.isPublic).isFalse()
        verify(exactly = 1) { experimentRepository.save(experiment.copy(isPublic = false)) }
    }

    @Test
    @EquivalencePartitioning
    fun `unpublish - non-owner gets AuthorizationException`() {
        // Arrange
        val experiment = ExperimentMother.publicOf(owner)
        every { experimentRepository.findById(experiment.id) } returns experiment

        // Act
        val error = assertThrows<AuthorizationException> { sut.unpublish(experiment.id, stranger) }

        // Assert
        assertThat(error.message).contains("not the owner")
        verify(exactly = 0) { experimentRepository.save(any()) }
    }

}
