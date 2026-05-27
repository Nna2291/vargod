package com.vargod.domain

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.UUID

class DomainExceptionsTest {

    @Test
    fun `NotFoundException carries entity name and id`() {
        val id = UUID.randomUUID()
        val ex = NotFoundException("Experiment", id)
        assertTrue(ex.message!!.contains("Experiment"))
        assertTrue(ex.message!!.contains(id.toString()))
    }

    @Test
    fun `AuthorizationException carries message`() {
        val ex = AuthorizationException("forbidden")
        assertEquals("forbidden", ex.message)
    }

    @Test
    fun `ValidationException aggregates failure messages`() {
        val v1 = UUID.randomUUID()
        val v2 = UUID.randomUUID()
        val results = listOf(
            ValidationResult(ValidationStatus.PASS, null, v1),
            ValidationResult(ValidationStatus.FAIL, "bad value", v2),
        )
        val ex = ValidationException(results)
        assertTrue(ex.message!!.contains("bad value"))
        assertEquals(results, ex.results)
    }

    @Test
    fun `IllegalExperimentStateException carries message`() {
        val ex = IllegalExperimentStateException("not pending")
        assertEquals("not pending", ex.message)
    }
}
