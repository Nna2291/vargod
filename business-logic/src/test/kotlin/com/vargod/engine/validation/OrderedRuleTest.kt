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
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

@Classical
@Epic("Business logic")
@Feature("Validation rules")
@DisplayName("OrderedRule")
class OrderedRuleTest {

    private lateinit var sut: OrderedRule

    @BeforeEach
    fun setUp() {
        sut = OrderedRule()
    }

    private fun params(direction: String?, vararg fields: String): Map<String, Any> = buildMap {
        put("fields", fields.toList())
        direction?.let { put("direction", it) }
    }

    @Test
    @EquivalencePartitioning
    fun `type - is ORDERED`() {
        // Arrange
        val expected = "ORDERED"

        // Act
        val type = sut.type()

        // Assert
        assertThat(type).isEqualTo(expected)
    }

    @Test
    @ErrorGuessing
    fun `type - differs from every other production rule`() {
        // Arrange
        val others = listOf(RangeRule(), SumEqualsFieldRule(), SumEqualsValueRule()).map { it.type() }

        // Act
        val type = sut.type()

        // Assert
        assertThat(others).doesNotContain(type)
    }

    @Test
    @EquivalencePartitioning
    fun `paramsSchema - requires fields and direction with fixed allowed values`() {
        // Arrange
        val expectedAllowed = listOf("ASCENDING", "DESCENDING")

        // Act
        val schema = sut.paramsSchema()

        // Assert
        assertThat(schema.fields.map { it.name }).containsExactly("fields", "direction")
        assertThat(schema.fields).allMatch { it.required }
        assertThat(schema.fields.single { it.name == "direction" }.allowedValues).isEqualTo(expectedAllowed)
    }

    @Test
    @ErrorGuessing
    fun `paramsSchema - reports both missing required parameters`() {
        // Arrange
        val schema = sut.paramsSchema()

        // Act
        val errors = schema.validate(emptyMap())

        // Assert
        assertThat(errors).containsExactly(
            "Missing required parameter 'fields'",
            "Missing required parameter 'direction'",
        )
    }

    /** Классы эквивалентности: строго возрастает / равны / убывает × направление. */
    @ParameterizedTest(name = "{0}: a={1}, b={2}, c={3} -> {4}")
    @CsvSource(
        "ASCENDING,  1.0, 2.0, 3.0, true",
        "ASCENDING,  1.0, 1.0, 3.0, false",
        "DESCENDING, 3.0, 2.0, 1.0, true",
        "ascending,  1.0, 2.0, 3.0, true",
    )
    @EquivalencePartitioning
    fun `check - strict ordering in requested direction`(direction: String, a: Double, b: Double, c: Double, expected: Boolean) {
        // Arrange
        val values = mapOf("a" to a, "b" to b, "c" to c)

        // Act
        val result = sut.check(values, params(direction, "a", "b", "c"))

        // Assert
        assertThat(result).isEqualTo(expected)
    }

    @Test
    @BoundaryValues
    fun `check - a single field is below the minimum of two`() {
        // Arrange
        val values = mapOf("a" to 1.0)

        // Act
        val error = assertThrows<IllegalArgumentException> { sut.check(values, params("ASCENDING", "a")) }

        // Assert
        assertThat(error.message).isEqualTo("ORDERED requires at least 2 fields")
    }

    @Test
    @ErrorGuessing
    fun `check - unknown direction is reported`() {
        // Arrange
        val values = mapOf("a" to 1.0, "b" to 2.0)

        // Act
        val error = assertThrows<IllegalArgumentException> { sut.check(values, params("SIDEWAYS", "a", "b")) }

        // Assert
        assertThat(error.message).contains("Unknown direction 'SIDEWAYS'")
    }

    @Test
    @BoundaryValues
    fun `validateParams - two fields are the minimal valid configuration`() {
        // Arrange
        val minimal = params("DESCENDING", "a", "b")

        // Act
        val errors = sut.validateParams(minimal)

        // Assert
        assertThat(errors).isEmpty()
    }

    @Test
    @BoundaryValues
    fun `validateParams - one field is rejected`() {
        // Arrange
        val tooFew = params("DESCENDING", "a")

        // Act
        val errors = sut.validateParams(tooFew)

        // Assert
        assertThat(errors).containsExactly("ORDERED requires at least 2 fields")
    }

}
