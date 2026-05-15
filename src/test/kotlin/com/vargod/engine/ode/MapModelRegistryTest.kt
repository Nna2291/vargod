package com.vargod.engine.ode

import com.vargod.engine.OdeModel
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class MapModelRegistryTest {

    @Test
    fun `getByCode returns registered model`() {
        val sir = SirModel()
        val lv = LotkaVolterraModel()
        val registry = MapModelRegistry(listOf(sir, lv))
        assertSame(sir, registry.getByCode("SIR"))
        assertSame(lv, registry.getByCode("LOTKA_VOLTERRA"))
    }

    @Test
    fun `listAll returns all registered models keyed by code`() {
        val sir = SirModel()
        val lv = LotkaVolterraModel()
        val logistic = LogisticGrowthModel()
        val registry = MapModelRegistry(listOf(sir, lv, logistic))
        val all = registry.listAll()
        assertEquals(3, all.size)
        assertSame(sir, all["SIR"])
        assertSame(lv, all["LOTKA_VOLTERRA"])
        assertSame(logistic, all["LOGISTIC"])
    }

    @Test
    fun `getByCode throws on unknown code`() {
        val registry = MapModelRegistry(listOf(SirModel()))
        val ex = assertThrows(IllegalArgumentException::class.java) {
            registry.getByCode("MYSTERY")
        }
        assert(ex.message!!.contains("MYSTERY"))
    }

    @Test
    fun `blank code is rejected`() {
        val blankModel = object : OdeModel {
            override fun code() = " "
            override fun variables() = emptyList<String>()
            override fun parameters() = emptyList<String>()
            override fun derivatives(state: DoubleArray, params: Map<String, Double>, t: Double) = doubleArrayOf()
        }
        assertThrows(IllegalArgumentException::class.java) {
            MapModelRegistry(listOf(blankModel))
        }
    }

    @Test
    fun `duplicate codes are rejected`() {
        val a = object : OdeModel {
            override fun code() = "DUP"
            override fun variables() = emptyList<String>()
            override fun parameters() = emptyList<String>()
            override fun derivatives(state: DoubleArray, params: Map<String, Double>, t: Double) = doubleArrayOf()
        }
        val b = object : OdeModel {
            override fun code() = "DUP"
            override fun variables() = emptyList<String>()
            override fun parameters() = emptyList<String>()
            override fun derivatives(state: DoubleArray, params: Map<String, Double>, t: Double) = doubleArrayOf()
        }
        assertThrows(IllegalArgumentException::class.java) {
            MapModelRegistry(listOf(a, b))
        }
    }
}
