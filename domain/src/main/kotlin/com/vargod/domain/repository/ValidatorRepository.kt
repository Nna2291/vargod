package com.vargod.domain.repository

import com.vargod.domain.Validator
import java.util.UUID

/**
 * CRUD валидаторов. Появился в P3a для admin-эндпоинтов: до этого
 * валидаторы грузились только инлайн через [ModelTemplateRepository]
 * как часть модели (read-only). Здесь они — самостоятельная сущность,
 * привязанная к model_templates через FK.
 *
 * Контракт намеренно простой: ни сортировки, ни пагинации — admin-список
 * заведомо короткий (десятки записей на модель).
 */
interface ValidatorRepository {
    /** Если modelTemplateId != null, возвращает только валидаторы этой модели. */
    fun findAll(modelTemplateId: UUID? = null): List<Validator>
    fun findById(id: UUID): Validator?
    fun save(validator: Validator, modelTemplateId: UUID, createdBy: UUID?): Validator
    fun update(validator: Validator): Validator
    fun deleteById(id: UUID)
}
