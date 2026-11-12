package com.vargod.repository.jdbc

import com.vargod.domain.ExperimentStatus
import com.vargod.domain.PersistenceException
import com.vargod.repository.jdbc.support.DbTestBase
import com.vargod.testing.BoundaryValues
import com.vargod.testing.EquivalencePartitioning
import com.vargod.testing.ErrorGuessing
import com.vargod.testing.StateTransition
import com.vargod.testing.TestTime
import com.vargod.testing.builders.aModelTemplate
import com.vargod.testing.builders.aSimulationResult
import com.vargod.testing.builders.anExperiment
import com.vargod.testing.mothers.ValidatorMother
import io.qameta.allure.Feature
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.UUID

@Feature("ExperimentRepository")
@DisplayName("JdbcExperimentRepository — classical style (embedded PostgreSQL)")
class JdbcExperimentRepositoryTest : DbTestBase() {

    private lateinit var sut: JdbcExperimentRepository

    private val owner = UUID.randomUUID()
    private val model = aModelTemplate().build()

    @BeforeEach
    fun setUp() {
        sut = JdbcExperimentRepository(dataSource, codec)
        givenUserWithId(owner)
        givenModel(model)
    }

    private fun experiment() = anExperiment().ownedBy(owner).forModel(model.id)

    // ---------------------------------------------------------------- save / findById

    @Test
    @EquivalencePartitioning
    fun `save - new experiment with parameters and validator snapshot is fully restored by findById`() {
        // Arrange
        val experiment = experiment().withValidatorSnapshot(listOf(ValidatorMother.positive("P"))).build()

        // Act
        sut.save(experiment)

        // Assert
        assertThat(sut.findById(experiment.id)).isEqualTo(experiment)
    }

    @Test
    @StateTransition
    fun `save - second save updates status, stores result and does not duplicate children`() {
        // Arrange
        val pending = sut.save(experiment().pending().build())
        val completed = pending.copy(status = ExperimentStatus.COMPLETED, result = aSimulationResult().build())

        // Act
        sut.save(completed)

        // Assert
        assertThat(sut.findById(pending.id)).isEqualTo(completed)
        assertThat(rowCount("parameter_sets")).isEqualTo(1)
        assertThat(rowCount("simulation_results")).isEqualTo(1)
    }

    @Test
    @BoundaryValues
    fun `save - name of 256 characters is rejected before touching the database`() {
        // Arrange
        val tooLong = experiment().named("x".repeat(256)).build()

        // Act
        val error = assertThrows<IllegalArgumentException> { sut.save(tooLong) }

        // Assert
        assertThat(error.message).contains("<= 255, got 256")
        assertThat(rowCount("experiments")).isZero()
    }

    @Test
    @ErrorGuessing
    fun `save - reference to unknown model is rolled back as a whole`() {
        // Arrange
        val orphan = experiment().forModel(UUID.randomUUID()).build()

        // Act
        val error = assertThrows<PersistenceException> { sut.save(orphan) }

        // Assert
        assertThat(error.message).startsWith("Failed to save experiment")
        assertThat(rowCount("experiments")).isZero()
        assertThat(rowCount("parameter_sets")).isZero()
    }

    @Test
    @EquivalencePartitioning
    fun `findById - unknown id gives null`() {
        // Arrange
        sut.save(experiment().build())

        // Act
        val result = sut.findById(UUID.randomUUID())

        // Assert
        assertThat(result).isNull()
    }

    // ---------------------------------------------------------------- findAllByUserId / findAllPublic

    @Test
    @EquivalencePartitioning
    fun `findAllByUserId - returns only experiments of that user in creation order`() {
        // Arrange
        val stranger = givenUser()
        val second = sut.save(experiment().createdAt(TestTime.minutesAfterNow(5)).build())
        val first = sut.save(experiment().createdAt(TestTime.minutesAfterNow(1)).build())
        sut.save(anExperiment().ownedBy(stranger.id).forModel(model.id).build())

        // Act
        val result = sut.findAllByUserId(owner)

        // Assert
        assertThat(result.map { it.id }).containsExactly(first.id, second.id)
    }

    @Test
    @BoundaryValues
    fun `findAllByUserId - user without experiments gets empty list`() {
        // Arrange
        sut.save(experiment().build())

        // Act
        val result = sut.findAllByUserId(UUID.randomUUID())

        // Assert
        assertThat(result).isEmpty()
    }

    @Test
    @EquivalencePartitioning
    fun `findAllPublic - returns public experiments with their results`() {
        // Arrange
        val published = sut.save(experiment().asPublic().completed().build())
        sut.save(experiment().asPrivate().build())

        // Act
        val result = sut.findAllPublic()

        // Assert
        assertThat(result).containsExactly(published)
    }

    @Test
    @BoundaryValues
    fun `findAllPublic - no public experiments gives empty list`() {
        // Arrange
        sut.save(experiment().asPrivate().build())

        // Act
        val result = sut.findAllPublic()

        // Assert
        assertThat(result).isEmpty()
    }

    // ---------------------------------------------------------------- findAllPaginated

    @Test
    @EquivalencePartitioning
    fun `findAllPaginated - second page of size 2 holds the oldest experiment`() {
        // Arrange
        val oldest = sut.save(experiment().createdAt(TestTime.minutesAfterNow(1)).build())
        sut.save(experiment().createdAt(TestTime.minutesAfterNow(2)).build())
        sut.save(experiment().createdAt(TestTime.minutesAfterNow(3)).build())

        // Act
        val page = sut.findAllPaginated(limit = 2, offset = 2)

        // Assert
        assertThat(page.map { it.id }).containsExactly(oldest.id)
    }

    @Test
    @BoundaryValues
    fun `findAllPaginated - limit 0 is rejected`() {
        // Arrange
        val limit = 0

        // Act
        val error = assertThrows<IllegalArgumentException> { sut.findAllPaginated(limit, 0) }

        // Assert
        assertThat(error.message).isEqualTo("limit must be in 1..1000, got 0")
    }

    // ---------------------------------------------------------------- findAllByIds

    @Test
    @EquivalencePartitioning
    fun `findAllByIds - returns only the requested experiments`() {
        // Arrange
        val a = sut.save(experiment().build())
        sut.save(experiment().build())
        val c = sut.save(experiment().build())

        // Act
        val result = sut.findAllByIds(listOf(a.id, c.id))

        // Assert
        assertThat(result.map { it.id }).containsExactlyInAnyOrder(a.id, c.id)
    }

    @Test
    @BoundaryValues
    fun `findAllByIds - empty id list gives empty result`() {
        // Arrange
        sut.save(experiment().build())

        // Act
        val result = sut.findAllByIds(emptyList())

        // Assert
        assertThat(result).isEmpty()
    }

    // ---------------------------------------------------------------- deleteById

    @Test
    @EquivalencePartitioning
    fun `deleteById - removes experiment together with parameters and result`() {
        // Arrange
        val experiment = sut.save(experiment().completed().build())

        // Act
        sut.deleteById(experiment.id)

        // Assert
        assertThat(sut.findById(experiment.id)).isNull()
        assertThat(rowCount("parameter_sets")).isZero()
        assertThat(rowCount("simulation_results")).isZero()
    }

    @Test
    @ErrorGuessing
    fun `deleteById - unknown id is a silent no-op`() {
        // Arrange
        sut.save(experiment().build())

        // Act
        sut.deleteById(UUID.randomUUID())

        // Assert
        assertThat(rowCount("experiments")).isEqualTo(1)
    }
}
