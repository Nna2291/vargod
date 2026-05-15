package com.vargod.repository.jdbc

import com.vargod.domain.Experiment
import com.vargod.domain.ParameterSet
import com.vargod.domain.PersistenceException
import com.vargod.domain.SimulationResult
import com.vargod.domain.repository.ExperimentRepository
import java.sql.Connection
import java.sql.SQLException
import java.util.UUID
import javax.sql.DataSource

class JdbcExperimentRepository(
    private val dataSource: DataSource,
    private val codec: JsonCodec = JsonCodec(),
) : ExperimentRepository {

    override fun findById(id: UUID): Experiment? = try {
        dataSource.connection.use { conn ->
            val row = conn.prepareStatement(SQL_FIND_BY_ID).use { ps ->
                ps.setObject(1, id)
                ps.executeQuery().use { rs -> if (rs.next()) rs.readExperimentRow() else null }
            } ?: return@use null
            row.assemble(
                parameterSet = loadParameterSet(conn, row.id),
                result = loadSimulationResult(conn, row.id),
            )
        }
    } catch (e: SQLException) {
        throw PersistenceException("Failed to find experiment id=$id: ${e.message}", e)
    }

    override fun findAllByUserId(userId: UUID): List<Experiment> =
        findAllBy("WHERE user_id = ?", arrayOf(userId), "Failed to list experiments for user $userId")

    override fun findAllPublic(): List<Experiment> =
        findAllBy("WHERE is_public = TRUE", emptyArray(), "Failed to list public experiments")

    override fun findAllByIds(ids: List<UUID>): List<Experiment> {
        if (ids.isEmpty()) return emptyList()
        val placeholders = ids.joinToString(",") { "?" }
        return findAllBy(
            "WHERE id IN ($placeholders)",
            ids.toTypedArray(),
            "Failed to list experiments by ids",
        )
    }

    override fun save(experiment: Experiment): Experiment {
        validate(experiment)
        try {
            dataSource.connection.use { conn ->
                conn.autoCommit = false
                try {
                    upsertExperiment(conn, experiment)
                    replaceParameterSet(conn, experiment.id, experiment.parameterSet)
                    replaceSimulationResult(conn, experiment.id, experiment.result)
                    conn.commit()
                } catch (e: SQLException) {
                    conn.rollback()
                    throw PersistenceException(
                        "Failed to save experiment id=${experiment.id}: ${e.message}", e
                    )
                } finally {
                    conn.autoCommit = true
                }
            }
        } catch (e: SQLException) {
            throw PersistenceException("Failed to obtain connection: ${e.message}", e)
        }
        return experiment
    }

    override fun deleteById(id: UUID) {
        try {
            dataSource.connection.use { conn ->
                conn.prepareStatement("DELETE FROM experiments WHERE id = ?").use { ps ->
                    ps.setObject(1, id)
                    ps.executeUpdate()
                }
            }
        } catch (e: SQLException) {
            throw PersistenceException("Failed to delete experiment id=$id: ${e.message}", e)
        }
    }

    // --- internals --------------------------------------------------------

    private fun validate(experiment: Experiment) {
        require(experiment.name == null || experiment.name.length <= 255) {
            "experiment.name length must be <= 255, got ${experiment.name?.length}"
        }
        experiment.parameterSet?.let { ps ->
            require(ps.values.isNotEmpty()) { "parameterSet.values must not be empty" }
            require(ps.tEnd > ps.tStart) { "parameterSet.tEnd must be greater than tStart" }
            require(ps.step > 0.0) { "parameterSet.step must be positive" }
        }
    }

    private fun findAllBy(
        whereClause: String,
        params: Array<out Any>,
        failureMessage: String,
    ): List<Experiment> = try {
        dataSource.connection.use { conn ->
            val rows = mutableListOf<ExperimentRow>()
            conn.prepareStatement("$SQL_SELECT_EXPERIMENT_BASE $whereClause ORDER BY created_at").use { ps ->
                params.forEachIndexed { i, p -> ps.setObject(i + 1, p) }
                ps.executeQuery().use { rs ->
                    while (rs.next()) rows += rs.readExperimentRow()
                }
            }
            if (rows.isEmpty()) return@use emptyList()
            val ids = rows.map { it.id }
            val parameterSets = loadParameterSets(conn, ids)
            val results = loadSimulationResults(conn, ids)
            rows.map { it.assemble(parameterSets[it.id], results[it.id]) }
        }
    } catch (e: SQLException) {
        throw PersistenceException("$failureMessage: ${e.message}", e)
    }

    private fun loadParameterSet(conn: Connection, experimentId: UUID): ParameterSet? {
        conn.prepareStatement(SQL_FIND_PARAMETER_SET_BY_EXPERIMENT).use { ps ->
            ps.setObject(1, experimentId)
            ps.executeQuery().use { rs ->
                return if (rs.next()) rs.readParameterSet(codec) else null
            }
        }
    }

    private fun loadSimulationResult(conn: Connection, experimentId: UUID): SimulationResult? {
        conn.prepareStatement(SQL_FIND_RESULT_BY_EXPERIMENT).use { ps ->
            ps.setObject(1, experimentId)
            ps.executeQuery().use { rs ->
                return if (rs.next()) rs.readSimulationResult(codec) else null
            }
        }
    }

    private fun loadParameterSets(conn: Connection, experimentIds: List<UUID>): Map<UUID, ParameterSet> {
        val placeholders = experimentIds.joinToString(",") { "?" }
        val sql = "SELECT id, \"values\", t_start, t_end, step, experiment_id FROM parameter_sets " +
            "WHERE experiment_id IN ($placeholders)"
        val out = mutableMapOf<UUID, ParameterSet>()
        conn.prepareStatement(sql).use { ps ->
            experimentIds.forEachIndexed { i, uuid -> ps.setObject(i + 1, uuid) }
            ps.executeQuery().use { rs ->
                while (rs.next()) {
                    val expId = rs.readUuid("experiment_id")
                    out[expId] = rs.readParameterSet(codec)
                }
            }
        }
        return out
    }

    private fun loadSimulationResults(conn: Connection, experimentIds: List<UUID>): Map<UUID, SimulationResult> {
        val placeholders = experimentIds.joinToString(",") { "?" }
        val sql = "SELECT id, data, created_at, experiment_id FROM simulation_results " +
            "WHERE experiment_id IN ($placeholders)"
        val out = mutableMapOf<UUID, SimulationResult>()
        conn.prepareStatement(sql).use { ps ->
            experimentIds.forEachIndexed { i, uuid -> ps.setObject(i + 1, uuid) }
            ps.executeQuery().use { rs ->
                while (rs.next()) {
                    val expId = rs.readUuid("experiment_id")
                    out[expId] = rs.readSimulationResult(codec)
                }
            }
        }
        return out
    }

    private fun upsertExperiment(conn: Connection, e: Experiment) {
        val exists = conn.prepareStatement("SELECT 1 FROM experiments WHERE id = ?").use { ps ->
            ps.setObject(1, e.id)
            ps.executeQuery().use { it.next() }
        }
        if (exists) {
            conn.prepareStatement(SQL_UPDATE_EXPERIMENT).use { ps ->
                ps.setString(1, e.name)
                ps.setString(2, e.status.name)
                ps.setBoolean(3, e.isPublic)
                ps.setObject(4, e.modelTemplateId)
                ps.setObject(5, e.id)
                ps.executeUpdate()
            }
        } else {
            conn.prepareStatement(SQL_INSERT_EXPERIMENT).use { ps ->
                ps.setObject(1, e.id)
                ps.setString(2, e.name)
                ps.setString(3, e.status.name)
                ps.setBoolean(4, e.isPublic)
                ps.setObject(5, OffsetDateTimeUtc(e.createdAt))
                ps.setObject(6, e.userId)
                ps.setObject(7, e.modelTemplateId)
                ps.executeUpdate()
            }
        }
    }

    private fun replaceParameterSet(conn: Connection, experimentId: UUID, ps: ParameterSet?) {
        conn.prepareStatement("DELETE FROM parameter_sets WHERE experiment_id = ?").use { stmt ->
            stmt.setObject(1, experimentId)
            stmt.executeUpdate()
        }
        if (ps == null) return
        conn.prepareStatement(SQL_INSERT_PARAMETER_SET).use { stmt ->
            stmt.setObject(1, ps.id)
            stmt.setString(2, codec.encode(ps.values))
            stmt.setDouble(3, ps.tStart)
            stmt.setDouble(4, ps.tEnd)
            stmt.setDouble(5, ps.step)
            stmt.setObject(6, experimentId)
            stmt.executeUpdate()
        }
    }

    private fun replaceSimulationResult(conn: Connection, experimentId: UUID, sr: SimulationResult?) {
        conn.prepareStatement("DELETE FROM simulation_results WHERE experiment_id = ?").use { stmt ->
            stmt.setObject(1, experimentId)
            stmt.executeUpdate()
        }
        if (sr == null) return
        conn.prepareStatement(SQL_INSERT_SIMULATION_RESULT).use { stmt ->
            stmt.setObject(1, sr.id)
            stmt.setString(2, codec.encodeSimulationData(sr.timePoints, sr.variables))
            stmt.setObject(3, OffsetDateTimeUtc(sr.createdAt))
            stmt.setObject(4, experimentId)
            stmt.executeUpdate()
        }
    }

    companion object {
        private const val SQL_SELECT_EXPERIMENT_BASE =
            "SELECT id, name, status, is_public, created_at, user_id, model_template_id FROM experiments"
        private const val SQL_FIND_BY_ID = "$SQL_SELECT_EXPERIMENT_BASE WHERE id = ?"
        private const val SQL_FIND_PARAMETER_SET_BY_EXPERIMENT =
            "SELECT id, \"values\", t_start, t_end, step, experiment_id FROM parameter_sets WHERE experiment_id = ?"
        private const val SQL_FIND_RESULT_BY_EXPERIMENT =
            "SELECT id, data, created_at, experiment_id FROM simulation_results WHERE experiment_id = ?"
        private const val SQL_INSERT_EXPERIMENT =
            "INSERT INTO experiments (id, name, status, is_public, created_at, user_id, model_template_id) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)"
        private const val SQL_UPDATE_EXPERIMENT =
            "UPDATE experiments SET name = ?, status = ?, is_public = ?, model_template_id = ? WHERE id = ?"
        private const val SQL_INSERT_PARAMETER_SET =
            "INSERT INTO parameter_sets (id, \"values\", t_start, t_end, step, experiment_id) VALUES (?, ?, ?, ?, ?, ?)"
        private const val SQL_INSERT_SIMULATION_RESULT =
            "INSERT INTO simulation_results (id, data, created_at, experiment_id) VALUES (?, ?, ?, ?)"
    }
}
