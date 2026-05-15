package com.vargod.ui.cli.bootstrap

import com.vargod.domain.PersistenceException
import com.vargod.engine.ode.LogisticGrowthModel
import com.vargod.engine.ode.LotkaVolterraModel
import com.vargod.engine.ode.SirModel
import com.vargod.repository.jdbc.JsonCodec
import java.sql.Connection
import java.sql.SQLException
import java.util.UUID
import javax.sql.DataSource

class DataSeeder(
    private val dataSource: DataSource,
    private val codec: JsonCodec = JsonCodec(),
) {
    fun seedIfEmpty() {
        try {
            dataSource.connection.use { conn ->
                if (modelTemplatesExist(conn)) return
                conn.autoCommit = false
                try {
                    seedSir(conn)
                    seedLotkaVolterra(conn)
                    seedLogistic(conn)
                    conn.commit()
                } catch (e: SQLException) {
                    conn.rollback()
                    throw PersistenceException("Failed to seed model templates: ${e.message}", e)
                } finally {
                    conn.autoCommit = true
                }
            }
        } catch (e: SQLException) {
            throw PersistenceException("Failed to obtain connection for seeding: ${e.message}", e)
        }
    }

    private fun modelTemplatesExist(conn: Connection): Boolean =
        conn.prepareStatement("SELECT 1 FROM model_templates LIMIT 1").use { ps ->
            ps.executeQuery().use { it.next() }
        }

    private fun seedSir(conn: Connection) {
        val id = UUID.randomUUID()
        insertModel(
            conn, id, SirModel.CODE,
            name = "SIR — модель эпидемии",
            description = "S/I/R: восприимчивые, заражённые, выздоровевшие; контактный коэффициент β и скорость выздоровления γ.",
        )
        insertRange(conn, id, "beta", min = 0.0, max = 10.0)
        insertRange(conn, id, "gamma", min = 0.0, max = 10.0)
        insertRange(conn, id, "N", min = 0.0, minExclusive = true)
        insertRange(conn, id, "S", min = 0.0)
        insertRange(conn, id, "I", min = 0.0)
        insertRange(conn, id, "R", min = 0.0)
        insertConstraint(
            conn, id, "S+I+R=N",
            mapOf(
                "type" to "SUM_EQUALS_FIELD",
                "fields" to listOf("S", "I", "R"),
                "expectedField" to "N",
                "tolerance" to 1e-6,
            ),
        )
    }

    private fun seedLotkaVolterra(conn: Connection) {
        val id = UUID.randomUUID()
        insertModel(
            conn, id, LotkaVolterraModel.CODE,
            name = "Lotka-Volterra — хищник-жертва",
            description = "Динамика популяций жертв (x) и хищников (y) с коэффициентами α/β/γ/δ.",
        )
        listOf("alpha", "beta", "gamma", "delta", "x", "y").forEach {
            insertRange(conn, id, it, min = 0.0)
        }
    }

    private fun seedLogistic(conn: Connection) {
        val id = UUID.randomUUID()
        insertModel(
            conn, id, LogisticGrowthModel.CODE,
            name = "Логистический рост",
            description = "Популяция P с коэффициентом роста r и ёмкостью K.",
        )
        insertRange(conn, id, "r", min = 0.0)
        insertRange(conn, id, "K", min = 0.0, minExclusive = true)
        insertRange(conn, id, "P", min = 0.0)
    }

    private fun insertModel(
        conn: Connection,
        id: UUID,
        code: String,
        name: String,
        description: String?,
        solverType: String = "RK45",
    ) {
        conn.prepareStatement(
            "INSERT INTO model_templates (id, code, name, description, solver_type) VALUES (?, ?, ?, ?, ?)"
        ).use { ps ->
            ps.setObject(1, id)
            ps.setString(2, code)
            ps.setString(3, name)
            ps.setString(4, description)
            ps.setString(5, solverType)
            ps.executeUpdate()
        }
    }

    private fun insertRange(
        conn: Connection,
        modelId: UUID,
        targetField: String,
        min: Double? = null,
        max: Double? = null,
        minExclusive: Boolean = false,
        maxExclusive: Boolean = false,
    ) {
        val rules = buildMap<String, Any> {
            min?.let { put("min", it) }
            max?.let { put("max", it) }
            if (minExclusive) put("minExclusive", true)
            if (maxExclusive) put("maxExclusive", true)
        }
        insertValidator(
            conn,
            modelId = modelId,
            name = "range[$targetField]",
            checkType = "RANGE",
            targetField = targetField,
            rules = rules,
        )
    }

    private fun insertConstraint(
        conn: Connection,
        modelId: UUID,
        name: String,
        rules: Map<String, Any>,
        targetField: String = "*",
    ) {
        insertValidator(
            conn,
            modelId = modelId,
            name = name,
            checkType = "CONSTRAINT",
            targetField = targetField,
            rules = rules,
        )
    }

    private fun insertValidator(
        conn: Connection,
        modelId: UUID,
        name: String,
        checkType: String,
        targetField: String,
        rules: Map<String, Any>,
    ) {
        conn.prepareStatement(
            "INSERT INTO validators (id, name, check_type, target_kind, target_field, rules, model_template_id) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)"
        ).use { ps ->
            ps.setObject(1, UUID.randomUUID())
            ps.setString(2, name)
            ps.setString(3, checkType)
            ps.setString(4, "PARAMETER")
            ps.setString(5, targetField)
            ps.setString(6, codec.encode(rules))
            ps.setObject(7, modelId)
            ps.executeUpdate()
        }
    }
}
