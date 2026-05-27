package com.vargod.service

import com.vargod.domain.AuditEntry
import com.vargod.domain.User
import com.vargod.domain.repository.AuditLogFilter
import java.util.UUID

/**
 * Запись и чтение журнала admin-действий (см. таблицу audit_log, V6).
 * Бизнес-логика: помимо стандартного CRUD сервис снимает email пользователя
 * в момент записи — чтобы запись пережила удаление пользователя (FK SET NULL).
 */
interface AuditLogService {
    fun log(
        actor: User?,
        action: String,
        targetType: String? = null,
        targetId: UUID? = null,
        payload: Map<String, Any?>? = null,
    ): AuditEntry

    fun find(filter: AuditLogFilter): List<AuditEntry>
}
