package com.vargod.engine.validation

import com.vargod.engine.ConstraintRule
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class MapConstraintRuleRegistryTest {

    @Test
    fun `getByType returns registered rule`() {
        val sv = SumEqualsValueRule()
        val sf = SumEqualsFieldRule()
        val ord = OrderedRule()
        val reg = MapConstraintRuleRegistry(listOf(sv, sf, ord))
        assertSame(sv, reg.getByType("SUM_EQUALS_VALUE"))
        assertSame(sf, reg.getByType("SUM_EQUALS_FIELD"))
        assertSame(ord, reg.getByType("ORDERED"))
    }

    @Test
    fun `listAll returns all rules keyed by type`() {
        val reg = MapConstraintRuleRegistry(listOf(SumEqualsValueRule(), OrderedRule()))
        val all = reg.listAll()
        assertEquals(2, all.size)
        assertEquals("SUM_EQUALS_VALUE", all["SUM_EQUALS_VALUE"]!!.type())
    }

    @Test
    fun `getByType throws on unknown`() {
        val reg = MapConstraintRuleRegistry(listOf(SumEqualsValueRule()))
        assertThrows(IllegalArgumentException::class.java) {
            reg.getByType("MYSTERY")
        }
    }

    @Test
    fun `duplicate types are rejected`() {
        val a: ConstraintRule = object : ConstraintRule {
            override fun type() = "X"
            override fun paramsSchema() = RuleParamsSchema(emptyList())
            override fun check(values: Map<String, Any>, params: Map<String, Any>) = true
        }
        val b: ConstraintRule = object : ConstraintRule {
            override fun type() = "X"
            override fun paramsSchema() = RuleParamsSchema(emptyList())
            override fun check(values: Map<String, Any>, params: Map<String, Any>) = false
        }
        assertThrows(IllegalArgumentException::class.java) {
            MapConstraintRuleRegistry(listOf(a, b))
        }
    }

    @Test
    fun `blank type is rejected`() {
        val rule = object : ConstraintRule {
            override fun type() = " "
            override fun paramsSchema() = RuleParamsSchema(emptyList())
            override fun check(values: Map<String, Any>, params: Map<String, Any>) = true
        }
        assertThrows(IllegalArgumentException::class.java) {
            MapConstraintRuleRegistry(listOf(rule))
        }
    }
}
