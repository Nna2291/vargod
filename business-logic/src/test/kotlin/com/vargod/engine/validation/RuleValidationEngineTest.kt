package com.vargod.engine.validation

import com.vargod.domain.ValidationStatus
import com.vargod.engine.ConstraintRule
import com.vargod.engine.ConstraintRuleRegistry
import com.vargod.support.RealEngines
import com.vargod.testing.BoundaryValues
import com.vargod.testing.Classical
import com.vargod.testing.EquivalencePartitioning
import com.vargod.testing.ErrorGuessing
import com.vargod.testing.London
import com.vargod.testing.builders.aValidator
import com.vargod.testing.mothers.ValidatorMother
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import io.qameta.allure.Epic
import io.qameta.allure.Feature
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@Classical
@Epic("Business logic")
@Feature("Validation engine")
@DisplayName("RuleValidationEngine — classical style (real rules)")
class RuleValidationEngineClassicalTest {

    private lateinit var sut: RuleValidationEngine

    @BeforeEach
    fun setUp() {
        sut = RealEngines.validationEngine()
    }

    @Test
    @EquivalencePartitioning
    fun `validate - all satisfied validators give one PASS per validator`() {
        // Arrange
        val validators = listOf(
            ValidatorMother.positive("N"),
            ValidatorMother.sirPopulationConservation(),
            ValidatorMother.ascending("t0", "t1"),
        )
        val values = mapOf<String, Any>("S" to 999.0, "I" to 1.0, "R" to 0.0, "N" to 1000.0, "t0" to 0.0, "t1" to 1.0)

        // Act
        val results = sut.validate(values, validators)

        // Assert
        assertThat(results).hasSize(3).allMatch { it.status == ValidationStatus.PASS && it.message == null }
        assertThat(results.map { it.validatorId }).containsExactlyElementsOf(validators.map { it.id })
    }

    @Test
    @EquivalencePartitioning
    fun `validate - violated RANGE gives FAIL with message from the rule`() {
        // Arrange
        val validator = ValidatorMother.fraction("gamma")

        // Act
        val results = sut.validate(mapOf("gamma" to 1.5), listOf(validator))

        // Assert
        assertThat(results).singleElement().satisfies({
            assertThat(it.status).isEqualTo(ValidationStatus.FAIL)
            assertThat(it.message).isEqualTo("Field 'gamma' = 1.5 violates max=1.0 (exclusive=false)")
        })
    }

    @Test
    @BoundaryValues
    fun `validate - no validators gives no results`() {
        // Arrange
        val values = mapOf<String, Any>("P" to -1.0)

        // Act
        val results = sut.validate(values, emptyList())

        // Assert
        assertThat(results).isEmpty()
    }

    @Test
    @ErrorGuessing
    fun `validate - unknown rule type gives FAIL instead of exception`() {
        // Arrange
        val validator = ValidatorMother.constraintOfUnknownType()

        // Act
        val results = sut.validate(emptyMap(), listOf(validator))

        // Assert
        assertThat(results.single().message).isEqualTo("Unknown constraint rule type: NO_SUCH_RULE")
    }

}

/**
 * Тот же движок в лондонском стиле: реестр и правило — mock'и. Проверяется
 * не результат вычислений, а КОНТРАКТ взаимодействия: какой тип правила
 * запрашивается и какие параметры передаются в ConstraintRule.check.
 */
@London
@Epic("Business logic")
@Feature("Validation engine")
@DisplayName("RuleValidationEngine — London style (MockK)")
@ExtendWith(MockKExtension::class)
class RuleValidationEngineLondonTest {

    @MockK private lateinit var registry: ConstraintRuleRegistry
    @MockK private lateinit var rule: ConstraintRule

    private lateinit var sut: RuleValidationEngine

    @BeforeEach
    fun setUp() {
        sut = RuleValidationEngine(registry)
    }

    @Test
    @EquivalencePartitioning
    fun `validate - RANGE validator gets targetField injected as field parameter`() {
        // Arrange
        val validator = aValidator().range("beta", min = 0.0).build()
        val values = mapOf<String, Any>("beta" to 0.3)
        every { registry.getByType("RANGE") } returns rule
        every { rule.check(values, mapOf("min" to 0.0, "field" to "beta")) } returns true

        // Act
        val results = sut.validate(values, listOf(validator))

        // Assert
        assertThat(results.single().status).isEqualTo(ValidationStatus.PASS)
        verify(exactly = 1) { rule.check(values, mapOf("min" to 0.0, "field" to "beta")) }
    }

    @Test
    @EquivalencePartitioning
    fun `validate - CONSTRAINT rules are passed to the rule unchanged`() {
        // Arrange
        val validator = ValidatorMother.ascending("a", "b")
        val values = mapOf<String, Any>("a" to 2.0, "b" to 1.0)
        every { registry.getByType("ORDERED") } returns rule
        every { rule.check(values, validator.rules) } returns false

        // Act
        val results = sut.validate(values, listOf(validator))

        // Assert
        assertThat(results.single().message).isEqualTo("Constraint 'ORDERED' violated")
        verify(exactly = 1) { rule.check(values, validator.rules) }
    }

    @Test
    @ErrorGuessing
    fun `validate - IllegalArgumentException from rule becomes FAIL with its message`() {
        // Arrange
        val validator = ValidatorMother.ascending("a", "b")
        every { registry.getByType(any()) } returns rule
        every { rule.check(any(), any()) } throws IllegalArgumentException("Value 'b' must be a number, got null")

        // Act
        val results = sut.validate(mapOf("a" to 1.0), listOf(validator))

        // Assert
        assertThat(results.single().status).isEqualTo(ValidationStatus.FAIL)
        assertThat(results.single().message).isEqualTo("Value 'b' must be a number, got null")
    }

}
