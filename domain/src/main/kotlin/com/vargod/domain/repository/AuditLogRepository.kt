package com.vargod.domain.repository

import com.vargod.domain.AuditEntry
import java.time.Instant
import java.util.UUID

/**
 * Хранилище журнала admin-действий. Только append + read с фильтрами;
 * редактирование и удаление сознательно не предоставляются — журнал
 * считается append-only artifact'ом.
 */
interface AuditLogRepository {
    fun save(entry: AuditEntry): AuditEntry
    fun find(filter: AuditLogFilter): List<AuditEntry>
}

/**
 * Фильтр выборки записей журнала. Все поля опциональны и складываются
 * через AND. limit/offset — стандартная страничная навигация (LIMIT/OFFSET).
 */
data class AuditLogFilter(
    val action: String? = null,
    val userId: UUID? = null,
    val from: Instant? = null,
    val to: Instant? = null,
    val limit: Int = 100,
    val offset: Int = 0,
)
