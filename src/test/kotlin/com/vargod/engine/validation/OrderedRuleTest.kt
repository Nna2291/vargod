package com.vargod.engine.validation

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class OrderedRuleTest {

    private val rule = OrderedRule()

    @Test
    fun `type is ORDERED`() {
        assertEquals("ORDERED", rule.type())
    }

    @Test
    fun `ascending strict passes`() {
        val values = mapOf("t1" to 0.0, "t2" to 1.0, "t3" to 2.5)
        val params: Map<String, Any> = mapOf(
            "fields" to listOf("t1", "t2", "t3"),
            "direction" to "ASCENDING",
        )
        assertTrue(rule.check(values, params))
    }

    @Test
    fun `non-strict ascending fails`() {
        val values = mapOf("t1" to 1.0, "t2" to 1.0, "t3" to 2.0)
        val params: Map<String, Any> = mapOf(
            "fields" to listOf("t1", "t2", "t3"),
            "direction" to "ASCENDING",
        )
        assertFalse(rule.check(values, params))
    }

    @Test
    fun `descending detects out-of-order`() {
        val values = mapOf("a" to 3.0, "b" to 5.0, "c" to 1.0)
        val params: Map<String, Any> = mapOf(
            "fields" to listOf("a", "b", "c"),
            "direction" to "DESCENDING",
        )
        assertFalse(rule.check(values, params))
    }

    @Test
    fun `descending strict passes`() {
        val values = mapOf("a" to 5.0, "b" to 3.0, "c" to 1.0)
        val params: Map<String, Any> = mapOf(
            "fields" to listOf("a", "b", "c"),
            "direction" to "descending", // case-insensitive
        )
        assertTrue(rule.check(values, params))
    }

    @Test
    fun `unknown direction throws`() {
        val values = mapOf("a" to 1.0, "b" to 2.0)
        assertThrows(IllegalArgumentException::class.java) {
            rule.check(
                values,
                mapOf("fields" to listOf("a", "b"), "direction" to "SIDEWAYS"),
            )
        }
    }

    @Test
    fun `at least two fields required`() {
        assertThrows(IllegalArgumentException::class.java) {
            rule.check(mapOf("a" to 1.0), mapOf("fields" to listOf("a"), "direction" to "ASCENDING"))
        }
    }

    @Test
    fun `missing direction throws`() {
        assertThrows(IllegalArgumentException::class.java) {
            rule.check(mapOf("a" to 1.0, "b" to 2.0), mapOf("fields" to listOf("a", "b")))
        }
    }
}
