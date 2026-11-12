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
@DisplayName("SumEqualsFieldRule")
class SumEqualsFieldRuleTest {

    private lateinit var sut: SumEqualsFieldRule

    /** S + I + R = N с допуском 0.5. */
    private val sirParams: Map<String, Any> = mapOf(
        "fields" to listOf("S", "I", "R"),
        "expectedField" to "N",
        "tolerance" to 0.5,
    )

    @BeforeEach
    fun setUp() {
        sut = SumEqualsFieldRule()
    }

    @Test
    @EquivalencePartitioning
    fun `type - is SUM_EQUALS_FIELD`() {
        // Arrange
        val expected = "SUM_EQUALS_FIELD"

        // Act
        val type = sut.type()

        // Assert
        assertThat(type).isEqualTo(expected)
    }

    @Test
    @ErrorGuessing
    fun `type - differs from the similar SUM_EQUALS_VALUE rule`() {
        // Arrange
        val similar = SumEqualsValueRule().type()

        // Act
        val type = sut.type()

        // Assert
        assertThat(type).isNotEqualTo(similar)
    }

    @Test
    @EquivalencePartitioning
    fun `paramsSchema - tolerance is the only optional parameter`() {
        // Arrange
        val expectedOptional = listOf("tolerance")

        // Act
        val schema = sut.paramsSchema()

        // Assert
        assertThat(schema.fields.filterNot { it.required }.map { it.name }).isEqualTo(expectedOptional)
    }

    @Test
    @ErrorGuessing
    fun `paramsSchema - expectedField must be a string`() {
        // Arrange
        val schema = sut.paramsSchema()

        // Act
        val errors = schema.validate(mapOf("fields" to listOf("a"), "expectedField" to 42))

        // Assert
        assertThat(errors).containsExactly("Parameter 'expectedField' must be a string")
    }

    @Test
    @EquivalencePartitioning
    fun `check - sum equal to referenced field passes`() {
        // Arrange
        val values = mapOf("S" to 990.0, "I" to 10.0, "R" to 0.0, "N" to 1000.0)

        // Act
        val result = sut.check(values, sirParams)

        // Assert
        assertThat(result).isTrue()
    }

    @Test
    @BoundaryValues
    fun `check - difference exactly equal to tolerance passes`() {
        // Arrange
        val values = mapOf("S" to 990.0, "I" to 10.0, "R" to 0.5, "N" to 1000.0)

        // Act
        val result = sut.check(values, sirParams)

        // Assert
        assertThat(result).isTrue()
    }

    @Test
    @BoundaryValues
    fun `check - difference just above tolerance fails`() {
        // Arrange
        val values = mapOf("S" to 990.0, "I" to 10.0, "R" to 0.5000001, "N" to 1000.0)

        // Act
        val result = sut.check(values, sirParams)

        // Assert
        assertThat(result).isFalse()
    }

    @Test
    @ErrorGuessing
    fun `check - missing expectedField parameter is reported`() {
        // Arrange
        val params = sirParams - "expectedField"

        // Act
        val error = assertThrows<IllegalArgumentException> { sut.check(mapOf("S" to 1.0, "I" to 1.0, "R" to 1.0), params) }

        // Assert
        assertThat(error.message).isEqualTo("Missing 'expectedField' parameter")
    }

    @Test
    @EquivalencePartitioning
    fun `validateParams - complete configuration has no errors`() {
        // Arrange
        val complete = sirParams

        // Act
        val errors = sut.validateParams(complete)

        // Assert
        assertThat(errors).isEmpty()
    }

    @Test
    @EquivalencePartitioning
    fun `validateParams - fields given as plain string are rejected`() {
        // Arrange
        val wrong = mapOf<String, Any>("fields" to "S,I,R", "expectedField" to "N")

        // Act
        val errors = sut.validateParams(wrong)

        // Assert
        assertThat(errors).containsExactly("Parameter 'fields' must be a list of strings")
    }
}
