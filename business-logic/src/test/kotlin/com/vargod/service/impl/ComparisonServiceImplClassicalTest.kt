package com.vargod.service.impl

import com.vargod.domain.AuthorizationException
import com.vargod.domain.NotFoundException
import com.vargod.testing.BoundaryValues
import com.vargod.testing.Classical
import com.vargod.testing.DecisionTable
import com.vargod.testing.EquivalencePartitioning
import com.vargod.testing.TestTime
import com.vargod.testing.builders.aComparison
import com.vargod.testing.builders.anExperiment
import com.vargod.testing.inmemory.InMemoryComparisonRepository
import com.vargod.testing.inmemory.InMemoryExperimentRepository
import io.qameta.allure.Epic
import io.qameta.allure.Feature
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import java.util.UUID

@Classical
@Epic("Business logic")
@Feature("ComparisonService")
@DisplayName("ComparisonServiceImpl — classical style (in-memory repositories)")
class ComparisonServiceImplClassicalTest {

    private lateinit var experiments: InMemoryExperimentRepository
    private lateinit var comparisons: InMemoryComparisonRepository
    private lateinit var sut: ComparisonServiceImpl

    private val owner = UUID.randomUUID()
    private val stranger = UUID.randomUUID()
    private val modelId = UUID.randomUUID()

    @BeforeEach
    fun setUp() {
        experiments = InMemoryExperimentRepository()
        comparisons = InMemoryComparisonRepository()
        sut = ComparisonServiceImpl(comparisons, experiments, TestTime.FIXED_CLOCK)
    }

    private fun ownExperiment() = experiments.save(anExperiment().ownedBy(owner).forModel(modelId).build())

    // ---------------------------------------------------------------- create

    @Test
    @BoundaryValues
    fun `create - exactly two experiments is the minimal valid comparison`() {
        // Arrange
        val e1 = ownExperiment()
        val e2 = ownExperiment()

        // Act
        val created = sut.create(listOf(e1.id, e2.id), owner)

        // Assert
        assertThat(comparisons.findById(created.id)).isEqualTo(created)
        assertThat(created.experiments.map { it.id }).containsExactlyInAnyOrder(e1.id, e2.id)
        assertThat(created.createdAt).isEqualTo(TestTime.NOW)
    }

    @Test
    @BoundaryValues
    fun `create - empty id list is rejected and nothing is stored`() {
        // Arrange
        val noIds = emptyList<UUID>()

        // Act
        val error = assertThrows<IllegalArgumentException> { sut.create(noIds, owner) }

        // Assert
        assertThat(error.message).contains("got 0")
        assertThat(comparisons.count()).isZero()
    }

    /**
     * Таблица решений «доступ к чужому эксперименту»:
     *
     * | условие / правило          | R1 | R2 | R3 | R4 |
     * |----------------------------|----|----|----|----|
     * | второй эксперимент — свой  | Y  | Y  | N  | N  |
     * | второй эксперимент публичен| Y  | N  | Y  | N  |
     * | действие: сравнение создано| Y  | Y  | Y  | N  |
     */
    @ParameterizedTest(name = "R{index}: own={0}, public={1} -> allowed={2}")
    @CsvSource(
        "true,  true,  true",
        "true,  false, true",
        "false, true,  true",
        "false, false, false",
    )
    @DecisionTable
    fun `create - access to the second experiment follows ownership and publicity rule`(
        own: Boolean, isPublic: Boolean, allowed: Boolean,
    ) {
        // Arrange
        val first = ownExperiment()
        val second = experiments.save(
            anExperiment().ownedBy(if (own) owner else stranger).forModel(modelId).apply { if (isPublic) asPublic() else asPrivate() }.build(),
        )
        val ids = listOf(first.id, second.id)

        // Act
        val outcome = runCatching { sut.create(ids, owner) }

        // Assert
        if (allowed) {
            assertThat(outcome.isSuccess).isTrue()
            assertThat(comparisons.count()).isEqualTo(1)
        } else {
            assertThat(outcome.exceptionOrNull()).isInstanceOf(AuthorizationException::class.java)
            assertThat(comparisons.count()).isZero()
        }
    }

    // ---------------------------------------------------------------- getById

    // ---------------------------------------------------------------- listByUser

}
