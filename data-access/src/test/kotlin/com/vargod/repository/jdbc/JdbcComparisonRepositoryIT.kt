package com.vargod.repository.jdbc

import com.vargod.domain.Comparison
import com.vargod.testfixtures.Fixtures
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID

class JdbcComparisonRepositoryIT : JdbcRepositoryTestBase() {

    @Test
    fun `save stores comparison with experiments and findById returns them in insertion order`() {
        val model = Fixtures.model()
        seedModelTemplate(model)
        val userId = UUID.randomUUID()
        val e1 = Fixtures.experiment(userId = userId, modelTemplateId = model.id)
        val e2 = Fixtures.experiment(userId = userId, modelTemplateId = model.id)
        val e3 = Fixtures.experiment(userId = userId, modelTemplateId = model.id)
        listOf(e1, e2, e3).forEach { experimentRepository.save(it) }

        val comparison = Comparison(
            id = UUID.randomUUID(),
            name = "Three runs",
            userId = userId,
            experiments = listOf(e3, e1, e2), // deliberately not chronological
            createdAt = Instant.parse("2026-05-14T11:00:00Z"),
        )
        comparisonRepository.save(comparison)

        val loaded = comparisonRepository.findById(comparison.id)
        assertNotNull(loaded)
        assertEquals(comparison.id, loaded!!.id)
        assertEquals("Three runs", loaded.name)
        assertEquals(userId, loaded.userId)
        assertEquals(comparison.createdAt, loaded.createdAt)
        assertEquals(listOf(e3.id, e1.id, e2.id), loaded.experiments.map { it.id })
    }

    @Test
    fun `findAllByUserId returns only that users comparisons`() {
        val model = Fixtures.model()
        seedModelTemplate(model)
        val alice = UUID.randomUUID()
        val bob = UUID.randomUUID()
        val aliceExps = listOf(
            Fixtures.experiment(userId = alice, modelTemplateId = model.id),
            Fixtures.experiment(userId = alice, modelTemplateId = model.id),
        )
        val bobExps = listOf(
            Fixtures.experiment(userId = bob, modelTemplateId = model.id),
            Fixtures.experiment(userId = bob, modelTemplateId = model.id),
        )
        (aliceExps + bobExps).forEach { experimentRepository.save(it) }

        val aliceCmp = Comparison(
            id = UUID.randomUUID(),
            name = "Alice",
            userId = alice,
            experiments = aliceExps,
            createdAt = Instant.parse("2026-05-14T12:00:00Z"),
        )
        val bobCmp = Comparison(
            id = UUID.randomUUID(),
            name = "Bob",
            userId = bob,
            experiments = bobExps,
            createdAt = Instant.parse("2026-05-14T12:30:00Z"),
        )
        comparisonRepository.save(aliceCmp)
        comparisonRepository.save(bobCmp)

        val aliceComparisons = comparisonRepository.findAllByUserId(alice)
        assertEquals(1, aliceComparisons.size)
        assertEquals(aliceCmp.id, aliceComparisons[0].id)
        assertEquals(2, aliceComparisons[0].experiments.size)

        val bobComparisons = comparisonRepository.findAllByUserId(bob)
        assertEquals(1, bobComparisons.size)
        assertEquals(bobCmp.id, bobComparisons[0].id)
    }
}
