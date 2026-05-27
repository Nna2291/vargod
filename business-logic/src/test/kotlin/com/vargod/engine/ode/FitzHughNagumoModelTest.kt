package com.vargod.engine.ode

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class FitzHughNagumoModelTest {

    private val model = FitzHughNagumoModel()

    @Test
    fun `code returns FITZHUGH_NAGUMO`() {
        assertEquals("FITZHUGH_NAGUMO", model.code())
    }

    @Test
    fun `variables and parameters are stable`() {
        assertEquals(listOf("V", "w"), model.variables())
        assertEquals(listOf("a", "b", "tau", "Iext"), model.parameters())
    }

    @Test
    fun `derivatives compute FHN equations deterministically`() {
        val state = doubleArrayOf(0.0, 0.0)
        val params = mapOf("a" to 0.7, "b" to 0.8, "tau" to 12.5, "Iext" to 0.5)
        val d = model.derivatives(state, params, 0.0)
        // dV = 0 - 0 - 0 + 0.5 = 0.5
        // dw = (0 + 0.7 - 0.8*0)/12.5 = 0.7/12.5 = 0.056
        assertEquals(0.5, d[0], 1e-12)
        assertEquals(0.7 / 12.5, d[1], 1e-12)
    }

    @Test
    fun `nontrivial state gives expected derivatives`() {
        val state = doubleArrayOf(1.5, 0.3)
        val params = mapOf("a" to 0.7, "b" to 0.8, "tau" to 12.5, "Iext" to 0.0)
        val d = model.derivatives(state, params, 0.0)
        // dV = 1.5 - 1.5^3/3 - 0.3 + 0 = 1.5 - 1.125 - 0.3 = 0.075
        // dw = (1.5 + 0.7 - 0.8*0.3)/12.5 = (1.5 + 0.7 - 0.24)/12.5 = 1.96/12.5 = 0.1568
        assertEquals(0.075, d[0], 1e-12)
        assertEquals(1.96 / 12.5, d[1], 1e-12)
    }

    @Test
    fun `non-positive tau is rejected`() {
        assertThrows(IllegalArgumentException::class.java) {
            model.derivatives(
                doubleArrayOf(0.0, 0.0),
                mapOf("a" to 0.7, "b" to 0.8, "tau" to 0.0, "Iext" to 0.0),
                0.0,
            )
        }
    }

    @Test
    fun `wrong state size throws`() {
        assertThrows(IllegalArgumentException::class.java) {
            model.derivatives(
                doubleArrayOf(0.0),
                mapOf("a" to 0.7, "b" to 0.8, "tau" to 1.0, "Iext" to 0.0),
                0.0,
            )
        }
    }

    @Test
    fun `missing parameter throws`() {
        assertThrows(IllegalArgumentException::class.java) {
            model.derivatives(doubleArrayOf(0.0, 0.0), mapOf("a" to 0.7), 0.0)
        }
    }
}
