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
@DisplayName("SeirdModel")
class SeirdModelTest : OdeModelContractTest() {

    override fun createModel(): OdeModel = SeirdModel()
    override val expectedCode = "SEIRD"
    override val expectedVariables = listOf("S", "E", "I", "R", "D")
    override val expectedParameters = listOf("beta", "sigma", "gamma", "mu", "N")
    override fun validParams() = mapOf("beta" to 0.4, "sigma" to 0.2, "gamma" to 0.1, "mu" to 0.01, "N" to 1000.0)
    override fun validState() = doubleArrayOf(900.0, 50.0, 40.0, 10.0, 0.0)

    @Test
    @EquivalencePartitioning
    fun `derivatives - known point matches SEIRD equations and conserves population`() {
        // Arrange: новые заражения = 0.4 * 900 * 40 / 1000 = 14.4
        val state = validState()

        // Act
        val d = sut.derivatives(state, validParams(), 0.0)

        // Assert
        assertThat(d).containsExactly(doubleArrayOf(-14.4, 4.4, 5.6, 4.0, 0.4), within(1e-9))
        assertThat(d.sum()).isCloseTo(0.0, within(1e-9))
    }
}
