package com.vargod.engine.validation

import com.vargod.testing.BoundaryValues
import com.vargod.testing.Classical
import com.vargod.testing.EquivalencePartitioning
import com.vargod.testing.ErrorGuessing
import com.vargod.testing.Pairwise
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
@DisplayName("RangeRule")
class RangeRuleTest {

    private lateinit var sut: RangeRule

    @BeforeEach
    fun setUp() {
        sut = RangeRule()
    }

    private fun params(min: Double? = 0.0, max: Double? = 10.0, minExclusive: Boolean = false, maxExclusive: Boolean = false) =
        buildMap<String, Any> {
            put("field", "x")
            min?.let { put("min", it) }
            max?.let { put("max", it) }
            put("minExclusive", minExclusive)
            put("maxExclusive", maxExclusive)
        }

    // ---------------------------------------------------------------- type / paramsSchema

    @Test
    @EquivalencePartitioning
    fun `type - is RANGE`() {
        // Arrange
        val expected = RangeRule.TYPE

        // Act
        val type = sut.type()

        // Assert
        assertThat(type).isEqualTo(expected).isEqualTo("RANGE")
    }

    @Test
    @ErrorGuessing
    fun `type - differs from every other production rule`() {
        // Arrange
        val others = listOf(OrderedRule(), SumEqualsFieldRule(), SumEqualsValueRule()).map { it.type() }

        // Act
        val type = sut.type()

        // Assert
        assertThat(others).doesNotContain(type)
    }

    @Test
    @EquivalencePartitioning
    fun `paramsSchema - declares four optional fields`() {
        // Arrange
        val expectedNames = listOf("min", "max", "minExclusive", "maxExclusive")

        // Act
        val schema = sut.paramsSchema()

        // Assert
        assertThat(schema.fields.map { it.name }).containsExactlyElementsOf(expectedNames)
        assertThat(schema.fields).noneMatch { it.required }
    }

    @Test
    @ErrorGuessing
    fun `paramsSchema - rejects non-numeric bound`() {
        // Arrange
        val schema = sut.paramsSchema()

        // Act
        val errors = schema.validate(mapOf("min" to "zero"))

        // Assert
        assertThat(errors).containsExactly("Parameter 'min' must be a number, got String")
    }

    // ---------------------------------------------------------------- check

    /**
     * Попарное тестирование трёх параметров: строгость нижней границы (2 значения),
     * строгость верхней границы (2) и положение значения на отрезке [0; 10]
     * (на min / внутри / на max — 3). Полный перебор — 12 наборов, попарный — 6:
     *
     * | minExcl | maxExcl | value     | pass |
     * |---------|---------|-----------|------|
     * | N       | N       | 0 (min)   | Y    |
     * | Y       | Y       | 0 (min)   | N    |
     * | N       | Y       | 5 (inner) | Y    |
     * | Y       | N       | 5 (inner) | Y    |
     * | N       | Y       | 10 (max)  | N    |
     * | Y       | N       | 10 (max)  | Y    |
     *
     * Любая пара значений любых двух параметров встречается хотя бы в одной строке.
     */
    @ParameterizedTest(name = "minExcl={0}, maxExcl={1}, value={2} -> pass={3}")
    @CsvSource(
        "false, false, 0.0,  true",
        "true,  true,  0.0,  false",
        "false, true,  5.0,  true",
        "true,  false, 5.0,  true",
        "false, true,  10.0, false",
        "true,  false, 10.0, true",
    )
    @Pairwise
    fun `check - pairwise combinations of bound strictness and value position`(
        minExclusive: Boolean, maxExclusive: Boolean, value: Double, expectedPass: Boolean,
    ) {
        // Arrange
        val rangeParams = params(minExclusive = minExclusive, maxExclusive = maxExclusive)

        // Act
        val outcome = runCatching { sut.check(mapOf("x" to value), rangeParams) }

        // Assert
        assertThat(outcome.isSuccess).isEqualTo(expectedPass)
        if (!expectedPass) assertThat(outcome.exceptionOrNull()).isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    @BoundaryValues
    fun `check - value one ulp below inclusive minimum is rejected`() {
        // Arrange
        val justBelow = Math.nextDown(0.0)

        // Act
        val error = assertThrows<IllegalArgumentException> { sut.check(mapOf("x" to justBelow), params()) }

        // Assert
        assertThat(error.message).contains("violates min=0.0")
    }

    @Test
    @ErrorGuessing
    fun `check - missing field value is reported`() {
        // Arrange
        val noValues = emptyMap<String, Any>()

        // Act
        val error = assertThrows<IllegalArgumentException> { sut.check(noValues, params()) }

        // Assert
        assertThat(error.message).isEqualTo("Field 'x' is missing")
    }

    @Test
    @ErrorGuessing
    fun `check - non-numeric value is reported`() {
        // Arrange
        val text = mapOf<String, Any>("x" to "ten")

        // Act
        val error = assertThrows<IllegalArgumentException> { sut.check(text, params()) }

        // Assert
        assertThat(error.message).contains("must be numeric")
    }

    @Test
    @ErrorGuessing
    fun `check - range without any bound is a configuration error`() {
        // Arrange
        val unbounded = params(min = null, max = null)

        // Act
        val error = assertThrows<IllegalArgumentException> { sut.check(mapOf("x" to 1.0), unbounded) }

        // Assert
        assertThat(error.message).contains("at least 'min' or 'max'")
    }

    // ---------------------------------------------------------------- validateParams

    @Test
    @EquivalencePartitioning
    fun `validateParams - min and max in order give no errors`() {
        // Arrange
        val ok = mapOf<String, Any>("min" to 0.0, "max" to 1.0, "maxExclusive" to true)

        // Act
        val errors = sut.validateParams(ok)

        // Assert
        assertThat(errors).isEmpty()
    }

    @Test
    @BoundaryValues
    fun `validateParams - min equal to max is allowed`() {
        // Arrange
        val degenerate = mapOf<String, Any>("min" to 3.0, "max" to 3.0)

        // Act
        val errors = sut.validateParams(degenerate)

        // Assert
        assertThat(errors).isEmpty()
    }

    @Test
    @EquivalencePartitioning
    fun `validateParams - neither min nor max is rejected`() {
        // Arrange
        val noBounds = mapOf<String, Any>("minExclusive" to true)

        // Act
        val errors = sut.validateParams(noBounds)

        // Assert
        assertThat(errors).containsExactly("RANGE requires at least one of 'min' or 'max'")
    }
}
