package com.vargod.service.impl

import com.vargod.domain.ModelTemplate
import com.vargod.domain.NotFoundException
import com.vargod.repository.ModelTemplateRepository
import com.vargod.service.ModelService
import java.util.UUID

class ModelServiceImpl(
    private val modelRepository: ModelTemplateRepository,
) : ModelService {

    override fun listAll(): List<ModelTemplate> = modelRepository.findAll()

    override fun getById(id: UUID): ModelTemplate =
        modelRepository.findById(id) ?: throw NotFoundException("ModelTemplate", id)
}
