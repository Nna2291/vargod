package com.vargod.repository.jdbc

import com.vargod.domain.AuditEntry
import com.vargod.domain.PersistenceException
import com.vargod.domain.repository.AuditLogFilter
import com.vargod.domain.repository.AuditLogRepository
import java.sql.ResultSet
import java.sql.SQLException
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID
import javax.sql.DataSource

/**
 * JDBC-реализация журнала admin-действий.
 *
 * find() строит динамический WHERE с placeholder'ами `?` и биндит параметры
 * в порядке появления — без склейки значений в строку SQL (SQL injection guard).
 */
class JdbcAuditLogRepository(private val dataSource: DataSource) : AuditLogRepository {

    override fun save(entry: AuditEntry): AuditEntry = wrap("save audit") {
        dataSource.connection.use { conn ->
            conn.prepareStatement(SQL_INSERT).use { ps ->
                ps.setObject(1, entry.id)
                ps.setObject(2, entry.userId)
                ps.setString(3, entry.userEmail)
                ps.setString(4, entry.action)
                ps.setString(5, entry.targetType)
                ps.setObject(6, entry.targetId)
                ps.setString(7, entry.payload)
                ps.setObject(8, OffsetDateTime.ofInstant(entry.createdAt, ZoneOffset.UTC))
                ps.executeUpdate()
            }
        }
        entry
    }

    override fun find(filter: AuditLogFilter): List<AuditEntry> = wrap("find audit") {
        require(filter.limit in 1..1000) { "limit must be in 1..1000, got ${filter.limit}" }
        require(filter.offset >= 0) { "offset must be >= 0, got ${filter.offset}" }

        val clauses = mutableListOf<String>()
        val binds = mutableListOf<Any?>()
        if (filter.action != null) {
            clauses += "action = ?"
            binds += filter.action
        }
        if (filter.userId != null) {
            clauses += "user_id = ?"
            binds += filter.userId
        }
        if (filter.from != null) {
            clauses += "created_at >= ?"
            binds += OffsetDateTime.ofInstant(filter.from, ZoneOffset.UTC)
        }
        if (filter.to != null) {
            clauses += "created_at < ?"
            binds += OffsetDateTime.ofInstant(filter.to, ZoneOffset.UTC)
        }
        val where = if (clauses.isEmpty()) "" else " WHERE " + clauses.joinToString(" AND ")
        val sql = "SELECT $COLS FROM audit_log$where ORDER BY created_at DESC LIMIT ? OFFSET ?"

        dataSource.connection.use { conn ->
            conn.prepareStatement(sql).use { ps ->
                binds.forEachIndexed { i, v -> ps.setObject(i + 1, v) }
                ps.setInt(binds.size + 1, filter.limit)
                ps.setInt(binds.size + 2, filter.offset)
                ps.executeQuery().use { rs ->
                    val out = mutableListOf<AuditEntry>()
                    while (rs.next()) out += rs.toEntry()
                    out
                }
            }
        }
    }

    private fun <T> wrap(label: String, block: () -> T): T = try {
        block()
    } catch (e: SQLException) {
        throw PersistenceException("JdbcAuditLogRepository.$label failed: ${e.message}", e)
    }

    private fun ResultSet.toEntry(): AuditEntry = AuditEntry(
        id = getObject("id", UUID::class.java),
        userId = getObject("user_id", UUID::class.java),
        userEmail = getString("user_email"),
        action = getString("action"),
        targetType = getString("target_type"),
        targetId = getObject("target_id", UUID::class.java),
        payload = getString("payload"),
        createdAt = getObject("created_at", OffsetDateTime::class.java).toInstant(),
    )

    private companion object {
        const val COLS = "id, user_id, user_email, action, target_type, target_id, payload, created_at"
        const val SQL_INSERT =
            "INSERT INTO audit_log ($COLS) VALUES (?, ?, ?, ?, ?, ?, ?, ?)"
    }
}
