package com.vargod.service.impl

import com.vargod.domain.NotFoundException
import com.vargod.domain.PersistenceException
import com.vargod.domain.repository.ModelTemplateRepository
import com.vargod.testing.BoundaryValues
import com.vargod.testing.Classical
import com.vargod.testing.EquivalencePartitioning
import com.vargod.testing.ErrorGuessing
import com.vargod.testing.London
import com.vargod.testing.inmemory.InMemoryModelTemplateRepository
import com.vargod.testing.mothers.ModelTemplateMother
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import io.qameta.allure.Epic
import io.qameta.allure.Feature
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import java.util.UUID

/*
 * Один и тот же набор сценариев ModelServiceImpl в двух вариантах — для сравнения
 * (требование 3 ЛР1): лондонский проверяет обращение к репозиторию, классический —
 * только наблюдаемый результат на реальных данных in-memory репозитория.
 */

@London
@Epic("Business logic")
@Feature("ModelService")
@DisplayName("ModelServiceImpl — London style (MockK)")
@ExtendWith(MockKExtension::class)
class ModelServiceImplLondonTest {

    @MockK private lateinit var modelRepository: ModelTemplateRepository

    private lateinit var sut: ModelServiceImpl

    @BeforeEach
    fun setUp() {
        sut = ModelServiceImpl(modelRepository)
    }

    @Test
    @EquivalencePartitioning
    fun `listAll - returns catalog from repository`() {
        // Arrange
        val catalog = listOf(ModelTemplateMother.logistic(), ModelTemplateMother.sir())
        every { modelRepository.findAll() } returns catalog

        // Act
        val result = sut.listAll()

        // Assert
        assertThat(result).isEqualTo(catalog)
        verify(exactly = 1) { modelRepository.findAll() }
    }

    @Test
    @ErrorGuessing
    fun `listAll - repository failure is propagated`() {
        // Arrange
        every { modelRepository.findAll() } throws PersistenceException("Failed to list model templates")

        // Act
        val error = assertThrows<PersistenceException> { sut.listAll() }

        // Assert
        assertThat(error.message).contains("model templates")
    }

    @Test
    @EquivalencePartitioning
    fun `getById - returns model found by repository`() {
        // Arrange
        val model = ModelTemplateMother.sir()
        every { modelRepository.findById(model.id) } returns model

        // Act
        val result = sut.getById(model.id)

        // Assert
        assertThat(result).isSameAs(model)
    }

    @Test
    @EquivalencePartitioning
    fun `getById - missing model gives NotFoundException`() {
        // Arrange
        val id = UUID.randomUUID()
        every { modelRepository.findById(id) } returns null

        // Act
        val error = assertThrows<NotFoundException> { sut.getById(id) }

        // Assert
        assertThat(error.message).isEqualTo("ModelTemplate not found: id=$id")
    }
}

@Classical
@Epic("Business logic")
@Feature("ModelService")
@DisplayName("ModelServiceImpl — classical style (in-memory repository)")
class ModelServiceImplClassicalTest {

    private val logistic = ModelTemplateMother.logistic()
    private val sir = ModelTemplateMother.sir()

    private lateinit var sut: ModelServiceImpl

    @BeforeEach
    fun setUp() {
        sut = ModelServiceImpl(InMemoryModelTemplateRepository(sir, logistic))
    }

    @Test
    @EquivalencePartitioning
    fun `listAll - returns catalog ordered by model code`() {
        // Arrange
        val expectedOrder = listOf("LOGISTIC", "SIR")

        // Act
        val result = sut.listAll()

        // Assert
        assertThat(result.map { it.code }).containsExactlyElementsOf(expectedOrder)
    }

    @Test
    @BoundaryValues
    fun `listAll - empty catalog gives empty list`() {
        // Arrange
        val emptyCatalog = ModelServiceImpl(InMemoryModelTemplateRepository())

        // Act
        val result = emptyCatalog.listAll()

        // Assert
        assertThat(result).isEmpty()
    }

    @Test
    @EquivalencePartitioning
    fun `getById - returns model with its validators`() {
        // Arrange
        val id = sir.id

        // Act
        val result = sut.getById(id)

        // Assert
        assertThat(result).isEqualTo(sir)
        assertThat(result.validators).hasSize(2)
    }

    @Test
    @EquivalencePartitioning
    fun `getById - unknown id gives NotFoundException`() {
        // Arrange
        val id = UUID.randomUUID()

        // Act
        val error = assertThrows<NotFoundException> { sut.getById(id) }

        // Assert
        assertThat(error.message).contains(id.toString())
    }
}
