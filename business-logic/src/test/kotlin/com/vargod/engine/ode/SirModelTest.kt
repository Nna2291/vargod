package com.vargod.engine.ode

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class SirModelTest {

    private val model = SirModel()

    @Test
    fun `code returns SIR`() {
        assertEquals("SIR", model.code())
    }

    @Test
    fun `variables and parameters are stable`() {
        assertEquals(listOf("S", "I", "R"), model.variables())
        assertEquals(listOf("beta", "gamma", "N"), model.parameters())
    }

    @Test
    fun `derivatives compute SIR equations`() {
        val state = doubleArrayOf(990.0, 10.0, 0.0)
        val params = mapOf("beta" to 0.3, "gamma" to 0.1, "N" to 1000.0)
        val d = model.derivatives(state, params, 0.0)
        // dS/dt = -0.3 * 990 * 10 / 1000 = -2.97
        // dI/dt = -dS/dt - 0.1*10 = 2.97 - 1.0 = 1.97
        // dR/dt = 0.1 * 10 = 1.0
        assertEquals(-2.97, d[0], 1e-9)
        assertEquals(1.97, d[1], 1e-9)
        assertEquals(1.0, d[2], 1e-9)
        // mass conservation: dS+dI+dR should be 0
        assertEquals(0.0, d.sum(), 1e-9)
    }

    @Test
    fun `wrong state size throws`() {
        val ex = assertThrows(IllegalArgumentException::class.java) {
            model.derivatives(doubleArrayOf(1.0), mapOf("beta" to 0.1, "gamma" to 0.1, "N" to 1.0), 0.0)
        }
        assert(ex.message!!.contains("SIR"))
    }

    @Test
    fun `missing parameter throws`() {
        assertThrows(IllegalArgumentException::class.java) {
            model.derivatives(doubleArrayOf(1.0, 1.0, 1.0), mapOf("beta" to 0.1, "gamma" to 0.1), 0.0)
        }
    }

    @Test
    fun `zero N is rejected`() {
        assertThrows(IllegalArgumentException::class.java) {
            model.derivatives(doubleArrayOf(1.0, 1.0, 1.0), mapOf("beta" to 0.1, "gamma" to 0.1, "N" to 0.0), 0.0)
        }
    }
}
