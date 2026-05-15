package com.vargod.repository.jdbc

import com.vargod.domain.ModelTemplate
import com.vargod.domain.PersistenceException
import com.vargod.domain.Validator
import com.vargod.domain.repository.ModelTemplateRepository
import java.sql.SQLException
import java.util.UUID
import javax.sql.DataSource

class JdbcModelTemplateRepository(
    private val dataSource: DataSource,
    private val codec: JsonCodec = JsonCodec(),
) : ModelTemplateRepository {

    override fun findAll(): List<ModelTemplate> = try {
        dataSource.connection.use { conn ->
            val rows = mutableListOf<Pair<UUID, (List<Validator>) -> ModelTemplate>>()
            conn.prepareStatement(SQL_FIND_ALL).use { ps ->
                ps.executeQuery().use { rs ->
                    while (rs.next()) {
                        val id = rs.readUuid("id")
                        val code = rs.getString("code")
                        val name = rs.getString("name")
                        val description = rs.getString("description")
                        val solverType = rs.getString("solver_type")
                        rows += id to { validators ->
                            ModelTemplate(id, code, name, description, solverType, validators)
                        }
                    }
                }
            }
            val byModel = if (rows.isEmpty()) emptyMap() else loadValidatorsForModels(conn, rows.map { it.first })
            rows.map { (id, build) -> build(byModel[id] ?: emptyList()) }
        }
    } catch (e: SQLException) {
        throw PersistenceException("Failed to list model templates: ${e.message}", e)
    }

    override fun findById(id: UUID): ModelTemplate? = try {
        dataSource.connection.use { conn ->
            val row = conn.prepareStatement(SQL_FIND_BY_ID).use { ps ->
                ps.setObject(1, id)
                ps.executeQuery().use { rs ->
                    if (rs.next()) ModelTemplateRow(
                        id = rs.readUuid("id"),
                        code = rs.getString("code"),
                        name = rs.getString("name"),
                        description = rs.getString("description"),
                        solverType = rs.getString("solver_type"),
                    ) else null
                }
            }
            row?.let { ModelTemplate(it.id, it.code, it.name, it.description, it.solverType, loadValidatorsForModel(conn, it.id)) }
        }
    } catch (e: SQLException) {
        throw PersistenceException("Failed to find model template id=$id: ${e.message}", e)
    }

    private data class ModelTemplateRow(
        val id: UUID,
        val code: String,
        val name: String,
        val description: String?,
        val solverType: String,
    )

    private fun loadValidatorsForModel(conn: java.sql.Connection, modelId: UUID): List<Validator> {
        conn.prepareStatement(SQL_FIND_VALIDATORS_BY_MODEL).use { ps ->
            ps.setObject(1, modelId)
            ps.executeQuery().use { rs ->
                val out = mutableListOf<Validator>()
                while (rs.next()) out += rs.readValidator(codec)
                return out
            }
        }
    }

    private fun loadValidatorsForModels(
        conn: java.sql.Connection,
        modelIds: List<UUID>,
    ): Map<UUID, List<Validator>> {
        val placeholders = modelIds.joinToString(",") { "?" }
        val sql = "SELECT id, name, check_type, target_kind, target_field, rules, model_template_id " +
            "FROM validators WHERE model_template_id IN ($placeholders)"
        conn.prepareStatement(sql).use { ps ->
            modelIds.forEachIndexed { i, uuid -> ps.setObject(i + 1, uuid) }
            ps.executeQuery().use { rs ->
                val byModel = mutableMapOf<UUID, MutableList<Validator>>()
                while (rs.next()) {
                    val modelId = rs.readUuid("model_template_id")
                    byModel.getOrPut(modelId) { mutableListOf() } += rs.readValidator(codec)
                }
                return byModel
            }
        }
    }

    companion object {
        private const val SQL_FIND_ALL =
            "SELECT id, code, name, description, solver_type FROM model_templates ORDER BY code"
        private const val SQL_FIND_BY_ID =
            "SELECT id, code, name, description, solver_type FROM model_templates WHERE id = ?"
        private const val SQL_FIND_VALIDATORS_BY_MODEL =
            "SELECT id, name, check_type, target_kind, target_field, rules, model_template_id " +
                "FROM validators WHERE model_template_id = ?"
    }
}
