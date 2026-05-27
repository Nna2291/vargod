package com.vargod.api.web.admin

import com.vargod.domain.AuditEntry
import com.vargod.domain.Validator
import com.vargod.domain.ValidatorTargetKind
import com.vargod.domain.ValidatorType
import com.vargod.engine.validation.RuleParamsSchema
import java.time.format.DateTimeFormatter
import java.util.UUID

// ============================================================
//  Admin: пользователи
// ============================================================

data class CreateUserRequest(val email: String, val password: String, val role: String)
data class ChangeRoleRequest(val role: String)

// ============================================================
//  Admin: валидаторы
// ============================================================

data class CreateValidatorRequest(
    val name: String,
    val checkType: ValidatorType,
    val targetKind: ValidatorTargetKind,
    val targetField: String,
    val rules: Map<String, Any>,
    val modelTemplateId: UUID,
)

data class UpdateValidatorRequest(
    val name: String,
    val checkType: ValidatorType,
    val targetKind: ValidatorTargetKind,
    val targetField: String,
    val rules: Map<String, Any>,
)

/**
 * Тип правила для admin UI: имя + полная схема параметров.
 * paramsSchema — доменный класс из engine.validation, сериализуется Jackson'ом
 * как есть; admin-фронтенд берёт его и рендерит динамическую форму.
 */
data class ValidatorTypeView(
    val type: String,
    val paramsSchema: RuleParamsSchema,
)

// ============================================================
//  Admin: журнал действий
// ============================================================

/**
 * Внешнее представление записи журнала. createdAt — ISO-8601 строкой
 * (Instant сериализуется по умолчанию массивом, что неудобно фронту).
 */
data class AuditEntryView(
    val id: UUID,
    val userId: UUID?,
    val userEmail: String?,
    val action: String,
    val targetType: String?,
    val targetId: UUID?,
    val payload: String?,
    val createdAt: String,
) {
    companion object {
        fun of(entry: AuditEntry): AuditEntryView = AuditEntryView(
            id = entry.id,
            userId = entry.userId,
            userEmail = entry.userEmail,
            action = entry.action,
            targetType = entry.targetType,
            targetId = entry.targetId,
            payload = entry.payload,
            createdAt = DateTimeFormatter.ISO_INSTANT.format(entry.createdAt),
        )
    }
}

// ============================================================
//  Admin: шаблоны моделей
// ============================================================

data class CreateModelTemplateRequest(
    val code: String,
    val name: String,
    val description: String?,
    val solverType: String,
    val recommendedMaxStep: Double? = null,
    val recommendedRelTol: Double? = null,
    val recommendedAbsTol: Double? = null,
)

data class UpdateModelTemplateRequest(
    val code: String,
    val name: String,
    val description: String?,
    val solverType: String,
    val recommendedMaxStep: Double? = null,
    val recommendedRelTol: Double? = null,
    val recommendedAbsTol: Double? = null,
)

/** View валидатора с минимумом полей — для admin-листингов. Совпадает с доменом. */
data class ValidatorView(
    val id: UUID,
    val name: String,
    val checkType: ValidatorType,
    val targetKind: ValidatorTargetKind,
    val targetField: String,
    val rules: Map<String, Any>,
) {
    companion object {
        fun of(v: Validator): ValidatorView = ValidatorView(
            id = v.id,
            name = v.name,
            checkType = v.checkType,
            targetKind = v.targetKind,
            targetField = v.targetField,
            rules = v.rules,
        )
    }
}
