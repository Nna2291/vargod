package com.vargod.service.impl

import com.vargod.domain.AuthorizationException
import com.vargod.domain.ExperimentStatus
import com.vargod.domain.IllegalExperimentStateException
import com.vargod.domain.NotFoundException
import com.vargod.domain.ValidationException
import com.vargod.domain.ValidationStatus
import com.vargod.support.RealEngines
import com.vargod.testing.BoundaryValues
import com.vargod.testing.Classical
import com.vargod.testing.EquivalencePartitioning
import com.vargod.testing.ErrorGuessing
import com.vargod.testing.StateTransition
import com.vargod.testing.TestTime
import com.vargod.testing.builders.aCreateExperimentRequest
import com.vargod.testing.builders.anExperiment
import com.vargod.testing.inmemory.InMemoryExperimentRepository
import com.vargod.testing.inmemory.InMemoryModelTemplateRepository
import com.vargod.testing.mothers.CreateExperimentRequestMother
import com.vargod.testing.mothers.ExperimentMother
import com.vargod.testing.mothers.ModelTemplateMother
import io.qameta.allure.Epic
import io.qameta.allure.Feature
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.UUID
import kotlin.math.exp

@Classical
@Epic("Business logic")
@Feature("ExperimentService")
@DisplayName("ExperimentServiceImpl — classical style (real engines + in-memory repositories)")
class ExperimentServiceImplClassicalTest {

    private lateinit var experiments: InMemoryExperimentRepository
    private lateinit var models: InMemoryModelTemplateRepository
    private lateinit var sut: ExperimentServiceImpl

    private val logistic = ModelTemplateMother.logistic()
    private val owner = UUID.randomUUID()
    private val stranger = UUID.randomUUID()

    @BeforeEach
    fun setUp() {
        experiments = InMemoryExperimentRepository()
        models = InMemoryModelTemplateRepository(logistic)
        sut = ExperimentServiceImpl(
            experimentRepository = experiments,
            modelRepository = models,
            simulationEngine = RealEngines.simulationEngine(),
            validationEngine = RealEngines.validationEngine(),
            clock = TestTime.FIXED_CLOCK,
        )
    }

    // ---------------------------------------------------------------- create

    @Test
    @EquivalencePartitioning
    fun `create - valid parameters produce stored PENDING experiment with validator snapshot`() {
        // Arrange
        val request = CreateExperimentRequestMother.validLogistic(logistic.id)

        // Act
        val created = sut.create(request, owner)

        // Assert
        val stored = experiments.findById(created.id)
        assertThat(stored).isEqualTo(created)
        assertThat(stored!!.status).isEqualTo(ExperimentStatus.PENDING)
        assertThat(stored.createdAt).isEqualTo(TestTime.NOW)
        assertThat(stored.validatorSnapshot).isEqualTo(logistic.validators)
        assertThat(stored.parameterSet!!.values).isEqualTo(request.parameters)
    }

    @Test
    @BoundaryValues
    fun `create - population exactly at exclusive lower bound 0 is rejected`() {
        // Arrange
        val request = aCreateExperimentRequest().forModel(logistic.id).withParameters("P" to 0.0, "r" to 0.5, "K" to 100.0).build()

        // Act
        val error = assertThrows<ValidationException> { sut.create(request, owner) }

        // Assert
        assertThat(error.message).contains("exclusive=true")
        assertThat(experiments.count()).isZero()
    }

    @Test
    @BoundaryValues
    fun `create - smallest positive population just above the bound is accepted`() {
        // Arrange
        val request = aCreateExperimentRequest().forModel(logistic.id)
            .withParameters("P" to Double.MIN_VALUE, "r" to 0.5, "K" to 100.0).build()

        // Act
        val created = sut.create(request, owner)

        // Assert
        assertThat(experiments.findById(created.id)).isNotNull
    }

    // ---------------------------------------------------------------- getById / lists

    // ---------------------------------------------------------------- runSimulation

    @Test
    @StateTransition
    fun `runSimulation - PENDING logistic experiment becomes COMPLETED with numeric result`() {
        // Arrange
        val experiment = experiments.save(ExperimentMother.pendingLogistic(owner, logistic.id))

        // Act
        val result = sut.runSimulation(experiment.id)

        // Assert
        val stored = experiments.findById(experiment.id)!!
        assertThat(stored.status).isEqualTo(ExperimentStatus.COMPLETED)
        assertThat(stored.result).isEqualTo(result)
        assertThat(result.timePoints).hasSize(11)
        // Аналитическое решение: P(t) = K / (1 + (K/P0 - 1) e^{-r t}); P(10) = 100 / (1 + 9 e^{-5}) ≈ 94.2826
        val analytical = 100.0 / (1.0 + 9.0 * exp(-0.5 * 10.0))
        assertThat(result.variables.getValue("P").last()).isCloseTo(analytical, within(1e-6))
    }

    @Test
    @StateTransition
    fun `runSimulation - COMPLETED experiment cannot be run again and keeps its result`() {
        // Arrange
        val experiment = experiments.save(ExperimentMother.completed(owner).copy(modelTemplateId = logistic.id))

        // Act
        val error = assertThrows<IllegalExperimentStateException> { sut.runSimulation(experiment.id) }

        // Assert
        assertThat(error.message).contains("COMPLETED")
        assertThat(experiments.findById(experiment.id)).isEqualTo(experiment)
    }

    @Test
    @StateTransition
    fun `runSimulation - model without ODE implementation moves experiment to FAILED`() {
        // Arrange
        val unknown = ModelTemplateMother.unknownModel()
        models.save(unknown)
        val experiment = experiments.save(anExperiment().ownedBy(owner).forModel(unknown.id).pending().build())

        // Act
        val error = assertThrows<IllegalArgumentException> { sut.runSimulation(experiment.id) }

        // Assert
        assertThat(error.message).contains("Unknown ODE model code")
        assertThat(experiments.findById(experiment.id)!!.status).isEqualTo(ExperimentStatus.FAILED)
    }

    // ---------------------------------------------------------------- publish / unpublish

    @Test
    @StateTransition
    fun `publish - private experiment of owner becomes public in storage`() {
        // Arrange
        val experiment = experiments.save(ExperimentMother.privateOf(owner))

        // Act
        sut.publish(experiment.id, owner)

        // Assert
        assertThat(experiments.findById(experiment.id)!!.isPublic).isTrue()
        assertThat(sut.listPublic()).extracting<UUID> { it.id }.containsExactly(experiment.id)
    }

    @Test
    @EquivalencePartitioning
    fun `publish - stranger cannot publish and experiment stays private`() {
        // Arrange
        val experiment = experiments.save(ExperimentMother.privateOf(owner))

        // Act
        assertThrows<AuthorizationException> { sut.publish(experiment.id, stranger) }

        // Assert
        assertThat(experiments.findById(experiment.id)!!.isPublic).isFalse()
    }

}
