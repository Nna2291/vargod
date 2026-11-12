package com.vargod.engine.ode

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
@Feature("ODE models")
@DisplayName("MapModelRegistry")
class MapModelRegistryTest {

    @Test
    @EquivalencePartitioning
    fun `getByCode - returns the registered model`() {
        // Arrange
        val sir = SirModel()
        val sut = MapModelRegistry(listOf(LogisticGrowthModel(), sir))

        // Act
        val result = sut.getByCode("SIR")

        // Assert
        assertThat(result).isSameAs(sir)
    }

    @Test
    @EquivalencePartitioning
    fun `getByCode - unknown code gives IllegalArgumentException`() {
        // Arrange
        val sut = RealEngines.modelRegistry()

        // Act
        val error = assertThrows<IllegalArgumentException> { sut.getByCode("SIRS") }

        // Assert
        assertThat(error.message).isEqualTo("Unknown ODE model code: SIRS")
    }

    @Test
    @EquivalencePartitioning
    fun `listAll - contains all seven production models keyed by code`() {
        // Arrange
        val sut = RealEngines.modelRegistry()

        // Act
        val all = sut.listAll()

        // Assert
        assertThat(all).hasSize(7)
        assertThat(all).allSatisfy { code, model -> assertThat(model.code()).isEqualTo(code) }
    }

    @Test
    @BoundaryValues
    fun `listAll - empty registry is empty`() {
        // Arrange
        val sut = MapModelRegistry(emptyList())

        // Act
        val all = sut.listAll()

        // Assert
        assertThat(all).isEmpty()
    }

    @Test
    @ErrorGuessing
    fun `constructor - two models with the same code are rejected`() {
        // Arrange
        val duplicates = listOf(SirModel(), SirModel())

        // Act
        val error = assertThrows<IllegalArgumentException> { MapModelRegistry(duplicates) }

        // Assert
        assertThat(error.message).isEqualTo("Duplicate ODE model code: SIR")
    }
}
