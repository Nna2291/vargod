package com.vargod.engine.ode

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class LotkaVolterraModelTest {

    private val model = LotkaVolterraModel()

    @Test
    fun `code returns LOTKA_VOLTERRA`() {
        assertEquals("LOTKA_VOLTERRA", model.code())
    }

    @Test
    fun `variables and parameters are stable`() {
        assertEquals(listOf("x", "y"), model.variables())
        assertEquals(listOf("alpha", "beta", "gamma", "delta"), model.parameters())
    }

    @Test
    fun `derivatives compute predator-prey equations`() {
        val state = doubleArrayOf(40.0, 9.0)
        val params = mapOf("alpha" to 0.1, "beta" to 0.02, "gamma" to 0.3, "delta" to 0.01)
        val d = model.derivatives(state, params, 0.0)
        // dx/dt = 0.1*40 - 0.02*40*9 = 4 - 7.2 = -3.2
        // dy/dt = -0.3*9 + 0.01*40*9 = -2.7 + 3.6 = 0.9
        assertEquals(-3.2, d[0], 1e-9)
        assertEquals(0.9, d[1], 1e-9)
    }

    @Test
    fun `wrong state size throws`() {
        assertThrows(IllegalArgumentException::class.java) {
            model.derivatives(
                doubleArrayOf(1.0, 1.0, 1.0),
                mapOf("alpha" to 1.0, "beta" to 1.0, "gamma" to 1.0, "delta" to 1.0),
                0.0,
            )
        }
    }

    @Test
    fun `missing parameter throws`() {
        assertThrows(IllegalArgumentException::class.java) {
            model.derivatives(doubleArrayOf(1.0, 1.0), mapOf("alpha" to 1.0, "beta" to 1.0), 0.0)
        }
    }
}
