package com.vargod.engine.validation

import com.vargod.domain.ValidationStatus
import com.vargod.engine.ConstraintRule
import com.vargod.engine.ConstraintRuleRegistry
import com.vargod.testfixtures.Fixtures
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class RuleValidationEngineTest {

    private val ruleRegistry = mockk<ConstraintRuleRegistry>()
    private val engine = RuleValidationEngine(ruleRegistry)

    @Test
    fun `passes value inside inclusive range`() {
        val v = Fixtures.rangeValidator("beta", min = 0.0, max = 1.0)
        val results = engine.validate(mapOf("beta" to 0.5), listOf(v))
        assertEquals(1, results.size)
        assertEquals(ValidationStatus.PASS, results[0].status)
        assertEquals(v.id, results[0].validatorId)
    }

    @Test
    fun `passes value on inclusive boundary`() {
        val v = Fixtures.rangeValidator("beta", min = 0.0, max = 1.0)
        assertEquals(ValidationStatus.PASS, engine.validate(mapOf("beta" to 0.0), listOf(v)).single().status)
        assertEquals(ValidationStatus.PASS, engine.validate(mapOf("beta" to 1.0), listOf(v)).single().status)
    }

    @Test
    fun `fails value on exclusive boundary`() {
        val v = Fixtures.rangeValidator("beta", min = 0.0, max = 1.0, minExclusive = true, maxExclusive = true)
        assertEquals(ValidationStatus.FAIL, engine.validate(mapOf("beta" to 0.0), listOf(v)).single().status)
        assertEquals(ValidationStatus.FAIL, engine.validate(mapOf("beta" to 1.0), listOf(v)).single().status)
    }

    @Test
    fun `fails value below min`() {
        val v = Fixtures.rangeValidator("beta", min = 0.0)
        val r = engine.validate(mapOf("beta" to -0.1), listOf(v)).single()
        assertEquals(ValidationStatus.FAIL, r.status)
        assert(r.message!!.contains("min"))
    }

    @Test
    fun `fails value above max`() {
        val v = Fixtures.rangeValidator("beta", max = 1.0)
        val r = engine.validate(mapOf("beta" to 1.5), listOf(v)).single()
        assertEquals(ValidationStatus.FAIL, r.status)
        assert(r.message!!.contains("max"))
    }

    @Test
    fun `fails when range value is missing`() {
        val v = Fixtures.rangeValidator("beta", min = 0.0)
        val r = engine.validate(emptyMap(), listOf(v)).single()
        assertEquals(ValidationStatus.FAIL, r.status)
        assert(r.message!!.contains("missing"))
    }

    @Test
    fun `fails when range value is not numeric`() {
        val v = Fixtures.rangeValidator("beta", min = 0.0)
        val r = engine.validate(mapOf("beta" to "oops"), listOf(v)).single()
        assertEquals(ValidationStatus.FAIL, r.status)
    }

    @Test
    fun `fails when range validator lacks min and max`() {
        val v = Fixtures.rangeValidator("beta")
        val r = engine.validate(mapOf("beta" to 1.0), listOf(v)).single()
        assertEquals(ValidationStatus.FAIL, r.status)
    }

    @Test
    fun `constraint passes when rule returns true`() {
        val rule = mockk<ConstraintRule>()
        every { rule.check(any(), any()) } returns true
        every { ruleRegistry.getByType("CUSTOM") } returns rule
        val v = Fixtures.constraintValidator("CUSTOM")
        val r = engine.validate(mapOf("x" to 1.0), listOf(v)).single()
        assertEquals(ValidationStatus.PASS, r.status)
    }

    @Test
    fun `constraint fails when rule returns false`() {
        val rule = mockk<ConstraintRule>()
        every { rule.check(any(), any()) } returns false
        every { ruleRegistry.getByType("CUSTOM") } returns rule
        val v = Fixtures.constraintValidator("CUSTOM")
        val r = engine.validate(mapOf("x" to 1.0), listOf(v)).single()
        assertEquals(ValidationStatus.FAIL, r.status)
        assert(r.message!!.contains("CUSTOM"))
    }

    @Test
    fun `unknown constraint type yields FAIL`() {
        every { ruleRegistry.getByType("UNKNOWN") } throws IllegalArgumentException("Unknown rule UNKNOWN")
        val v = Fixtures.constraintValidator("UNKNOWN")
        val r = engine.validate(emptyMap(), listOf(v)).single()
        assertEquals(ValidationStatus.FAIL, r.status)
        assert(r.message!!.contains("Unknown"))
    }

    @Test
    fun `constraint without type yields FAIL`() {
        val v = com.vargod.domain.Validator(
            id = java.util.UUID.randomUUID(),
            name = "broken",
            checkType = com.vargod.domain.ValidatorType.CONSTRAINT,
            targetKind = com.vargod.domain.ValidatorTargetKind.PARAMETER,
            targetField = "*",
            rules = emptyMap(),
        )
        val r = engine.validate(emptyMap(), listOf(v)).single()
        assertEquals(ValidationStatus.FAIL, r.status)
        assert(r.message!!.contains("type"))
    }

    @Test
    fun `constraint check throwing IllegalArgumentException yields FAIL`() {
        val rule = mockk<ConstraintRule>()
        every { rule.check(any(), any()) } throws IllegalArgumentException("missing field")
        every { ruleRegistry.getByType("CUSTOM") } returns rule
        val v = Fixtures.constraintValidator("CUSTOM")
        val r = engine.validate(emptyMap(), listOf(v)).single()
        assertEquals(ValidationStatus.FAIL, r.status)
        assert(r.message!!.contains("missing field"))
    }

    @Test
    fun `mixed validators produce one result per validator`() {
        val rule = mockk<ConstraintRule>()
        every { rule.check(any(), any()) } returns true
        every { ruleRegistry.getByType("CUSTOM") } returns rule
        val v1 = Fixtures.rangeValidator("beta", min = 0.0, max = 1.0)
        val v2 = Fixtures.constraintValidator("CUSTOM")
        val results = engine.validate(mapOf("beta" to 0.5), listOf(v1, v2))
        assertEquals(2, results.size)
        assertEquals(v1.id, results[0].validatorId)
        assertEquals(v2.id, results[1].validatorId)
    }
}
