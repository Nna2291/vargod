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
                        val maxStep = rs.readNullableDouble("recommended_max_step")
                        val relTol = rs.readNullableDouble("recommended_rel_tol")
                        val absTol = rs.readNullableDouble("recommended_abs_tol")
                        rows += id to { validators ->
                            ModelTemplate(
                                id, code, name, description, solverType, validators,
                                recommendedMaxStep = maxStep,
                                recommendedRelTol = relTol,
                                recommendedAbsTol = absTol,
                            )
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

    override fun save(template: ModelTemplate): ModelTemplate = try {
        // UPSERT по id: либо вставка нового шаблона, либо обновление метаданных.
        // Валидаторы сюда не пишем — у них своя таблица и свой репозиторий.
        dataSource.connection.use { conn ->
            conn.prepareStatement(SQL_UPSERT).use { ps ->
                ps.setObject(1, template.id)
                ps.setString(2, template.code)
                ps.setString(3, template.name)
                ps.setString(4, template.description)
                ps.setString(5, template.solverType)
                setNullableDouble(ps, 6, template.recommendedMaxStep)
                setNullableDouble(ps, 7, template.recommendedRelTol)
                setNullableDouble(ps, 8, template.recommendedAbsTol)
                ps.executeUpdate()
            }
        }
        template
    } catch (e: SQLException) {
        throw PersistenceException("Failed to save model template id=${template.id}: ${e.message}", e)
    }

    override fun deleteById(id: UUID): Unit = try {
        dataSource.connection.use { conn ->
            conn.prepareStatement("DELETE FROM model_templates WHERE id = ?").use { ps ->
                ps.setObject(1, id)
                ps.executeUpdate()
                Unit
            }
        }
    } catch (e: SQLException) {
        throw PersistenceException("Failed to delete model template id=$id: ${e.message}", e)
    }

    private fun setNullableDouble(ps: java.sql.PreparedStatement, idx: Int, value: Double?) {
        if (value == null) ps.setNull(idx, java.sql.Types.DOUBLE) else ps.setDouble(idx, value)
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
                        recommendedMaxStep = rs.readNullableDouble("recommended_max_step"),
                        recommendedRelTol = rs.readNullableDouble("recommended_rel_tol"),
                        recommendedAbsTol = rs.readNullableDouble("recommended_abs_tol"),
                    ) else null
                }
            }
            row?.let {
                ModelTemplate(
                    it.id, it.code, it.name, it.description, it.solverType,
                    validators = loadValidatorsForModel(conn, it.id),
                    recommendedMaxStep = it.recommendedMaxStep,
                    recommendedRelTol = it.recommendedRelTol,
                    recommendedAbsTol = it.recommendedAbsTol,
                )
            }
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
        val recommendedMaxStep: Double?,
        val recommendedRelTol: Double?,
        val recommendedAbsTol: Double?,
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
            "SELECT id, code, name, description, solver_type, " +
                "recommended_max_step, recommended_rel_tol, recommended_abs_tol " +
                "FROM model_templates ORDER BY code"
        private const val SQL_FIND_BY_ID =
            "SELECT id, code, name, description, solver_type, " +
                "recommended_max_step, recommended_rel_tol, recommended_abs_tol " +
                "FROM model_templates WHERE id = ?"
        private const val SQL_FIND_VALIDATORS_BY_MODEL =
            "SELECT id, name, check_type, target_kind, target_field, rules, model_template_id " +
                "FROM validators WHERE model_template_id = ?"
        private const val SQL_UPSERT =
            "INSERT INTO model_templates (id, code, name, description, solver_type, " +
                "recommended_max_step, recommended_rel_tol, recommended_abs_tol) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?) " +
                "ON CONFLICT (id) DO UPDATE SET " +
                "code = EXCLUDED.code, name = EXCLUDED.name, description = EXCLUDED.description, " +
                "solver_type = EXCLUDED.solver_type, " +
                "recommended_max_step = EXCLUDED.recommended_max_step, " +
                "recommended_rel_tol = EXCLUDED.recommended_rel_tol, " +
                "recommended_abs_tol = EXCLUDED.recommended_abs_tol"
    }
}
