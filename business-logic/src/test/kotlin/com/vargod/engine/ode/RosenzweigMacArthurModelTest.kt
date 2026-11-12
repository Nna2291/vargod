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
@DisplayName("RosenzweigMacArthurModel")
class RosenzweigMacArthurModelTest : OdeModelContractTest() {

    override fun createModel(): OdeModel = RosenzweigMacArthurModel()
    override val expectedCode = "ROSENZWEIG_MACARTHUR"
    override val expectedVariables = listOf("x", "y")
    override val expectedParameters = listOf("r", "K", "a", "b", "eps", "m")
    override fun validParams() = mapOf("r" to 1.0, "K" to 100.0, "a" to 0.5, "b" to 0.1, "eps" to 0.3, "m" to 0.2)
    override fun validState() = doubleArrayOf(20.0, 5.0)

    @Test
    @EquivalencePartitioning
    fun `derivatives - known point matches equations with type II functional response`() {
        // Arrange: f(20) = 0.5*20 / (1 + 0.5*0.1*20) = 5
        val state = validState()

        // Act
        val d = sut.derivatives(state, validParams(), 0.0)

        // Assert: dx = 20*(1 - 0.2) - 5*5 = -9; dy = 0.3*5*5 - 0.2*5 = 6.5
        assertThat(d).containsExactly(doubleArrayOf(-9.0, 6.5), within(1e-9))
    }
}
