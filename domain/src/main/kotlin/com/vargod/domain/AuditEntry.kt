package com.vargod.domain

import java.time.Instant
import java.util.UUID

/**
 * Запись журнала admin-действий (см. таблицу `audit_log`, V6).
 *
 * userEmail — снимок на момент записи: пользователь может быть удалён
 * (FK SET NULL), но запись должна оставаться читаемой человеком.
 * Поэтому при логировании сервис копирует email в саму запись —
 * а не подгружает через JOIN при чтении.
 *
 * action — короткий код действия из [com.vargod.service.AuditActions];
 * targetType + targetId — необязательная ссылка на сущность, к которой
 * относится действие (например, "USER" + userId).
 * payload — произвольный JSON со специфичными для действия деталями.
 */
data class AuditEntry(
    val id: UUID,
    val userId: UUID?,
    val userEmail: String?,
    val action: String,
    val targetType: String?,
    val targetId: UUID?,
    val payload: String?,
    val createdAt: Instant,
)
