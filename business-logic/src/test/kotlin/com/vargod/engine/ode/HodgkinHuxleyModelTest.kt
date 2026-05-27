package com.vargod.engine.ode

import com.vargod.domain.ModelTemplate
import com.vargod.domain.ParameterSet
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.UUID
import kotlin.math.abs

class HodgkinHuxleyModelTest {

    private val model = HodgkinHuxleyModel()

    private val defaultParams = mapOf(
        "gNa" to 120.0,
        "gK" to 36.0,
        "gL" to 0.3,
        "ENa" to 50.0,
        "EK" to (-77.0),
        "EL" to (-54.387),
        "Cm" to 1.0,
        "Iext" to 0.0,
    )

    @Test
    fun `code returns HODGKIN_HUXLEY`() {
        assertEquals("HODGKIN_HUXLEY", model.code())
    }

    @Test
    fun `variables and parameters are stable`() {
        assertEquals(listOf("V", "n", "m", "h"), model.variables())
        assertEquals(
            listOf("gNa", "gK", "gL", "ENa", "EK", "EL", "Cm", "Iext"),
            model.parameters(),
        )
    }

    @Test
    fun `near-resting state with no current produces small derivatives`() {
        // V_rest ≈ -65 mV, n ≈ 0.318, m ≈ 0.053, h ≈ 0.596 — стационарные ворота для V=-65.
        val state = doubleArrayOf(-65.0, 0.318, 0.053, 0.596)
        val d = model.derivatives(state, defaultParams, 0.0)
        // Derivatives should be small near rest.
        assertTrue(abs(d[0]) < 1.0) { "dV/dt too large at rest: ${d[0]}" }
        assertTrue(abs(d[1]) < 1e-2) { "dn/dt too large at rest: ${d[1]}" }
        assertTrue(abs(d[2]) < 1e-2) { "dm/dt too large at rest: ${d[2]}" }
        assertTrue(abs(d[3]) < 1e-2) { "dh/dt too large at rest: ${d[3]}" }
    }

    @Test
    fun `removable singularity at V=-55 returns finite alpha_n`() {
        // At V=-55, alpha_n denominator is exactly 0; the model must use the limit (0.1).
        val state = doubleArrayOf(-55.0, 0.5, 0.5, 0.5)
        val d = model.derivatives(state, defaultParams, 0.0)
        assertTrue(d.all { it.isFinite() }) { "non-finite derivative at V=-55: ${d.toList()}" }
    }

    @Test
    fun `removable singularity at V=-40 returns finite alpha_m`() {
        val state = doubleArrayOf(-40.0, 0.5, 0.5, 0.5)
        val d = model.derivatives(state, defaultParams, 0.0)
        assertTrue(d.all { it.isFinite() }) { "non-finite derivative at V=-40: ${d.toList()}" }
    }

    @Test
    fun `wrong state size throws`() {
        assertThrows(IllegalArgumentException::class.java) {
            model.derivatives(doubleArrayOf(0.0, 0.0), defaultParams, 0.0)
        }
    }

    @Test
    fun `missing parameter throws`() {
        assertThrows(IllegalArgumentException::class.java) {
            model.derivatives(doubleArrayOf(-65.0, 0.3, 0.05, 0.6), mapOf("gNa" to 120.0), 0.0)
        }
    }

    @Test
    fun `injection current of 10 produces an action potential within 50 ms`() {
        val registry = MapModelRegistry(listOf(HodgkinHuxleyModel()))
        // Solver tuned for HH stiffness — те же значения, что попадут в БД как hints.
        val engine = OdeSimulationEngine(
            registry,
            absoluteTolerance = 1e-9,
            relativeTolerance = 1e-6,
            maxStep = 0.01,
        )
        val params = ParameterSet(
            id = UUID.randomUUID(),
            values = defaultParams + mapOf(
                "Iext" to 10.0,
                "V" to (-65.0),
                "n" to 0.318,
                "m" to 0.053,
                "h" to 0.596,
            ),
            tStart = 0.0,
            tEnd = 50.0,
            step = 0.05,
        )
        val hhTemplate = ModelTemplate(
            id = UUID.randomUUID(),
            code = "HODGKIN_HUXLEY",
            name = "HH",
            description = null,
            solverType = "RK45",
            validators = emptyList(),
        )
        val result = engine.solve(hhTemplate, params)
        val vSeries = result.variables.getValue("V")
        assertTrue(vSeries.any { it > 0.0 }) {
            "expected at least one action potential (V > 0 mV), max=${vSeries.max()}"
        }
    }
}
