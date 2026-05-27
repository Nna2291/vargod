package com.vargod.repository.jdbc

import com.vargod.domain.Comparison
import com.vargod.domain.PersistenceException
import com.vargod.domain.repository.ComparisonRepository
import com.vargod.domain.repository.ExperimentRepository
import java.sql.Connection
import java.sql.SQLException
import java.util.UUID
import javax.sql.DataSource

class JdbcComparisonRepository(
    private val dataSource: DataSource,
    private val experimentRepository: ExperimentRepository,
) : ComparisonRepository {

    override fun findById(id: UUID): Comparison? = try {
        dataSource.connection.use { conn ->
            val row = conn.prepareStatement(SQL_FIND_BY_ID).use { ps ->
                ps.setObject(1, id)
                ps.executeQuery().use { rs ->
                    if (rs.next()) ComparisonRow(
                        id = rs.readUuid("id"),
                        name = rs.getString("name"),
                        userId = rs.readUuid("user_id"),
                        createdAt = rs.readInstant("created_at"),
                    ) else null
                }
            }
            if (row == null) null
            else {
                val orderedIds = loadExperimentIds(conn, listOf(row.id))[row.id] ?: emptyList()
                row.assemble(loadExperimentsInOrder(orderedIds))
            }
        }
    } catch (e: SQLException) {
        throw PersistenceException("Failed to find comparison id=$id: ${e.message}", e)
    }

    override fun findAllByUserId(userId: UUID): List<Comparison> = try {
        dataSource.connection.use { conn ->
            val rows = mutableListOf<ComparisonRow>()
            conn.prepareStatement(SQL_FIND_BY_USER).use { ps ->
                ps.setObject(1, userId)
                ps.executeQuery().use { rs ->
                    while (rs.next()) {
                        rows += ComparisonRow(
                            id = rs.readUuid("id"),
                            name = rs.getString("name"),
                            userId = rs.readUuid("user_id"),
                            createdAt = rs.readInstant("created_at"),
                        )
                    }
                }
            }
            if (rows.isEmpty()) return@use emptyList()
            val orderedByComparison = loadExperimentIds(conn, rows.map { it.id })
            rows.map { row ->
                val ids = orderedByComparison[row.id] ?: emptyList()
                row.assemble(loadExperimentsInOrder(ids))
            }
        }
    } catch (e: SQLException) {
        throw PersistenceException("Failed to list comparisons for user $userId: ${e.message}", e)
    }

    override fun save(comparison: Comparison): Comparison {
        validate(comparison)
        try {
            dataSource.connection.use { conn ->
                conn.autoCommit = false
                try {
                    upsertComparison(conn, comparison)
                    replaceJunction(conn, comparison)
                    conn.commit()
                } catch (e: SQLException) {
                    conn.rollback()
                    throw PersistenceException(
                        "Failed to save comparison id=${comparison.id}: ${e.message}", e
                    )
                } finally {
                    conn.autoCommit = true
                }
            }
        } catch (e: SQLException) {
            throw PersistenceException("Failed to obtain connection: ${e.message}", e)
        }
        return comparison
    }

    // --- internals --------------------------------------------------------

    private fun validate(c: Comparison) {
        require(c.experiments.isNotEmpty()) { "comparison must contain at least one experiment" }
        val ids = c.experiments.map { it.id }
        require(ids.distinct().size == ids.size) { "comparison experiments must be unique" }
        val name = c.name
        require(name == null || name.length <= 255) {
            "comparison.name length must be <= 255, got ${name?.length}"
        }
    }

    private fun upsertComparison(conn: Connection, c: Comparison) {
        val exists = conn.prepareStatement("SELECT 1 FROM comparisons WHERE id = ?").use { ps ->
            ps.setObject(1, c.id)
            ps.executeQuery().use { it.next() }
        }
        if (exists) {
            conn.prepareStatement(SQL_UPDATE_COMPARISON).use { ps ->
                ps.setString(1, c.name)
                ps.setObject(2, c.id)
                ps.executeUpdate()
            }
        } else {
            conn.prepareStatement(SQL_INSERT_COMPARISON).use { ps ->
                ps.setObject(1, c.id)
                ps.setString(2, c.name)
                ps.setObject(3, OffsetDateTimeUtc(c.createdAt))
                ps.setObject(4, c.userId)
                ps.executeUpdate()
            }
        }
    }

    private fun replaceJunction(conn: Connection, c: Comparison) {
        conn.prepareStatement("DELETE FROM comparison_experiments WHERE comparison_id = ?").use { ps ->
            ps.setObject(1, c.id)
            ps.executeUpdate()
        }
        if (c.experiments.isEmpty()) return
        conn.prepareStatement(SQL_INSERT_JUNCTION).use { ps ->
            c.experiments.forEachIndexed { index, exp ->
                ps.setObject(1, c.id)
                ps.setObject(2, exp.id)
                ps.setInt(3, index)
                ps.addBatch()
            }
            ps.executeBatch()
        }
    }

    private fun loadExperimentIds(conn: Connection, comparisonIds: List<UUID>): Map<UUID, List<UUID>> {
        if (comparisonIds.isEmpty()) return emptyMap()
        val placeholders = comparisonIds.joinToString(",") { "?" }
        val sql = "SELECT comparison_id, experiment_id, position FROM comparison_experiments " +
            "WHERE comparison_id IN ($placeholders) ORDER BY comparison_id, position"
        val out = mutableMapOf<UUID, MutableList<UUID>>()
        conn.prepareStatement(sql).use { ps ->
            comparisonIds.forEachIndexed { i, uuid -> ps.setObject(i + 1, uuid) }
            ps.executeQuery().use { rs ->
                while (rs.next()) {
                    val cmpId = rs.readUuid("comparison_id")
                    out.getOrPut(cmpId) { mutableListOf() } += rs.readUuid("experiment_id")
                }
            }
        }
        return out
    }

    private fun loadExperimentsInOrder(ids: List<UUID>) =
        if (ids.isEmpty()) emptyList()
        else experimentRepository.findAllByIds(ids).let { fetched ->
            val byId = fetched.associateBy { it.id }
            ids.mapNotNull { byId[it] }
        }

    private data class ComparisonRow(
        val id: UUID,
        val name: String?,
        val userId: UUID,
        val createdAt: java.time.Instant,
    ) {
        fun assemble(experiments: List<com.vargod.domain.Experiment>) = Comparison(
            id = id, name = name, userId = userId, experiments = experiments, createdAt = createdAt,
        )
    }

    companion object {
        private const val SQL_FIND_BY_ID =
            "SELECT id, name, created_at, user_id FROM comparisons WHERE id = ?"
        private const val SQL_FIND_BY_USER =
            "SELECT id, name, created_at, user_id FROM comparisons WHERE user_id = ? ORDER BY created_at"
        private const val SQL_INSERT_COMPARISON =
            "INSERT INTO comparisons (id, name, created_at, user_id) VALUES (?, ?, ?, ?)"
        private const val SQL_UPDATE_COMPARISON =
            "UPDATE comparisons SET name = ? WHERE id = ?"
        private const val SQL_INSERT_JUNCTION =
            "INSERT INTO comparison_experiments (comparison_id, experiment_id, position) VALUES (?, ?, ?)"
    }
}
