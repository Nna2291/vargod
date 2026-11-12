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
@DisplayName("SirModel")
class SirModelTest : OdeModelContractTest() {

    override fun createModel(): OdeModel = SirModel()
    override val expectedCode = "SIR"
    override val expectedVariables = listOf("S", "I", "R")
    override val expectedParameters = listOf("beta", "gamma", "N")
    override fun validParams() = mapOf("beta" to 0.3, "gamma" to 0.1, "N" to 1000.0)
    override fun validState() = doubleArrayOf(990.0, 10.0, 0.0)

    @Test
    @EquivalencePartitioning
    fun `derivatives - known point matches SIR equations and conserves population`() {
        // Arrange
        val state = doubleArrayOf(990.0, 10.0, 0.0)

        // Act
        val d = sut.derivatives(state, validParams(), 0.0)

        // Assert: dS = -0.3*990*10/1000 = -2.97; dI = 2.97 - 0.1*10 = 1.97; dR = 1.0
        assertThat(d).containsExactly(doubleArrayOf(-2.97, 1.97, 1.0), within(1e-9))
        assertThat(d.sum()).isCloseTo(0.0, within(1e-9))
    }

    @Test
    @BoundaryValues
    fun `derivatives - zero population N is rejected`() {
        // Arrange
        val params = validParams() + ("N" to 0.0)

        // Act
        val error = assertThrows<IllegalArgumentException> { sut.derivatives(validState(), params, 0.0) }

        // Assert
        assertThat(error.message).isEqualTo("N must be positive")
    }
}
