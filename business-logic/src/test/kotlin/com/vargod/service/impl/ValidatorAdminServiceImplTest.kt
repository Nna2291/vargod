package com.vargod.service.impl

import com.vargod.domain.ValidationException
import com.vargod.domain.Validator
import com.vargod.domain.ValidatorTargetKind
import com.vargod.domain.ValidatorType
import com.vargod.domain.repository.ValidatorRepository
import com.vargod.engine.validation.MapConstraintRuleRegistry
import com.vargod.engine.validation.OrderedRule
import com.vargod.engine.validation.RangeRule
import com.vargod.engine.validation.SumEqualsFieldRule
import com.vargod.engine.validation.SumEqualsValueRule
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.UUID

/**
 * Контракт: create/update прогоняют rules через ConstraintRule.validateParams.
 * Если payload не проходит — летит ValidationException ДО обращения к
 * репозиторию (это и есть смысл «422 при создании, а не при запуске»).
 */
class ValidatorAdminServiceImplTest {

    private val repo = mockk<ValidatorRepository>(relaxUnitFun = true)
    private val registry = MapConstraintRuleRegistry(
        listOf(RangeRule(), SumEqualsValueRule(), SumEqualsFieldRule(), OrderedRule())
    )
    private val service = ValidatorAdminServiceImpl(repo, registry)

    @Test
    fun `create with RANGE validator missing both min and max throws ValidationException`() {
        val malformed = rangeValidator(rules = mapOf<String, Any>("minExclusive" to true))
        assertThrows(ValidationException::class.java) {
            service.create(malformed, modelTemplateId = UUID.randomUUID(), createdBy = null)
        }
        // Репозиторий НЕ должен был получить save.
        verify(exactly = 0) { repo.save(any(), any(), any()) }
    }

    @Test
    fun `create with RANGE where min greater than max throws ValidationException with descriptive message`() {
        val malformed = rangeValidator(rules = mapOf("min" to 5.0, "max" to 1.0))
        val ex = assertThrows(ValidationException::class.java) {
            service.create(malformed, modelTemplateId = UUID.randomUUID(), createdBy = null)
        }
        assertTrue(
            ex.results.any { (it.message ?: "").contains("'min'") && (it.message ?: "").contains("'max'") },
            "Ожидалось упоминание ограничения min/max: ${ex.results.map { it.message }}",
        )
    }

    @Test
    fun `create with CONSTRAINT missing type in rules throws ValidationException`() {
        val malformed = Validator(
            id = UUID.randomUUID(),
            name = "broken",
            checkType = ValidatorType.CONSTRAINT,
            targetKind = ValidatorTargetKind.PARAMETER,
            targetField = "x",
            rules = mapOf("value" to 42.0),
        )
        assertThrows(ValidationException::class.java) {
            service.create(malformed, modelTemplateId = UUID.randomUUID(), createdBy = null)
        }
    }

    @Test
    fun `create with valid RANGE delegates to repository`() {
        val ok = rangeValidator(rules = mapOf("min" to 0.0, "max" to 100.0))
        val modelId = UUID.randomUUID()
        val captured = slot<Validator>()
        every { repo.save(capture(captured), modelId, null) } answers { captured.captured }

        val saved = service.create(ok, modelId, createdBy = null)

        assertEquals(ok.id, saved.id)
        verify(exactly = 1) { repo.save(any(), modelId, null) }
    }

    private fun rangeValidator(rules: Map<String, Any>) = Validator(
        id = UUID.randomUUID(),
        name = "test-range",
        checkType = ValidatorType.RANGE,
        targetKind = ValidatorTargetKind.PARAMETER,
        targetField = "x",
        rules = rules,
    )
}
