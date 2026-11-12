package com.vargod.engine.validation

import com.vargod.engine.validation.RuleParamsSchema.FieldSpec
import com.vargod.engine.validation.RuleParamsSchema.ParamType
import com.vargod.testing.BoundaryValues
import com.vargod.testing.Classical
import com.vargod.testing.EquivalencePartitioning
import com.vargod.testing.ErrorGuessing
import io.qameta.allure.Epic
import io.qameta.allure.Feature
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.MethodSource

@Classical
@Epic("Business logic")
@Feature("Validation rules")
@DisplayName("RuleParamsSchema")
class RuleParamsSchemaTest {

    private fun schemaOf(type: ParamType, required: Boolean = true, allowed: List<String> = emptyList()) =
        RuleParamsSchema(listOf(FieldSpec("p", type, required = required, allowedValues = allowed)))

    /** Классы эквивалентности «тип параметра × допустимое / недопустимое значение». */
    @ParameterizedTest(name = "{0} accepts {1}")
    @MethodSource("validValues")
    @EquivalencePartitioning
    fun `validate - value of the declared type is accepted`(type: ParamType, value: Any) {
        // Arrange
        val schema = schemaOf(type)

        // Act
        val errors = schema.validate(mapOf("p" to value))

        // Assert
        assertThat(errors).isEmpty()
    }

    @ParameterizedTest(name = "{0} rejects {1}")
    @MethodSource("invalidValues")
    @EquivalencePartitioning
    fun `validate - value of another type is rejected with parameter name`(type: ParamType, value: Any) {
        // Arrange
        val schema = schemaOf(type)

        // Act
        val errors = schema.validate(mapOf("p" to value))

        // Assert
        assertThat(errors).singleElement().asString().contains("'p'")
    }

    @ParameterizedTest(name = "{0} -> integer={1}")
    @CsvSource("2.999999, false", "3.0, true", "3.000001, false")
    @BoundaryValues
    fun `validate - INTEGER accepts a double only when it is whole`(value: Double, isInteger: Boolean) {
        // Arrange
        val schema = schemaOf(ParamType.INTEGER)

        // Act
        val errors = schema.validate(mapOf("p" to value))

        // Assert
        assertThat(errors.isEmpty()).isEqualTo(isInteger)
    }

    @Test
    @EquivalencePartitioning
    fun `validate - missing required parameter is reported`() {
        // Arrange
        val schema = schemaOf(ParamType.NUMBER, required = true)

        // Act
        val errors = schema.validate(emptyMap())

        // Assert
        assertThat(errors).containsExactly("Missing required parameter 'p'")
    }

    @Test
    @EquivalencePartitioning
    fun `validate - string outside allowed values is rejected`() {
        // Arrange
        val schema = schemaOf(ParamType.STRING, allowed = listOf("A", "B"))

        // Act
        val errors = schema.validate(mapOf("p" to "C"))

        // Assert
        assertThat(errors).containsExactly("Parameter 'p' must be one of [A, B], got 'C'")
    }

    companion object {
        @JvmStatic
        fun validValues(): List<Arguments> = listOf(
            Arguments.of(ParamType.NUMBER, 1.5),
            Arguments.of(ParamType.INTEGER, 7),
            Arguments.of(ParamType.STRING, "text"),
            Arguments.of(ParamType.BOOLEAN, true),
            Arguments.of(ParamType.LIST_OF_STRING, listOf("a", "b")),
        )

        @JvmStatic
        fun invalidValues(): List<Arguments> = listOf(
            Arguments.of(ParamType.NUMBER, "1.5"),
            Arguments.of(ParamType.INTEGER, 7.5),
            Arguments.of(ParamType.STRING, 1),
            Arguments.of(ParamType.BOOLEAN, "true"),
            Arguments.of(ParamType.LIST_OF_STRING, "a,b"),
        )
    }
}
