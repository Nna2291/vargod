package com.vargod.engine.ode

import com.vargod.engine.OdeModel
import com.vargod.testing.BoundaryValues
import com.vargod.testing.Classical
import com.vargod.testing.EquivalencePartitioning
import com.vargod.testing.builders.aParameterSet
import com.vargod.testing.mothers.ModelTemplateMother
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@Classical
@DisplayName("HodgkinHuxleyModel")
class HodgkinHuxleyModelTest : OdeModelContractTest() {

    override fun createModel(): OdeModel = HodgkinHuxleyModel()
    override val expectedCode = "HODGKIN_HUXLEY"
    override val expectedVariables = listOf("V", "n", "m", "h")
    override val expectedParameters = listOf("gNa", "gK", "gL", "ENa", "EK", "EL", "Cm", "Iext")
    override fun validParams() = mapOf(
        "gNa" to 120.0, "gK" to 36.0, "gL" to 0.3,
        "ENa" to 50.0, "EK" to -77.0, "EL" to -54.387,
        "Cm" to 1.0, "Iext" to 0.0,
    )
    /** Состояние покоя: V ≈ -65 мВ и стационарные значения воротных переменных. */
    override fun validState() = doubleArrayOf(-65.0, 0.318, 0.053, 0.596)

    /**
     * alpha_n(V) = 0.01(V+55) / (1 - e^{-(V+55)/10}) — при V = -55 знаменатель равен 0
     * (устранимая особенность). Значение в самой точке должно совпасть с пределом
     * и с соседней точкой V = -55 + 1e-4.
     */
    @Test
    @BoundaryValues
    fun `derivatives - removable singularity at V = -55 is continuous`() {
        // Arrange
        val atSingularity = doubleArrayOf(-55.0, 0.5, 0.5, 0.5)
        val nearby = sut.derivatives(doubleArrayOf(-55.0 + 1e-4, 0.5, 0.5, 0.5), validParams(), 0.0)

        // Act
        val d = sut.derivatives(atSingularity, validParams(), 0.0)

        // Assert
        assertThat(d.toList()).allMatch { it.isFinite() }
        assertThat(d[1]).isCloseTo(nearby[1], org.assertj.core.api.Assertions.within(1e-5))
    }

    @Test
    @EquivalencePartitioning
    fun `solve - injected current of 10 produces an action potential within 50 ms`() {
        // Arrange
        val engine = OdeSimulationEngine(MapModelRegistry(listOf(HodgkinHuxleyModel())))
        val params = aParameterSet()
            .withValues(validParams() + mapOf("Iext" to 10.0, "V" to -65.0, "n" to 0.318, "m" to 0.053, "h" to 0.596))
            .withTimeRange(0.0, 50.0)
            .withStep(0.05)
            .build()

        // Act
        val result = engine.solve(ModelTemplateMother.hodgkinHuxley(), params)

        // Assert: пик потенциала действия выше 0 мВ
        assertThat(result.variables.getValue("V").max()).isGreaterThan(0.0)
    }
}
