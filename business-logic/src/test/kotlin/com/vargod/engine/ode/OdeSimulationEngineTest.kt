package com.vargod.engine.ode

import com.vargod.engine.ModelRegistry
import com.vargod.engine.OdeModel
import com.vargod.support.RealEngines
import com.vargod.testing.BoundaryValues
import com.vargod.testing.Classical
import com.vargod.testing.EquivalencePartitioning
import com.vargod.testing.ErrorGuessing
import com.vargod.testing.London
import com.vargod.testing.builders.aModelTemplate
import com.vargod.testing.builders.aParameterSet
import com.vargod.testing.mothers.ModelTemplateMother
import com.vargod.testing.mothers.ParameterSetMother
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import io.qameta.allure.Epic
import io.qameta.allure.Feature
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import kotlin.math.exp

@Classical
@Epic("Business logic")
@Feature("Simulation engine")
@DisplayName("OdeSimulationEngine — classical style (real ODE models)")
class OdeSimulationEngineClassicalTest {

    private lateinit var sut: OdeSimulationEngine

    @BeforeEach
    fun setUp() {
        sut = RealEngines.simulationEngine()
    }

    @Test
    @EquivalencePartitioning
    fun `solve - logistic growth follows the analytical solution`() {
        // Arrange
        val params = ParameterSetMother.logistic() // P0 = 10, r = 0.5, K = 100, t ∈ [0; 10], шаг 1
        val analytical = { t: Double -> 100.0 / (1.0 + 9.0 * exp(-0.5 * t)) }

        // Act
        val result = sut.solve(ModelTemplateMother.logistic(), params)

        // Assert
        assertThat(result.timePoints).hasSize(11)
        result.timePoints.forEachIndexed { i, t ->
            assertThat(result.variables.getValue("P")[i]).isCloseTo(analytical(t), within(1e-6))
        }
    }

    @Test
    @BoundaryValues
    fun `solve - step equal to the whole horizon gives exactly two samples`() {
        // Arrange
        val params = aParameterSet().withTimeRange(0.0, 1.0).withStep(1.0).build()

        // Act
        val result = sut.solve(ModelTemplateMother.logistic(), params)

        // Assert
        assertThat(result.timePoints).containsExactly(0.0, 1.0)
    }

    @Test
    @BoundaryValues
    fun `solve - zero-length horizon is rejected`() {
        // Arrange
        val params = aParameterSet().withTimeRange(3.0, 3.0).build()

        // Act
        val error = assertThrows<IllegalArgumentException> { sut.solve(ModelTemplateMother.logistic(), params) }

        // Assert
        assertThat(error.message).startsWith("tEnd")
    }

    @Test
    @ErrorGuessing
    fun `solve - missing initial value of a state variable is reported`() {
        // Arrange
        val params = aParameterSet().withValues("r" to 0.5, "K" to 100.0).build()

        // Act
        val error = assertThrows<IllegalArgumentException> { sut.solve(ModelTemplateMother.logistic(), params) }

        // Assert
        assertThat(error.message).isEqualTo("Missing initial value for variable 'P'")
    }
}

/**
 * Лондонский вариант: реестр и ОДУ-модель — mock'и. Так проверяется то, что
 * классическим тестом не увидеть: подсказки решателя из шаблона действительно
 * доходят до интегратора (меньший maxStep → больше вычислений производных).
 */
@London
@Epic("Business logic")
@Feature("Simulation engine")
@DisplayName("OdeSimulationEngine — London style (MockK)")
@ExtendWith(MockKExtension::class)
class OdeSimulationEngineLondonTest {

    @MockK private lateinit var registry: ModelRegistry
    @MockK private lateinit var ode: OdeModel

    private lateinit var sut: OdeSimulationEngine

    @BeforeEach
    fun setUp() {
        sut = OdeSimulationEngine(registry, maxStep = 1.0)
        every { registry.getByCode("CONST") } returns ode
        every { ode.code() } returns "CONST"
        every { ode.variables() } returns listOf("x")
        every { ode.derivatives(any(), any(), any()) } returns doubleArrayOf(1.0)
    }

    @Test
    @EquivalencePartitioning
    fun `solve - recommended max step from template overrides engine default`() {
        // Arrange
        val template = aModelTemplate().withCode("CONST").withSolverHints(maxStep = 0.01, relTol = null, absTol = null).build()
        val params = aParameterSet().withValues("x" to 0.0).withTimeRange(0.0, 1.0).withStep(0.5).build()

        // Act
        val result = sut.solve(template, params)

        // Assert: dx/dt = 1 => x(1) = 1; при maxStep = 0.01 нужно не меньше 100 шагов
        assertThat(result.variables.getValue("x").last()).isCloseTo(1.0, within(1e-9))
        verify(atLeast = 100) { ode.derivatives(any(), any(), any()) }
    }

    @Test
    @ErrorGuessing
    fun `solve - model returning wrong number of derivatives is reported`() {
        // Arrange
        val template = aModelTemplate().withCode("CONST").build()
        val params = aParameterSet().withValues("x" to 0.0).build()
        every { ode.derivatives(any(), any(), any()) } returns doubleArrayOf(1.0, 2.0)

        // Act
        val error = assertThrows<IllegalArgumentException> { sut.solve(template, params) }

        // Assert
        assertThat(error.message).contains("produced 2 derivatives but expected 1")
    }
}
