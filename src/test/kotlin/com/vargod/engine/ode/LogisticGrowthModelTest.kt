package com.vargod.engine.ode

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class LogisticGrowthModelTest {

    private val model = LogisticGrowthModel()

    @Test
    fun `code returns LOGISTIC`() {
        assertEquals("LOGISTIC", model.code())
    }

    @Test
    fun `variables and parameters are stable`() {
        assertEquals(listOf("P"), model.variables())
        assertEquals(listOf("r", "K"), model.parameters())
    }

    @Test
    fun `derivative follows logistic equation`() {
        val state = doubleArrayOf(50.0)
        val params = mapOf("r" to 0.4, "K" to 100.0)
        val d = model.derivatives(state, params, 0.0)
        // dP/dt = 0.4 * 50 * (1 - 50/100) = 0.4 * 50 * 0.5 = 10.0
        assertEquals(10.0, d[0], 1e-9)
    }

    @Test
    fun `derivative at carrying capacity is zero`() {
        val d = model.derivatives(doubleArrayOf(100.0), mapOf("r" to 0.4, "K" to 100.0), 0.0)
        assertEquals(0.0, d[0], 1e-12)
    }

    @Test
    fun `wrong state size throws`() {
        assertThrows(IllegalArgumentException::class.java) {
            model.derivatives(doubleArrayOf(1.0, 1.0), mapOf("r" to 1.0, "K" to 1.0), 0.0)
        }
    }

    @Test
    fun `non-positive K throws`() {
        assertThrows(IllegalArgumentException::class.java) {
            model.derivatives(doubleArrayOf(1.0), mapOf("r" to 1.0, "K" to 0.0), 0.0)
        }
    }

    @Test
    fun `missing parameter throws`() {
        assertThrows(IllegalArgumentException::class.java) {
            model.derivatives(doubleArrayOf(1.0), mapOf("r" to 1.0), 0.0)
        }
    }
}
