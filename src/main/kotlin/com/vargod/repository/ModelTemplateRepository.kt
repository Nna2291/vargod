package com.vargod.repository

import com.vargod.domain.ModelTemplate
import java.util.UUID

interface ModelTemplateRepository {
    fun findAll(): List<ModelTemplate>
    fun findById(id: UUID): ModelTemplate?
}
