package com.vargod.service.impl

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.vargod.domain.AuditEntry
import com.vargod.domain.User
import com.vargod.domain.repository.AuditLogFilter
import com.vargod.domain.repository.AuditLogRepository
import com.vargod.service.AuditLogService
import org.slf4j.LoggerFactory
import java.time.Clock
import java.time.Instant
import java.util.UUID

/**
 * Реализация журнала действий.
 *
 * Снимок userEmail на момент записи (вместо JOIN на users при чтении) — журнал
 * должен оставаться читаемым даже после удаления пользователя (FK SET NULL
 * только NULL'ит user_id, но user_email — собственная колонка).
 *
 * payload сериализуется в JSON через Jackson; ошибки сериализации не падают
 * наружу — логируются и в БД попадает null. Журнал не должен ломать основное
 * действие из-за служебной записи.
 */
class AuditLogServiceImpl(
    private val auditLogRepository: AuditLogRepository,
    private val mapper: ObjectMapper = jacksonObjectMapper(),
    /** Источник времени для createdAt записи журнала (внедряется ради детерминированных тестов). */
    private val clock: Clock = Clock.systemUTC(),
) : AuditLogService {

    private val log = LoggerFactory.getLogger(AuditLogServiceImpl::class.java)

    override fun log(
        actor: User?,
        action: String,
        targetType: String?,
        targetId: UUID?,
        payload: Map<String, Any?>?,
    ): AuditEntry {
        val payloadJson = payload?.let { encodePayload(it) }
        val entry = AuditEntry(
            id = UUID.randomUUID(),
            userId = actor?.id,
            // Снимок email на момент действия — переживёт удаление пользователя.
            userEmail = actor?.email,
            action = action,
            targetType = targetType,
            targetId = targetId,
            payload = payloadJson,
            createdAt = Instant.now(clock),
        )
        return auditLogRepository.save(entry)
    }

    override fun find(filter: AuditLogFilter): List<AuditEntry> =
        auditLogRepository.find(filter)

    private fun encodePayload(payload: Map<String, Any?>): String? = try {
        mapper.writeValueAsString(payload)
    } catch (e: Exception) {
        log.warn("Не удалось сериализовать payload аудита: {}", e.message)
        null
    }
}
