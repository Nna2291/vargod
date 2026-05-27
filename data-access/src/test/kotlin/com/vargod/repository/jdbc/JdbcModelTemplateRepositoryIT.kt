package com.vargod.repository.jdbc

import com.vargod.testfixtures.Fixtures
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.UUID

class JdbcModelTemplateRepositoryIT : JdbcRepositoryTestBase() {

    @Test
    fun `findAll and findById return seeded templates with their validators`() {
        val sirValidators = listOf(
            Fixtures.rangeValidator(targetField = "alpha", min = 0.0, max = 1.0),
            Fixtures.constraintValidator(
                type = "SUM_EQUALS_VALUE",
                extraRules = mapOf("fields" to listOf("S", "I", "R"), "expected" to 1.0),
            ),
        )
        val sir = Fixtures.model(code = "SIR", validators = sirValidators)
        val logistic = Fixtures.model(code = "LOGISTIC", validators = emptyList())
        seedModelTemplate(sir)
        seedModelTemplate(logistic)

        val all = modelTemplateRepository.findAll()
        assertEquals(2, all.size)
        val byCode = all.associateBy { it.code }
        assertEquals(2, byCode["SIR"]!!.validators.size)
        assertTrue(byCode["LOGISTIC"]!!.validators.isEmpty())

        val foundSir = modelTemplateRepository.findById(sir.id)
        assertNotNull(foundSir)
        assertEquals("SIR", foundSir!!.code)
        assertEquals(2, foundSir.validators.size)
        val ruleTypes = foundSir.validators.map { it.checkType.name }.toSet()
        assertEquals(setOf("RANGE", "CONSTRAINT"), ruleTypes)

        assertNull(modelTemplateRepository.findById(UUID.randomUUID()))
    }
}
