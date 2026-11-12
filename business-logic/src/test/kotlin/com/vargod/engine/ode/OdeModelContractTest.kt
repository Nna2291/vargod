package com.vargod.engine.ode

import com.vargod.engine.OdeModel
import com.vargod.support.RealEngines
import com.vargod.testing.BoundaryValues
import com.vargod.testing.EquivalencePartitioning
import com.vargod.testing.ErrorGuessing
import io.qameta.allure.Epic
import io.qameta.allure.Feature
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

/**
 * Общий контракт интерфейса [OdeModel] для всех семи моделей.
 *
 * Наследник задаёт ожидаемые код/переменные/параметры и корректную точку
 * (state + params); базовый класс проверяет на ней все четыре публичных метода —
 * по позитивному и негативному тесту на каждый. Проверки, специфичные для
 * модели (известные значения производных, законы сохранения), — в наследнике.
 */
@Epic("Business logic")
@Feature("ODE models")
abstract class OdeModelContractTest {

    protected abstract fun createModel(): OdeModel
    protected abstract val expectedCode: String
    protected abstract val expectedVariables: List<String>
    protected abstract val expectedParameters: List<String>

    /** Корректные значения параметров модели. */
    protected abstract fun validParams(): Map<String, Double>

    /** Корректное состояние (значения переменных в порядке [OdeModel.variables]). */
    protected abstract fun validState(): DoubleArray

    protected lateinit var sut: OdeModel

    @BeforeEach
    fun createSut() {
        sut = createModel()
    }

    @Test
    @EquivalencePartitioning
    fun `code - equals the documented model code`() {
        // Arrange
        val expected = expectedCode

        // Act
        val code = sut.code()

        // Assert
        assertThat(code).isEqualTo(expected)
    }

    @Test
    @ErrorGuessing
    fun `code - does not collide with codes of other models`() {
        // Arrange
        val otherCodes = RealEngines.allOdeModels().filterNot { it::class == sut::class }.map { it.code() }

        // Act
        val code = sut.code()

        // Assert
        assertThat(otherCodes).doesNotContain(code)
    }

    @Test
    @EquivalencePartitioning
    fun `variables - are declared in solver order`() {
        // Arrange
        val expected = expectedVariables

        // Act
        val variables = sut.variables()

        // Assert
        assertThat(variables).containsExactlyElementsOf(expected)
    }

    /**
     * Начальные значения и параметры лежат в одной Map (ParameterSet.values),
     * поэтому совпадение имени переменной и параметра — скрытая ошибка.
     */
    @Test
    @ErrorGuessing
    fun `variables - share no names with parameters`() {
        // Arrange
        val parameters = sut.parameters()

        // Act
        val variables = sut.variables()

        // Assert
        assertThat(variables).doesNotContainAnyElementsOf(parameters)
    }

    @Test
    @EquivalencePartitioning
    fun `parameters - are declared as documented`() {
        // Arrange
        val expected = expectedParameters

        // Act
        val parameters = sut.parameters()

        // Assert
        assertThat(parameters).containsExactlyElementsOf(expected)
    }

    @Test
    @EquivalencePartitioning
    fun `parameters - the last declared parameter is required by derivatives`() {
        // Arrange
        val missing = expectedParameters.last()
        val incomplete = validParams() - missing

        // Act
        val error = assertThrows<IllegalArgumentException> { sut.derivatives(validState(), incomplete, 0.0) }

        // Assert
        assertThat(error.message).contains("Missing parameter '$missing'")
    }

    @Test
    @EquivalencePartitioning
    fun `derivatives - valid point gives one finite derivative per variable`() {
        // Arrange
        val state = validState()

        // Act
        val derivatives = sut.derivatives(state, validParams(), 0.0)

        // Assert
        assertThat(derivatives).hasSize(expectedVariables.size)
        assertThat(derivatives.toList()).allMatch { it.isFinite() }
    }

    @Test
    @BoundaryValues
    fun `derivatives - state one element shorter is rejected`() {
        // Arrange
        val shorter = validState().copyOf(expectedVariables.size - 1)

        // Act
        val error = assertThrows<IllegalArgumentException> { sut.derivatives(shorter, validParams(), 0.0) }

        // Assert
        assertThat(error.message).contains("expects ${expectedVariables.size} state variable")
    }
}
