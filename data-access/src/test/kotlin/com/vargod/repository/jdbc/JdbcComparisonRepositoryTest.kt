package com.vargod.repository.jdbc

import com.vargod.repository.jdbc.support.DbTestBase
import com.vargod.testing.BoundaryValues
import com.vargod.testing.EquivalencePartitioning
import com.vargod.testing.builders.aComparison
import com.vargod.testing.builders.aModelTemplate
import com.vargod.testing.builders.anExperiment
import io.qameta.allure.Feature
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.UUID

@Feature("ComparisonRepository")
@DisplayName("JdbcComparisonRepository — classical style (embedded PostgreSQL)")
class JdbcComparisonRepositoryTest : DbTestBase() {

    private lateinit var experiments: JdbcExperimentRepository
    private lateinit var sut: JdbcComparisonRepository

    private val owner = UUID.randomUUID()
    private val model = aModelTemplate().build()

    @BeforeEach
    fun setUp() {
        experiments = JdbcExperimentRepository(dataSource, codec)
        sut = JdbcComparisonRepository(dataSource, experiments)
        givenUserWithId(owner)
        givenModel(model)
    }

    private fun storedExperiment() = experiments.save(anExperiment().ownedBy(owner).forModel(model.id).build())

    @Test
    @EquivalencePartitioning
    fun `save - findById returns experiments in the order they were given`() {
        // Arrange
        val (e1, e2, e3) = List(3) { storedExperiment() }
        val comparison = aComparison().ownedBy(owner).of(e3, e1, e2).build()

        // Act
        sut.save(comparison)

        // Assert
        assertThat(sut.findById(comparison.id)).isEqualTo(comparison)
    }

    @Test
    @BoundaryValues
    fun `save - comparison without experiments is rejected`() {
        // Arrange
        val empty = aComparison().ownedBy(owner).of(emptyList()).build()

        // Act
        val error = assertThrows<IllegalArgumentException> { sut.save(empty) }

        // Assert
        assertThat(error.message).contains("at least one experiment")
        assertThat(rowCount("comparisons")).isZero()
    }

    @Test
    @EquivalencePartitioning
    fun `findById - unknown id gives null`() {
        // Arrange
        sut.save(aComparison().ownedBy(owner).of(storedExperiment(), storedExperiment()).build())

        // Act
        val result = sut.findById(UUID.randomUUID())

        // Assert
        assertThat(result).isNull()
    }

    @Test
    @EquivalencePartitioning
    fun `findById - re-saving with fewer experiments replaces the list`() {
        // Arrange
        val (e1, e2, e3) = List(3) { storedExperiment() }
        val comparison = sut.save(aComparison().ownedBy(owner).of(e1, e2, e3).build())
        sut.save(comparison.copy(experiments = listOf(e2, e3)))

        // Act
        val result = sut.findById(comparison.id)

        // Assert
        assertThat(result!!.experiments.map { it.id }).containsExactly(e2.id, e3.id)
        assertThat(rowCount("comparison_experiments")).isEqualTo(2)
    }

    @Test
    @EquivalencePartitioning
    fun `findAllByUserId - returns only comparisons of that user`() {
        // Arrange
        val stranger = givenUser()
        val mine = sut.save(aComparison().ownedBy(owner).of(storedExperiment(), storedExperiment()).build())
        sut.save(aComparison().ownedBy(stranger.id).of(storedExperiment(), storedExperiment()).build())

        // Act
        val result = sut.findAllByUserId(owner)

        // Assert
        assertThat(result).containsExactly(mine)
    }

    @Test
    @BoundaryValues
    fun `findAllByUserId - user without comparisons gets empty list`() {
        // Arrange
        sut.save(aComparison().ownedBy(owner).of(storedExperiment(), storedExperiment()).build())

        // Act
        val result = sut.findAllByUserId(UUID.randomUUID())

        // Assert
        assertThat(result).isEmpty()
    }
}
