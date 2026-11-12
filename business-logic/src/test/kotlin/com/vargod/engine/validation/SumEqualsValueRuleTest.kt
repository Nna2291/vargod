package com.vargod.engine.validation

import com.vargod.testing.BoundaryValues
import com.vargod.testing.Classical
import com.vargod.testing.EquivalencePartitioning
import com.vargod.testing.ErrorGuessing
import io.qameta.allure.Epic
import io.qameta.allure.Feature
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

@Classical
@Epic("Business logic")
@Feature("Validation rules")
@DisplayName("SumEqualsValueRule")
class SumEqualsValueRuleTest {

    private lateinit var sut: SumEqualsValueRule

    private val fractions: Map<String, Any> = mapOf("fields" to listOf("a", "b", "c"), "expected" to 1.0)

    @BeforeEach
    fun setUp() {
        sut = SumEqualsValueRule()
    }

    @Test
    @EquivalencePartitioning
    fun `type - is SUM_EQUALS_VALUE`() {
        // Arrange
        val expected = SumEqualsValueRule.TYPE

        // Act
        val type = sut.type()

        // Assert
        assertThat(type).isEqualTo(expected).isEqualTo("SUM_EQUALS_VALUE")
    }

    @Test
    @ErrorGuessing
    fun `type - differs from the similar SUM_EQUALS_FIELD rule`() {
        // Arrange
        val similar = SumEqualsFieldRule.TYPE

        // Act
        val type = sut.type()

        // Assert
        assertThat(type).isNotEqualTo(similar)
    }

    @Test
    @EquivalencePartitioning
    fun `paramsSchema - fields and expected are required`() {
        // Arrange
        val expectedRequired = listOf("fields", "expected")

        // Act
        val schema = sut.paramsSchema()

        // Assert
        assertThat(schema.fields.filter { it.required }.map { it.name }).isEqualTo(expectedRequired)
    }

    @Test
    @ErrorGuessing
    fun `paramsSchema - expected given as text is rejected`() {
        // Arrange
        val schema = sut.paramsSchema()

        // Act
        val errors = schema.validate(mapOf("fields" to listOf("a"), "expected" to "1"))

        // Assert
        assertThat(errors).containsExactly("Parameter 'expected' must be a number, got String")
    }

    @Test
    @EquivalencePartitioning
    fun `check - fractions summing to one pass`() {
        // Arrange
        val values = mapOf("a" to 0.2, "b" to 0.3, "c" to 0.5)

        // Act
        val result = sut.check(values, fractions)

        // Assert
        assertThat(result).isTrue()
    }

    @Test
    @BoundaryValues
    fun `check - difference twice the default tolerance fails`() {
        // Arrange
        val values = mapOf("a" to 0.5, "b" to 0.5, "c" to 2 * SumEqualsValueRule.DEFAULT_TOLERANCE)

        // Act
        val result = sut.check(values, fractions)

        // Assert
        assertThat(result).isFalse()
    }

    @Test
    @ErrorGuessing
    fun `check - missing fields parameter is reported`() {
        // Arrange
        val params = mapOf<String, Any>("expected" to 1.0)

        // Act
        val error = assertThrows<IllegalArgumentException> { sut.check(mapOf("a" to 1.0), params) }

        // Assert
        assertThat(error.message).isEqualTo("Missing 'fields' parameter")
    }

    @Test
    @EquivalencePartitioning
    fun `validateParams - complete configuration has no errors`() {
        // Arrange
        val complete = fractions + ("tolerance" to 1e-6)

        // Act
        val errors = sut.validateParams(complete)

        // Assert
        assertThat(errors).isEmpty()
    }

    @Test
    @EquivalencePartitioning
    fun `validateParams - missing expected is reported`() {
        // Arrange
        val incomplete = mapOf<String, Any>("fields" to listOf("a", "b"))

        // Act
        val errors = sut.validateParams(incomplete)

        // Assert
        assertThat(errors).containsExactly("Missing required parameter 'expected'")
    }
}
