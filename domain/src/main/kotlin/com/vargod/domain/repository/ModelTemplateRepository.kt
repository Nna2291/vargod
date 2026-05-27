package com.vargod.domain.repository

import com.vargod.domain.ModelTemplate
import java.util.UUID

interface ModelTemplateRepository {
    fun findAll(): List<ModelTemplate>
    fun findById(id: UUID): ModelTemplate?
    /**
     * Сохраняет шаблон модели. Валидаторы из template.validators НЕ переписываются —
     * жизненный цикл валидаторов отдан [ValidatorRepository]. На update метаданные
     * (name/description/solverType/code) обновляются, validators игнорируются.
     */
    fun save(template: ModelTemplate): ModelTemplate
    fun deleteById(id: UUID)
}
