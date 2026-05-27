package com.vargod.engine.ode

import com.vargod.domain.ParameterSet
import com.vargod.engine.ModelRegistry
import com.vargod.engine.OdeModel
import com.vargod.testfixtures.Fixtures
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.UUID
import kotlin.math.abs
import kotlin.math.exp

class OdeSimulationEngineTest {

    @Test
    fun `solve produces time series for logistic growth matching analytical solution`() {
        val registry = MapModelRegistry(listOf(LogisticGrowthModel()))
        val engine = OdeSimulationEngine(registry)
        val model = Fixtures.model(code = "LOGISTIC")
        val p0 = 10.0
        val r = 0.4
        val k = 100.0
        val params = ParameterSet(
            id = UUID.randomUUID(),
            values = mapOf("P" to p0, "r" to r, "K" to k),
            tStart = 0.0,
            tEnd = 10.0,
            step = 1.0,
        )
        val result = engine.solve(model, params)

        // Analytical: P(t) = K / (1 + (K/P0 - 1) * exp(-r*t))
        val analytical: (Double) -> Double = { t -> k / (1.0 + (k / p0 - 1.0) * exp(-r * t)) }
        assertTrue(result.timePoints.size >= 10) { "expected at least 10 sample points, got ${result.timePoints.size}" }
        val pSeries = result.variables.getValue("P")
        result.timePoints.forEachIndexed { i, t ->
            val expected = analytical(t)
            val actual = pSeries[i]
            assertTrue(abs(actual - expected) / expected < 1e-3) {
                "deviation at t=$t too large: expected=$expected, actual=$actual"
            }
        }
    }

    @Test
    fun `solve calls registry with model code and uses returned OdeModel`() {
        val registry = mockk<ModelRegistry>()
        val fakeOde = object : OdeModel {
            override fun code() = "CONST"
            override fun variables() = listOf("x")
            override fun parameters() = emptyList<String>()
            override fun derivatives(state: DoubleArray, params: Map<String, Double>, t: Double) = doubleArrayOf(1.0)
        }
        every { registry.getByCode("CONST") } returns fakeOde
        val engine = OdeSimulationEngine(registry)
        val model = Fixtures.model(code = "CONST")
        val params = ParameterSet(
            id = UUID.randomUUID(),
            values = mapOf("x" to 0.0),
            tStart = 0.0,
            tEnd = 5.0,
            step = 1.0,
        )

        val result = engine.solve(model, params)

        verify(exactly = 1) { registry.getByCode("CONST") }
        val xSeries = result.variables.getValue("x")
        assertEquals(result.timePoints.size, xSeries.size)
        // dx/dt = 1, x(0)=0 -> x(t) = t. Verify last sample.
        val tLast = result.timePoints.last()
        val xLast = xSeries.last()
        assertTrue(abs(xLast - tLast) < 1e-6) { "expected x(t)=t, got x=$xLast at t=$tLast" }
    }

    @Test
    fun `solve rejects tEnd not after tStart`() {
        val engine = OdeSimulationEngine(MapModelRegistry(listOf(LogisticGrowthModel())))
        val params = ParameterSet(
            UUID.randomUUID(),
            mapOf("P" to 1.0, "r" to 1.0, "K" to 1.0),
            tStart = 1.0,
            tEnd = 1.0,
            step = 0.1,
        )
        assertThrows(IllegalArgumentException::class.java) {
            engine.solve(Fixtures.model(code = "LOGISTIC"), params)
        }
    }

    @Test
    fun `solve rejects non-positive step`() {
        val engine = OdeSimulationEngine(MapModelRegistry(listOf(LogisticGrowthModel())))
        val params = ParameterSet(
            UUID.randomUUID(),
            mapOf("P" to 1.0, "r" to 1.0, "K" to 1.0),
            tStart = 0.0,
            tEnd = 1.0,
            step = 0.0,
        )
        assertThrows(IllegalArgumentException::class.java) {
            engine.solve(Fixtures.model(code = "LOGISTIC"), params)
        }
    }

    @Test
    fun `solve rejects missing initial value for variable`() {
        val engine = OdeSimulationEngine(MapModelRegistry(listOf(LogisticGrowthModel())))
        val params = ParameterSet(
            UUID.randomUUID(),
            mapOf("r" to 0.1, "K" to 100.0), // missing P
            tStart = 0.0,
            tEnd = 1.0,
            step = 0.1,
        )
        assertThrows(IllegalArgumentException::class.java) {
            engine.solve(Fixtures.model(code = "LOGISTIC"), params)
        }
    }
}
