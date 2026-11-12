package com.vargod.engine.ode

import com.vargod.engine.OdeModel
import com.vargod.testing.BoundaryValues
import com.vargod.testing.Classical
import com.vargod.testing.EquivalencePartitioning
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

@Classical
@DisplayName("LotkaVolterraModel")
class LotkaVolterraModelTest : OdeModelContractTest() {

    override fun createModel(): OdeModel = LotkaVolterraModel()
    override val expectedCode = "LOTKA_VOLTERRA"
    override val expectedVariables = listOf("x", "y")
    override val expectedParameters = listOf("alpha", "beta", "gamma", "delta")
    override fun validParams() = mapOf("alpha" to 1.0, "beta" to 0.1, "gamma" to 1.5, "delta" to 0.075)
    override fun validState() = doubleArrayOf(10.0, 5.0)

    @Test
    @EquivalencePartitioning
    fun `derivatives - non-trivial equilibrium x = gamma over delta, y = alpha over beta is stationary`() {
        // Arrange: x* = 1.5 / 0.075 = 20, y* = 1.0 / 0.1 = 10
        val equilibrium = doubleArrayOf(20.0, 10.0)

        // Act
        val d = sut.derivatives(equilibrium, validParams(), 0.0)

        // Assert
        assertThat(d).containsExactly(doubleArrayOf(0.0, 0.0), within(1e-12))
    }
}
