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
@DisplayName("LogisticGrowthModel")
class LogisticGrowthModelTest : OdeModelContractTest() {

    override fun createModel(): OdeModel = LogisticGrowthModel()
    override val expectedCode = "LOGISTIC"
    override val expectedVariables = listOf("P")
    override val expectedParameters = listOf("r", "K")
    override fun validParams() = mapOf("r" to 0.5, "K" to 100.0)
    override fun validState() = doubleArrayOf(10.0)

    @Test
    @BoundaryValues
    fun `derivatives - population exactly at carrying capacity does not grow`() {
        // Arrange
        val atCapacity = doubleArrayOf(100.0)

        // Act
        val d = sut.derivatives(atCapacity, validParams(), 0.0)

        // Assert
        assertThat(d[0]).isEqualTo(0.0)
    }

    @Test
    @BoundaryValues
    fun `derivatives - zero carrying capacity is rejected`() {
        // Arrange
        val params = validParams() + ("K" to 0.0)

        // Act
        val error = assertThrows<IllegalArgumentException> { sut.derivatives(validState(), params, 0.0) }

        // Assert
        assertThat(error.message).contains("must be positive")
    }
}
