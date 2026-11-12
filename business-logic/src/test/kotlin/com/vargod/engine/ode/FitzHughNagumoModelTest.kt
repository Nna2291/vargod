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
@DisplayName("FitzHughNagumoModel")
class FitzHughNagumoModelTest : OdeModelContractTest() {

    override fun createModel(): OdeModel = FitzHughNagumoModel()
    override val expectedCode = "FITZHUGH_NAGUMO"
    override val expectedVariables = listOf("V", "w")
    override val expectedParameters = listOf("a", "b", "tau", "Iext")
    override fun validParams() = mapOf("a" to 0.7, "b" to 0.8, "tau" to 12.5, "Iext" to 0.5)
    override fun validState() = doubleArrayOf(0.0, 0.0)

    @Test
    @EquivalencePartitioning
    fun `derivatives - known point matches FitzHugh-Nagumo equations`() {
        // Arrange
        val state = doubleArrayOf(1.5, 0.3)
        val params = validParams() + ("Iext" to 0.0)

        // Act
        val d = sut.derivatives(state, params, 0.0)

        // Assert: dV = 1.5 - 1.5^3/3 - 0.3 = 0.075; dw = (1.5 + 0.7 - 0.8*0.3) / 12.5 = 0.1568
        assertThat(d).containsExactly(doubleArrayOf(0.075, 0.1568), within(1e-12))
    }

    @Test
    @BoundaryValues
    fun `derivatives - zero time constant tau is rejected`() {
        // Arrange
        val params = validParams() + ("tau" to 0.0)

        // Act
        val error = assertThrows<IllegalArgumentException> { sut.derivatives(validState(), params, 0.0) }

        // Assert
        assertThat(error.message).isEqualTo("tau must be positive")
    }
}
