package com.vargod.service

import com.vargod.domain.ModelTemplate
import java.util.UUID

interface ModelService {
    fun listAll(): List<ModelTemplate>
    fun getById(id: UUID): ModelTemplate
}
