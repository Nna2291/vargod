package com.vargod.service

import com.vargod.domain.Validator
import com.vargod.engine.validation.RuleParamsSchema
import java.util.UUID

/**
 * Admin-CRUD валидаторов. До сохранения payload каждого валидатора прогоняется
 * через ConstraintRule.validateParams(...) — чтобы 422 прилетал прямо при
 * создании, а не позже, при запуске эксперимента с этим валидатором.
 */
interface ValidatorAdminService {
    fun list(modelTemplateId: UUID? = null): List<Validator>
    fun get(id: UUID): Validator
    fun create(validator: Validator, modelTemplateId: UUID, createdBy: UUID?): Validator
    fun update(validator: Validator): Validator
    fun delete(id: UUID)

    /**
     * Список доступных типов правил вместе с их paramsSchema — для admin UI,
     * чтобы дать строить форму создания валидатора динамически.
     */
    fun listAvailableTypes(): List<ValidatorTypeInfo>
}

data class ValidatorTypeInfo(
    val type: String,
    val paramsSchema: RuleParamsSchema,
)
