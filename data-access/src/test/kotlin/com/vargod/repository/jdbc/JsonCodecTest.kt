package com.vargod.repository.jdbc

import com.vargod.domain.PersistenceException
import com.vargod.testing.Classical
import com.vargod.testing.EquivalencePartitioning
import com.vargod.testing.ErrorGuessing
import com.vargod.testing.mothers.ValidatorMother
import io.qameta.allure.Epic
import io.qameta.allure.Feature
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

@Classical
@Epic("Data access")
@Feature("JSON mapping")
@DisplayName("JsonCodec")
class JsonCodecTest {

    private lateinit var sut: JsonCodec

    @BeforeEach
    fun setUp() {
        sut = JsonCodec()
    }

    @Test
    @EquivalencePartitioning
    fun `encode - map is written as compact JSON`() {
        // Arrange
        val value = mapOf("beta" to 0.3, "N" to 1000)

        // Act
        val json = sut.encode(value)

        // Assert
        assertThat(json).isEqualTo("""{"beta":0.3,"N":1000}""")
    }

    @Test
    @ErrorGuessing
    fun `encode - object without serializable properties gives PersistenceException`() {
        // Arrange
        val notSerializable = Any()

        // Act
        val error = assertThrows<PersistenceException> { sut.encode(notSerializable) }

        // Assert
        assertThat(error.message).startsWith("Failed to encode JSON")
    }

    @Test
    @EquivalencePartitioning
    fun `decodeDoubleMap - integer values are converted to doubles`() {
        // Arrange
        val json = """{"S":999,"beta":0.3}"""

        // Act
        val values = sut.decodeDoubleMap(json)

        // Assert
        assertThat(values).isEqualTo(mapOf("S" to 999.0, "beta" to 0.3))
    }

    @Test
    @ErrorGuessing
    fun `decodeDoubleMap - truncated JSON gives PersistenceException`() {
        // Arrange
        val broken = """{"S":999,"""

        // Act
        val error = assertThrows<PersistenceException> { sut.decodeDoubleMap(broken) }

        // Assert
        assertThat(error.message).startsWith("Failed to decode Map<String, Double>")
    }

    @Test
    @EquivalencePartitioning
    fun `decodeValidators - snapshot written by encodeValidators is restored`() {
        // Arrange
        val snapshot = listOf(ValidatorMother.positive("P"), ValidatorMother.ascending("t0", "t1"))
        val json = sut.encodeValidators(snapshot)

        // Act
        val restored = sut.decodeValidators(json)

        // Assert
        assertThat(restored).isEqualTo(snapshot)
    }

    @Test
    @ErrorGuessing
    fun `decodeValidators - unknown enum value gives PersistenceException`() {
        // Arrange
        val json = """[{"id":"00000000-0000-0000-0000-000000000001","name":"v","checkType":"REGEX",""" +
            """"targetKind":"PARAMETER","targetField":"P","rules":{}}]"""

        // Act
        val error = assertThrows<PersistenceException> { sut.decodeValidators(json) }

        // Assert
        assertThat(error.message).startsWith("Failed to decode validator snapshot")
    }

    @Test
    @EquivalencePartitioning
    fun `decodeSimulationData - data written by encodeSimulationData is restored`() {
        // Arrange
        val times = listOf(0.0, 0.5, 1.0)
        val series = mapOf("x" to listOf(1.0, 2.0, 3.0))
        val json = sut.encodeSimulationData(times, series)

        // Act
        val (restoredTimes, restoredSeries) = sut.decodeSimulationData(json)

        // Assert
        assertThat(restoredTimes).isEqualTo(times)
        assertThat(restoredSeries).isEqualTo(series)
    }

    @Test
    @ErrorGuessing
    fun `decodeSimulationData - JSON without timePoints gives PersistenceException`() {
        // Arrange
        val json = """{"variables":{"x":[1.0]}}"""

        // Act
        val error = assertThrows<PersistenceException> { sut.decodeSimulationData(json) }

        // Assert
        assertThat(error.message).startsWith("Failed to decode simulation data")
    }
}
