package com.vargod.repository.jdbc

import com.vargod.domain.PersistenceException
import com.vargod.repository.jdbc.support.DbTestBase
import com.vargod.testing.EquivalencePartitioning
import com.vargod.testing.ErrorGuessing
import com.vargod.testing.builders.aModelTemplate
import com.vargod.testing.builders.aValidator
import com.vargod.testing.mothers.ValidatorMother
import io.qameta.allure.Feature
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.UUID

@Feature("ValidatorRepository")
@DisplayName("JdbcValidatorRepository — classical style (embedded PostgreSQL)")
class JdbcValidatorRepositoryTest : DbTestBase() {

    private lateinit var sut: JdbcValidatorRepository

    private val model = aModelTemplate().withCode("SIR").build()
    private val otherModel = aModelTemplate().withCode("LOGISTIC").build()

    @BeforeEach
    fun setUp() {
        sut = JdbcValidatorRepository(dataSource, codec)
        givenModel(model)
        givenModel(otherModel)
    }

    @Test
    @EquivalencePartitioning
    fun `findAll - with model id returns only that model's validators`() {
        // Arrange
        val mine = givenValidator(ValidatorMother.sirPopulationConservation(), model.id)
        givenValidator(ValidatorMother.positive("K"), otherModel.id)

        // Act
        val result = sut.findAll(model.id)

        // Assert
        assertThat(result).containsExactly(mine)
    }

    @Test
    @EquivalencePartitioning
    fun `findAll - without model id returns validators of all models`() {
        // Arrange
        givenValidator(ValidatorMother.positive("N"), model.id)
        givenValidator(ValidatorMother.positive("K"), otherModel.id)

        // Act
        val result = sut.findAll(null)

        // Assert
        assertThat(result).hasSize(2)
    }

    @Test
    @EquivalencePartitioning
    fun `findById - JSON rules are restored with their types`() {
        // Arrange
        val ordered = givenValidator(ValidatorMother.ascending("t0", "t1"), model.id)

        // Act
        val result = sut.findById(ordered.id)

        // Assert
        assertThat(result).isEqualTo(ordered)
        assertThat(result!!.rules["fields"]).isEqualTo(listOf("t0", "t1"))
    }

    @Test
    @EquivalencePartitioning
    fun `findById - unknown id gives null`() {
        // Arrange
        givenValidator(ValidatorMother.positive("N"), model.id)

        // Act
        val result = sut.findById(UUID.randomUUID())

        // Assert
        assertThat(result).isNull()
    }

    @Test
    @EquivalencePartitioning
    fun `save - stored validator is bound to the model`() {
        // Arrange
        val validator = ValidatorMother.fraction("gamma")

        // Act
        sut.save(validator, model.id, createdBy = null)

        // Assert
        assertThat(sut.findAll(model.id)).containsExactly(validator)
    }

    @Test
    @ErrorGuessing
    fun `save - unknown model violates foreign key`() {
        // Arrange
        val validator = ValidatorMother.fraction("gamma")

        // Act
        val error = assertThrows<PersistenceException> { sut.save(validator, UUID.randomUUID(), createdBy = null) }

        // Assert
        assertThat(error.message).startsWith("JdbcValidatorRepository.save failed")
    }

    @Test
    @EquivalencePartitioning
    fun `update - name and rules are replaced`() {
        // Arrange
        val original = givenValidator(ValidatorMother.fraction("gamma"), model.id)
        val changed = aValidator().withId(original.id).named("gamma <= 0.5").range("gamma", min = 0.0, max = 0.5).build()

        // Act
        sut.update(changed)

        // Assert
        assertThat(sut.findById(original.id)).isEqualTo(changed)
    }

    @Test
    @ErrorGuessing
    fun `update - unknown validator gives PersistenceException`() {
        // Arrange
        val ghost = ValidatorMother.positive("P")

        // Act
        val error = assertThrows<PersistenceException> { sut.update(ghost) }

        // Assert
        assertThat(error.message).contains("Validator not found for update")
    }

    @Test
    @EquivalencePartitioning
    fun `deleteById - removes the validator`() {
        // Arrange
        val validator = givenValidator(ValidatorMother.positive("N"), model.id)

        // Act
        sut.deleteById(validator.id)

        // Assert
        assertThat(sut.findById(validator.id)).isNull()
    }

    @Test
    @ErrorGuessing
    fun `deleteById - unknown id leaves other validators intact`() {
        // Arrange
        givenValidator(ValidatorMother.positive("N"), model.id)

        // Act
        sut.deleteById(UUID.randomUUID())

        // Assert
        assertThat(rowCount("validators")).isEqualTo(1)
    }
}
