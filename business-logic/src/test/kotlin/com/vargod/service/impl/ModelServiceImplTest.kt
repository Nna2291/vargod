package com.vargod.service.impl

import com.vargod.domain.NotFoundException
import com.vargod.domain.repository.ModelTemplateRepository
import com.vargod.testfixtures.Fixtures
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.util.UUID

class ModelServiceImplTest {

    private val repo = mockk<ModelTemplateRepository>()
    private val service = ModelServiceImpl(repo)

    @Test
    fun `listAll returns all models from repository`() {
        val models = listOf(Fixtures.model(code = "SIR"), Fixtures.model(code = "LOGISTIC"))
        every { repo.findAll() } returns models
        assertEquals(models, service.listAll())
        verify(exactly = 1) { repo.findAll() }
    }

    @Test
    fun `getById returns model when found`() {
        val model = Fixtures.model()
        every { repo.findById(model.id) } returns model
        assertEquals(model, service.getById(model.id))
    }

    @Test
    fun `getById throws NotFoundException when missing`() {
        val id = UUID.randomUUID()
        every { repo.findById(id) } returns null
        assertThrows(NotFoundException::class.java) { service.getById(id) }
    }
}
