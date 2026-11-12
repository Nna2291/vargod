package com.vargod.domain

import com.vargod.testing.Classical
import com.vargod.testing.EquivalencePartitioning
import io.qameta.allure.Epic
import io.qameta.allure.Feature
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.util.UUID

@Classical
@Epic("Domain")
@Feature("Exceptions")
@DisplayName("Domain exceptions")
class DomainExceptionsTest {

    @Test
    @EquivalencePartitioning
    fun `NotFoundException - message names entity and id`() {
        // Arrange
        val id = UUID.randomUUID()

        // Act
        val error = NotFoundException("Experiment", id)

        // Assert
        assertThat(error.message).isEqualTo("Experiment not found: id=$id")
    }

    @Test
    @EquivalencePartitioning
    fun `ValidationException - message lists only failed validators`() {
        // Arrange
        val results = listOf(
            ValidationResult(ValidationStatus.PASS, "ignored", UUID.randomUUID()),
            ValidationResult(ValidationStatus.FAIL, "P must be > 0", UUID.randomUUID()),
        )

        // Act
        val error = ValidationException(results)

        // Assert
        assertThat(error.message).isEqualTo("Validation failed: P must be > 0")
        assertThat(error.results).isEqualTo(results)
    }

    @Test
    @EquivalencePartitioning
    fun `PersistenceException - keeps the original cause`() {
        // Arrange
        val cause = IllegalStateException("db down")

        // Act
        val error = PersistenceException("save failed", cause)

        // Assert
        assertThat(error.cause).isSameAs(cause)
    }
}
