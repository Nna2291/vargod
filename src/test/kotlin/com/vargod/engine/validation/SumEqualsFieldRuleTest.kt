package com.vargod.engine.validation

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SumEqualsFieldRuleTest {

    private val rule = SumEqualsFieldRule()

    @Test
    fun `type is SUM_EQUALS_FIELD`() {
        assertEquals("SUM_EQUALS_FIELD", rule.type())
    }

    @Test
    fun `passes when sum of fields equals expectedField`() {
        val values = mapOf("S" to 500.0, "I" to 300.0, "R" to 200.0, "N" to 1000.0)
        val params: Map<String, Any> = mapOf(
            "fields" to listOf("S", "I", "R"),
            "expectedField" to "N",
        )
        assertTrue(rule.check(values, params))
    }

    @Test
    fun `fails when sum differs from expectedField`() {
        val values = mapOf("S" to 500.0, "I" to 300.0, "R" to 100.0, "N" to 1000.0)
        val params: Map<String, Any> = mapOf(
            "fields" to listOf("S", "I", "R"),
            "expectedField" to "N",
        )
        assertFalse(rule.check(values, params))
    }

    @Test
    fun `missing expectedField throws`() {
        assertThrows(IllegalArgumentException::class.java) {
            rule.check(mapOf("a" to 1.0), mapOf("fields" to listOf("a")))
        }
    }

    @Test
    fun `missing referenced value throws`() {
        val params: Map<String, Any> = mapOf("fields" to listOf("a"), "expectedField" to "N")
        assertThrows(IllegalArgumentException::class.java) {
            rule.check(mapOf("a" to 1.0), params)
        }
    }
}
