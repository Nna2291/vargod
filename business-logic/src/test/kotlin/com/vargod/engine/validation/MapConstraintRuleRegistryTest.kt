package com.vargod.engine.validation

import com.vargod.engine.ConstraintRule
import com.vargod.support.RealEngines
import com.vargod.testing.BoundaryValues
import com.vargod.testing.Classical
import com.vargod.testing.EquivalencePartitioning
import com.vargod.testing.ErrorGuessing
import io.qameta.allure.Epic
import io.qameta.allure.Feature
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

@Classical
@Epic("Business logic")
@Feature("Validation rules")
@DisplayName("MapConstraintRuleRegistry")
class MapConstraintRuleRegistryTest {

    /** Минимальная реализация правила с произвольным type — для проверок конструктора. */
    private fun ruleOfType(type: String): ConstraintRule = object : ConstraintRule {
        override fun type() = type
        override fun paramsSchema() = RuleParamsSchema(emptyList())
        override fun check(values: Map<String, Any>, params: Map<String, Any>) = true
    }

    @Test
    @EquivalencePartitioning
    fun `getByType - returns the registered rule instance`() {
        // Arrange
        val ordered = OrderedRule()
        val sut = MapConstraintRuleRegistry(listOf(RangeRule(), ordered))

        // Act
        val result = sut.getByType("ORDERED")

        // Assert
        assertThat(result).isSameAs(ordered)
    }

    @Test
    @EquivalencePartitioning
    fun `getByType - unknown type gives IllegalArgumentException`() {
        // Arrange
        val sut = RealEngines.ruleRegistry()

        // Act
        val error = assertThrows<IllegalArgumentException> { sut.getByType("NO_SUCH_RULE") }

        // Assert
        assertThat(error.message).isEqualTo("Unknown constraint rule type: NO_SUCH_RULE")
    }

    @Test
    @EquivalencePartitioning
    fun `listAll - keeps registration order and keys rules by type`() {
        // Arrange
        val sut = RealEngines.ruleRegistry()

        // Act
        val all = sut.listAll()

        // Assert
        assertThat(all.keys).containsExactly("RANGE", "SUM_EQUALS_VALUE", "SUM_EQUALS_FIELD", "ORDERED")
        assertThat(all).allSatisfy { type, rule -> assertThat(rule.type()).isEqualTo(type) }
    }

    @Test
    @BoundaryValues
    fun `listAll - registry without rules is empty`() {
        // Arrange
        val sut = MapConstraintRuleRegistry(emptyList())

        // Act
        val all = sut.listAll()

        // Assert
        assertThat(all).isEmpty()
    }

    @Test
    @ErrorGuessing
    fun `constructor - duplicate rule type is rejected`() {
        // Arrange
        val rules = listOf(RangeRule(), RangeRule())

        // Act
        val error = assertThrows<IllegalArgumentException> { MapConstraintRuleRegistry(rules) }

        // Assert
        assertThat(error.message).isEqualTo("Duplicate constraint rule type: RANGE")
    }

    @Test
    @BoundaryValues
    fun `constructor - blank rule type is rejected`() {
        // Arrange
        val rules = listOf(ruleOfType("   "))

        // Act
        val error = assertThrows<IllegalArgumentException> { MapConstraintRuleRegistry(rules) }

        // Assert
        assertThat(error.message).contains("must not be blank")
    }
}
