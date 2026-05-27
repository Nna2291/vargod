package com.vargod.engine.ode

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.math.abs

class RosenzweigMacArthurModelTest {

    private val model = RosenzweigMacArthurModel()

    private val defaultParams = mapOf(
        "r" to 1.0,
        "K" to 100.0,
        "a" to 0.5,
        "b" to 0.1,
        "eps" to 0.3,
        "m" to 0.2,
    )

    @Test
    fun `code returns ROSENZWEIG_MACARTHUR`() {
        assertEquals("ROSENZWEIG_MACARTHUR", model.code())
    }

    @Test
    fun `variables and parameters are stable`() {
        assertEquals(listOf("x", "y"), model.variables())
        assertEquals(listOf("r", "K", "a", "b", "eps", "m"), model.parameters())
    }

    @Test
    fun `trivial fixed point at origin yields zero derivatives`() {
        val d = model.derivatives(doubleArrayOf(0.0, 0.0), defaultParams, 0.0)
        assertEquals(0.0, d[0], 1e-12)
        assertEquals(0.0, d[1], 1e-12)
    }

    @Test
    fun `functional response saturates as prey grows large`() {
        // f(x) = a·x / (1 + a·b·x); lim x -> ∞ = 1/b. With b=0.1, that's 10.
        // Check that consumption term y·f(x) approaches y/b.
        val y = 1.0
        val params = defaultParams + ("eps" to 0.0) + ("m" to 0.0) + ("r" to 0.0)
        val largeX = 1e9
        val d = model.derivatives(doubleArrayOf(largeX, y), params, 0.0)
        // dx = r·x·(1 − x/K) − f(x)·y; with r=0, dx = -f(x)·y ≈ -y/b = -10.
        val saturation = 1.0 / (defaultParams.getValue("b"))
        assertTrue(abs(d[0] - (-y * saturation)) / saturation < 1e-6) {
            "expected dx ≈ ${-y * saturation}, got ${d[0]}"
        }
    }

    @Test
    fun `non-trivial state gives expected derivatives`() {
        val state = doubleArrayOf(20.0, 5.0)
        val d = model.derivatives(state, defaultParams, 0.0)
        // f(20) = 0.5*20 / (1 + 0.5*0.1*20) = 10 / 2 = 5
        // dx = 1*20*(1 − 20/100) − 5*5 = 20*0.8 − 25 = 16 − 25 = -9
        // dy = 0.3*5*5 − 0.2*5 = 7.5 − 1.0 = 6.5
        assertEquals(-9.0, d[0], 1e-9)
        assertEquals(6.5, d[1], 1e-9)
    }

    @Test
    fun `non-positive K is rejected`() {
        assertThrows(IllegalArgumentException::class.java) {
            model.derivatives(
                doubleArrayOf(1.0, 1.0),
                defaultParams + ("K" to 0.0),
                0.0,
            )
        }
    }

    @Test
    fun `wrong state size throws`() {
        assertThrows(IllegalArgumentException::class.java) {
            model.derivatives(doubleArrayOf(1.0), defaultParams, 0.0)
        }
    }

    @Test
    fun `missing parameter throws`() {
        assertThrows(IllegalArgumentException::class.java) {
            model.derivatives(doubleArrayOf(1.0, 1.0), mapOf("r" to 1.0), 0.0)
        }
    }
}
