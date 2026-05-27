package com.vargod.engine.ode

import com.vargod.domain.ParameterSet
import com.vargod.testfixtures.Fixtures
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.UUID
import kotlin.math.abs

class SeirdModelTest {

    private val model = SeirdModel()

    @Test
    fun `code returns SEIRD`() {
        assertEquals("SEIRD", model.code())
    }

    @Test
    fun `variables and parameters are stable`() {
        assertEquals(listOf("S", "E", "I", "R", "D"), model.variables())
        assertEquals(listOf("beta", "sigma", "gamma", "mu", "N"), model.parameters())
    }

    @Test
    fun `derivatives conserve population`() {
        val state = doubleArrayOf(900.0, 50.0, 40.0, 10.0, 0.0)
        val params = mapOf(
            "beta" to 0.4, "sigma" to 0.2, "gamma" to 0.1, "mu" to 0.01, "N" to 1000.0
        )
        val d = model.derivatives(state, params, 0.0)
        // dS+dE+dI+dR+dD = 0 (mass conservation)
        assertTrue(abs(d.sum()) < 1e-9) { "expected zero sum, got ${d.sum()}" }
    }

    @Test
    fun `simulation preserves total population over time`() {
        val registry = MapModelRegistry(listOf(SeirdModel()))
        val engine = OdeSimulationEngine(registry)
        val totalN = 1000.0
        val params = ParameterSet(
            id = UUID.randomUUID(),
            values = mapOf(
                "S" to 990.0, "E" to 5.0, "I" to 5.0, "R" to 0.0, "D" to 0.0,
                "beta" to 0.4, "sigma" to 0.2, "gamma" to 0.1, "mu" to 0.01, "N" to totalN,
            ),
            tStart = 0.0,
            tEnd = 20.0,
            step = 1.0,
        )
        val result = engine.solve(Fixtures.model(code = "SEIRD"), params)
        val s = result.variables.getValue("S")
        val e = result.variables.getValue("E")
        val i = result.variables.getValue("I")
        val r = result.variables.getValue("R")
        val dDead = result.variables.getValue("D")
        for (idx in s.indices) {
            val sum = s[idx] + e[idx] + i[idx] + r[idx] + dDead[idx]
            assertTrue(abs(sum - totalN) < 1e-3) {
                "population drift at idx=$idx: sum=$sum, expected=$totalN"
            }
        }
    }

    @Test
    fun `wrong state size throws`() {
        assertThrows(IllegalArgumentException::class.java) {
            model.derivatives(
                doubleArrayOf(1.0, 1.0),
                mapOf("beta" to 0.1, "sigma" to 0.1, "gamma" to 0.1, "mu" to 0.0, "N" to 1.0),
                0.0,
            )
        }
    }

    @Test
    fun `missing parameter throws`() {
        assertThrows(IllegalArgumentException::class.java) {
            model.derivatives(doubleArrayOf(1.0, 1.0, 1.0, 1.0, 1.0), mapOf("beta" to 0.1), 0.0)
        }
    }

    @Test
    fun `zero N is rejected`() {
        assertThrows(IllegalArgumentException::class.java) {
            model.derivatives(
                doubleArrayOf(1.0, 0.0, 0.0, 0.0, 0.0),
                mapOf("beta" to 0.1, "sigma" to 0.1, "gamma" to 0.1, "mu" to 0.0, "N" to 0.0),
                0.0,
            )
        }
    }
}
