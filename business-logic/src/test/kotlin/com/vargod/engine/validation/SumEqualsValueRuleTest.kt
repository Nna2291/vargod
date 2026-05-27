package com.vargod.engine.validation

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SumEqualsValueRuleTest {

    private val rule = SumEqualsValueRule()

    @Test
    fun `type is SUM_EQUALS_VALUE`() {
        assertEquals("SUM_EQUALS_VALUE", rule.type())
    }

    @Test
    fun `passes when sum equals expected within default tolerance`() {
        val values = mapOf("S" to 700.0, "I" to 299.9999999999, "R" to 0.0000000001)
        val params: Map<String, Any> = mapOf(
            "fields" to listOf("S", "I", "R"),
            "expected" to 1000.0,
        )
        assertTrue(rule.check(values, params))
    }

    @Test
    fun `fails when sum diverges from expected beyond tolerance`() {
        val values = mapOf("S" to 800.0, "I" to 100.0, "R" to 50.0)
        val params: Map<String, Any> = mapOf(
            "fields" to listOf("S", "I", "R"),
            "expected" to 1000.0,
        )
        assertFalse(rule.check(values, params))
    }

    @Test
    fun `respects custom tolerance`() {
        val values = mapOf("a" to 1.0, "b" to 1.0)
        val params: Map<String, Any> = mapOf(
            "fields" to listOf("a", "b"),
            "expected" to 2.1,
            "tolerance" to 0.2,
        )
        assertTrue(rule.check(values, params))
    }

    @Test
    fun `throws when fields parameter is missing`() {
        assertThrows(IllegalArgumentException::class.java) {
            rule.check(mapOf("a" to 1.0), mapOf("expected" to 1.0))
        }
    }

    @Test
    fun `throws when expected is missing or not numeric`() {
        assertThrows(IllegalArgumentException::class.java) {
            rule.check(mapOf("a" to 1.0), mapOf("fields" to listOf("a")))
        }
    }

    @Test
    fun `throws when a referenced field is missing or non-numeric`() {
        val params: Map<String, Any> = mapOf("fields" to listOf("a", "b"), "expected" to 2.0)
        assertThrows(IllegalArgumentException::class.java) {
            rule.check(mapOf("a" to 1.0), params)
        }
        assertThrows(IllegalArgumentException::class.java) {
            rule.check(mapOf("a" to "oops", "b" to 1.0), params)
        }
    }
}
