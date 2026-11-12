package com.vargod.service.impl

import com.vargod.domain.AuthorizationException
import com.vargod.domain.Comparison
import com.vargod.domain.NotFoundException
import com.vargod.domain.repository.ComparisonRepository
import com.vargod.domain.repository.ExperimentRepository
import com.vargod.testing.BoundaryValues
import com.vargod.testing.DecisionTable
import com.vargod.testing.EquivalencePartitioning
import com.vargod.testing.ErrorGuessing
import com.vargod.testing.London
import com.vargod.testing.TestTime
import com.vargod.testing.builders.aComparison
import com.vargod.testing.builders.anExperiment
import com.vargod.testing.mothers.ExperimentMother
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.slot
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

@London
@Epic("Business logic")
@Feature("ComparisonService")
@DisplayName("ComparisonServiceImpl — London style (MockK)")
@ExtendWith(MockKExtension::class)
class ComparisonServiceImplLondonTest {

    @MockK private lateinit var comparisonRepository: ComparisonRepository
    @MockK private lateinit var experimentRepository: ExperimentRepository

    private lateinit var sut: ComparisonServiceImpl

    private val owner = UUID.randomUUID()
    private val stranger = UUID.randomUUID()
    private val modelId = UUID.randomUUID()

    @BeforeEach
    fun setUp() {
        sut = ComparisonServiceImpl(comparisonRepository, experimentRepository, TestTime.FIXED_CLOCK)
    }

    // ---------------------------------------------------------------- create

    @Test
    @EquivalencePartitioning
    fun `create - own experiments of one model are loaded once and saved as comparison`() {
        // Arrange
        val e1 = ExperimentMother.privateOf(owner, modelId)
        val e2 = ExperimentMother.privateOf(owner, modelId)
        val ids = listOf(e1.id, e2.id)
        val saved = slot<Comparison>()
        every { experimentRepository.findAllByIds(ids) } returns listOf(e1, e2)
        every { comparisonRepository.save(capture(saved)) } answers { firstArg() }

        // Act
        val result = sut.create(ids, owner)

        // Assert
        assertThat(result).isEqualTo(saved.captured)
        assertThat(result.userId).isEqualTo(owner)
        assertThat(result.experiments).containsExactly(e1, e2)
        assertThat(result.createdAt).isEqualTo(TestTime.NOW)
        verify(exactly = 1) { experimentRepository.findAllByIds(ids) }
    }

    @Test
    @DecisionTable
    fun `create - private experiment of another user gives AuthorizationException`() {
        // Arrange
        val mine = ExperimentMother.privateOf(owner, modelId)
        val theirsPrivate = ExperimentMother.privateOf(stranger, modelId)
        every { experimentRepository.findAllByIds(any()) } returns listOf(mine, theirsPrivate)

        // Act
        val error = assertThrows<AuthorizationException> { sut.create(listOf(mine.id, theirsPrivate.id), owner) }

        // Assert
        assertThat(error.message).contains(theirsPrivate.id.toString())
        verify(exactly = 0) { comparisonRepository.save(any()) }
    }

    @Test
    @BoundaryValues
    fun `create - single experiment is below the minimum of two`() {
        // Arrange
        val ids = listOf(UUID.randomUUID())

        // Act
        val error = assertThrows<IllegalArgumentException> { sut.create(ids, owner) }

        // Assert
        assertThat(error.message).contains("at least 2")
        verify(exactly = 0) { experimentRepository.findAllByIds(any()) }
    }

    @Test
    @ErrorGuessing
    fun `create - duplicated experiment id is rejected before loading`() {
        // Arrange
        val id = UUID.randomUUID()

        // Act
        val error = assertThrows<IllegalArgumentException> { sut.create(listOf(id, id), owner) }

        // Assert
        assertThat(error.message).contains("Duplicate")
        verify(exactly = 0) { experimentRepository.findAllByIds(any()) }
    }

    @Test
    @EquivalencePartitioning
    fun `create - missing experiment gives NotFoundException with its id`() {
        // Arrange
        val existing = ExperimentMother.privateOf(owner, modelId)
        val missingId = UUID.randomUUID()
        every { experimentRepository.findAllByIds(listOf(existing.id, missingId)) } returns listOf(existing)

        // Act
        val error = assertThrows<NotFoundException> { sut.create(listOf(existing.id, missingId), owner) }

        // Assert
        assertThat(error.message).isEqualTo("Experiment not found: id=$missingId")
        verify(exactly = 0) { comparisonRepository.save(any()) }
    }

    @Test
    @EquivalencePartitioning
    fun `create - experiments of different models cannot be compared`() {
        // Arrange
        val sir = anExperiment().ownedBy(owner).forModel(UUID.randomUUID()).build()
        val logistic = anExperiment().ownedBy(owner).forModel(UUID.randomUUID()).build()
        every { experimentRepository.findAllByIds(any()) } returns listOf(sir, logistic)

        // Act
        val error = assertThrows<IllegalArgumentException> { sut.create(listOf(sir.id, logistic.id), owner) }

        // Assert
        assertThat(error.message).contains("same model")
        verify(exactly = 0) { comparisonRepository.save(any()) }
    }

    // ---------------------------------------------------------------- getById

    @Test
    @EquivalencePartitioning
    fun `getById - returns comparison found by repository`() {
        // Arrange
        val comparison = aComparison().ownedBy(owner).build()
        every { comparisonRepository.findById(comparison.id) } returns comparison

        // Act
        val result = sut.getById(comparison.id)

        // Assert
        assertThat(result).isSameAs(comparison)
    }

    @Test
    @EquivalencePartitioning
    fun `getById - missing comparison gives NotFoundException`() {
        // Arrange
        val id = UUID.randomUUID()
        every { comparisonRepository.findById(id) } returns null

        // Act
        val error = assertThrows<NotFoundException> { sut.getById(id) }

        // Assert
        assertThat(error.message).isEqualTo("Comparison not found: id=$id")
    }

    // ---------------------------------------------------------------- listByUser

    @Test
    @EquivalencePartitioning
    fun `listByUser - delegates to repository`() {
        // Arrange
        val comparisons = listOf(aComparison().ownedBy(owner).build())
        every { comparisonRepository.findAllByUserId(owner) } returns comparisons

        // Act
        val result = sut.listByUser(owner)

        // Assert
        assertThat(result).isEqualTo(comparisons)
        verify(exactly = 1) { comparisonRepository.findAllByUserId(owner) }
    }

    @Test
    @BoundaryValues
    fun `listByUser - user without comparisons gets empty list`() {
        // Arrange
        every { comparisonRepository.findAllByUserId(stranger) } returns emptyList()

        // Act
        val result = sut.listByUser(stranger)

        // Assert
        assertThat(result).isEmpty()
    }
}
