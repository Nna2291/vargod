package com.vargod.service.impl

import com.vargod.domain.ModelTemplate
import com.vargod.domain.NotFoundException
import com.vargod.domain.repository.ModelTemplateRepository
import com.vargod.service.ModelService
import org.slf4j.LoggerFactory
import java.util.UUID

class ModelServiceImpl(
    private val modelRepository: ModelTemplateRepository,
) : ModelService {

    private val log = LoggerFactory.getLogger(ModelServiceImpl::class.java)

    override fun listAll(): List<ModelTemplate> {
        log.debug("Запрос каталога моделей")
        return modelRepository.findAll()
    }

    override fun getById(id: UUID): ModelTemplate {
        log.debug("Запрос модели {}", id)
        return modelRepository.findById(id) ?: throw NotFoundException("ModelTemplate", id)
    }
}
