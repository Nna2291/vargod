package com.vargod.repository.jdbc

import com.vargod.domain.PersistenceException
import com.vargod.repository.jdbc.support.DbTestBase
import com.vargod.testing.EquivalencePartitioning
import com.vargod.testing.ErrorGuessing
import com.vargod.testing.builders.aModelTemplate
import com.vargod.testing.builders.anExperiment
import com.vargod.testing.mothers.ModelTemplateMother
import io.qameta.allure.Feature
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.UUID

@Feature("ModelTemplateRepository")
@DisplayName("JdbcModelTemplateRepository — classical style (embedded PostgreSQL)")
class JdbcModelTemplateRepositoryTest : DbTestBase() {

    private lateinit var sut: JdbcModelTemplateRepository

    @BeforeEach
    fun setUp() {
        sut = JdbcModelTemplateRepository(dataSource, codec)
    }

    @Test
    @EquivalencePartitioning
    fun `findAll - returns templates ordered by code, each with its own validators`() {
        // Arrange
        val sir = givenModel(ModelTemplateMother.sir())
        val logistic = givenModel(ModelTemplateMother.logistic())

        // Act
        val result = sut.findAll()

        // Assert
        assertThat(result.map { it.code }).containsExactly("LOGISTIC", "SIR")
        assertThat(result.first { it.code == "SIR" }.validators).containsExactlyInAnyOrderElementsOf(sir.validators)
        assertThat(result.first { it.code == "LOGISTIC" }.validators).hasSameSizeAs(logistic.validators)
    }

    @Test
    @EquivalencePartitioning
    fun `findAll - empty catalog gives empty list`() {
        // Arrange
        val empty = rowCount("model_templates")

        // Act
        val result = sut.findAll()

        // Assert
        assertThat(empty).isZero()
        assertThat(result).isEmpty()
    }

    @Test
    @EquivalencePartitioning
    fun `findById - returns template with solver hints`() {
        // Arrange
        val hh = sut.save(ModelTemplateMother.hodgkinHuxley())

        // Act
        val result = sut.findById(hh.id)

        // Assert
        assertThat(result).isEqualTo(hh)
    }

    @Test
    @EquivalencePartitioning
    fun `findById - unknown id gives null`() {
        // Arrange
        givenModel(ModelTemplateMother.sir())

        // Act
        val result = sut.findById(UUID.randomUUID())

        // Assert
        assertThat(result).isNull()
    }

    @Test
    @EquivalencePartitioning
    fun `save - existing template metadata is updated, validators are kept`() {
        // Arrange
        val sir = givenModel(ModelTemplateMother.sir())
        val renamed = sir.copy(name = "SIR (renamed)", validators = emptyList())

        // Act
        sut.save(renamed)

        // Assert
        val stored = sut.findById(sir.id)!!
        assertThat(stored.name).isEqualTo("SIR (renamed)")
        assertThat(stored.validators).hasSize(sir.validators.size)
    }

    @Test
    @ErrorGuessing
    fun `save - second template with an existing code violates unique constraint`() {
        // Arrange
        givenModel(aModelTemplate().withCode("SIR").build())
        val clash = aModelTemplate().withCode("SIR").build()

        // Act
        val error = assertThrows<PersistenceException> { sut.save(clash) }

        // Assert
        assertThat(error.message).startsWith("Failed to save model template")
    }

    @Test
    @EquivalencePartitioning
    fun `deleteById - removes template and cascades its validators`() {
        // Arrange
        val sir = givenModel(ModelTemplateMother.sir())

        // Act
        sut.deleteById(sir.id)

        // Assert
        assertThat(sut.findById(sir.id)).isNull()
        assertThat(rowCount("validators")).isZero()
    }

    @Test
    @ErrorGuessing
    fun `deleteById - template used by an experiment cannot be deleted`() {
        // Arrange
        val sir = givenModel(ModelTemplateMother.sir())
        val owner = givenUser()
        givenExperimentRow(anExperiment().ownedBy(owner.id).forModel(sir.id).build())

        // Act
        val error = assertThrows<PersistenceException> { sut.deleteById(sir.id) }

        // Assert
        assertThat(error.message).startsWith("Failed to delete model template")
        assertThat(sut.findById(sir.id)).isNotNull
    }
}
