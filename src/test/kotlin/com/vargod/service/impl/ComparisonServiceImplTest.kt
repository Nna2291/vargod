package com.vargod.service.impl

import com.vargod.domain.AuthorizationException
import com.vargod.domain.Comparison
import com.vargod.domain.NotFoundException
import com.vargod.repository.ComparisonRepository
import com.vargod.repository.ExperimentRepository
import com.vargod.testfixtures.Fixtures
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.util.UUID

class ComparisonServiceImplTest {

    private val comparisonRepo = mockk<ComparisonRepository>()
    private val experimentRepo = mockk<ExperimentRepository>()
    private val service = ComparisonServiceImpl(comparisonRepo, experimentRepo)

    @Test
    fun `create succeeds when experiments belong to user and share a model`() {
        val userId = UUID.randomUUID()
        val modelId = UUID.randomUUID()
        val e1 = Fixtures.experiment(userId = userId, modelTemplateId = modelId)
        val e2 = Fixtures.experiment(userId = userId, modelTemplateId = modelId)
        every { experimentRepo.findAllByIds(listOf(e1.id, e2.id)) } returns listOf(e1, e2)

        val savedSlot = slot<Comparison>()
        every { comparisonRepo.save(capture(savedSlot)) } answers { savedSlot.captured }

        val result = service.create(listOf(e1.id, e2.id), userId)
        assertEquals(userId, result.userId)
        assertEquals(listOf(e1, e2), result.experiments)
        verify(exactly = 1) { comparisonRepo.save(any()) }
    }

    @Test
    fun `create succeeds when a foreign experiment is public`() {
        val userId = UUID.randomUUID()
        val otherUser = UUID.randomUUID()
        val modelId = UUID.randomUUID()
        val owned = Fixtures.experiment(userId = userId, modelTemplateId = modelId)
        val foreignPublic = Fixtures.experiment(userId = otherUser, modelTemplateId = modelId, isPublic = true)
        every { experimentRepo.findAllByIds(listOf(owned.id, foreignPublic.id)) } returns listOf(owned, foreignPublic)
        every { comparisonRepo.save(any()) } answers { firstArg() }

        val result = service.create(listOf(owned.id, foreignPublic.id), userId)
        assertEquals(2, result.experiments.size)
    }

    @Test
    fun `create rejects fewer than two experiments`() {
        val userId = UUID.randomUUID()
        assertThrows(IllegalArgumentException::class.java) {
            service.create(listOf(UUID.randomUUID()), userId)
        }
    }

    @Test
    fun `create rejects duplicate ids`() {
        val id = UUID.randomUUID()
        assertThrows(IllegalArgumentException::class.java) {
            service.create(listOf(id, id), UUID.randomUUID())
        }
    }

    @Test
    fun `create throws NotFoundException when an experiment is missing`() {
        val userId = UUID.randomUUID()
        val modelId = UUID.randomUUID()
        val e1 = Fixtures.experiment(userId = userId, modelTemplateId = modelId)
        val missingId = UUID.randomUUID()
        every { experimentRepo.findAllByIds(listOf(e1.id, missingId)) } returns listOf(e1)
        val ex = assertThrows(NotFoundException::class.java) {
            service.create(listOf(e1.id, missingId), userId)
        }
        assert(ex.message!!.contains(missingId.toString()))
    }

    @Test
    fun `create rejects when a foreign private experiment is included`() {
        val userId = UUID.randomUUID()
        val modelId = UUID.randomUUID()
        val owned = Fixtures.experiment(userId = userId, modelTemplateId = modelId)
        val foreignPrivate = Fixtures.experiment(userId = UUID.randomUUID(), modelTemplateId = modelId, isPublic = false)
        every { experimentRepo.findAllByIds(any()) } returns listOf(owned, foreignPrivate)
        assertThrows(AuthorizationException::class.java) {
            service.create(listOf(owned.id, foreignPrivate.id), userId)
        }
    }

    @Test
    fun `create rejects experiments with different models`() {
        val userId = UUID.randomUUID()
        val e1 = Fixtures.experiment(userId = userId, modelTemplateId = UUID.randomUUID())
        val e2 = Fixtures.experiment(userId = userId, modelTemplateId = UUID.randomUUID())
        every { experimentRepo.findAllByIds(any()) } returns listOf(e1, e2)
        assertThrows(IllegalArgumentException::class.java) {
            service.create(listOf(e1.id, e2.id), userId)
        }
    }

    @Test
    fun `getById returns comparison when present`() {
        val cmp = Comparison(
            id = UUID.randomUUID(),
            name = null,
            userId = UUID.randomUUID(),
            experiments = emptyList(),
            createdAt = java.time.Instant.parse("2026-05-13T10:00:00Z"),
        )
        every { comparisonRepo.findById(cmp.id) } returns cmp
        assertSame(cmp, service.getById(cmp.id))
    }

    @Test
    fun `getById throws NotFoundException when missing`() {
        val id = UUID.randomUUID()
        every { comparisonRepo.findById(id) } returns null
        assertThrows(NotFoundException::class.java) { service.getById(id) }
    }

    @Test
    fun `listByUser delegates to repository`() {
        val userId = UUID.randomUUID()
        every { comparisonRepo.findAllByUserId(userId) } returns emptyList()
        assertEquals(emptyList<Comparison>(), service.listByUser(userId))
        verify(exactly = 1) { comparisonRepo.findAllByUserId(userId) }
    }
}
