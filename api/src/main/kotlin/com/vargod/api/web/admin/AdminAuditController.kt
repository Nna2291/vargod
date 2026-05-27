package com.vargod.api.web.admin

import com.vargod.domain.repository.AuditLogFilter
import com.vargod.service.AuditLogService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.Instant
import java.time.format.DateTimeParseException
import java.util.UUID

/**
 * Чтение журнала admin-действий с фильтром по action / userId / временному
 * диапазону. from/to принимаются как ISO-8601 строки.
 */
@RestController
@RequestMapping("/api/admin/audit")
@Tag(name = "Admin: Audit Log", description = "Журнал admin-действий")
class AdminAuditController(
    private val auditLogService: AuditLogService,
) {

    @Operation(summary = "Список записей журнала с фильтрами (action, userId, from, to, limit, offset)")
    @GetMapping
    fun list(
        @RequestParam(required = false) action: String?,
        @RequestParam(required = false) userId: UUID?,
        @RequestParam(required = false) from: String?,
        @RequestParam(required = false) to: String?,
        @RequestParam(defaultValue = "100") limit: Int,
        @RequestParam(defaultValue = "0") offset: Int,
    ): List<AuditEntryView> {
        val filter = AuditLogFilter(
            action = action,
            userId = userId,
            from = parseInstant("from", from),
            to = parseInstant("to", to),
            limit = limit,
            offset = offset,
        )
        return auditLogService.find(filter).map(AuditEntryView::of)
    }

    private fun parseInstant(name: String, raw: String?): Instant? {
        if (raw.isNullOrBlank()) return null
        return try {
            Instant.parse(raw)
        } catch (e: DateTimeParseException) {
            throw IllegalArgumentException("Parameter '$name' must be ISO-8601 instant, got '$raw'")
        }
    }
}
