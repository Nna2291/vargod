package com.vargod.repository.jdbc

import com.vargod.domain.PersistenceException
import com.vargod.domain.Validator
import com.vargod.domain.repository.ValidatorRepository
import java.sql.SQLException
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID
import javax.sql.DataSource

/**
 * JDBC-CRUD валидаторов с метаданными created_at/updated_at/created_by (V5).
 *
 * Параллелен [JdbcModelTemplateRepository.loadValidatorsForModel] — мы не
 * убираем оттуда загрузку валидаторов: чтение модели как агрегата с её
 * валидаторами остаётся самостоятельной операцией.
 */
class JdbcValidatorRepository(
    private val dataSource: DataSource,
    private val codec: JsonCodec = JsonCodec(),
) : ValidatorRepository {

    override fun findAll(modelTemplateId: UUID?): List<Validator> = wrap("findAll") {
        val (where, binds) = if (modelTemplateId == null) {
            "" to emptyList<Any>()
        } else {
            " WHERE model_template_id = ?" to listOf(modelTemplateId)
        }
        dataSource.connection.use { conn ->
            conn.prepareStatement("SELECT $COLS FROM validators$where ORDER BY created_at").use { ps ->
                binds.forEachIndexed { i, v -> ps.setObject(i + 1, v) }
                ps.executeQuery().use { rs ->
                    val out = mutableListOf<Validator>()
                    while (rs.next()) out += rs.readValidator(codec)
                    out
                }
            }
        }
    }

    override fun findById(id: UUID): Validator? = wrap("findById") {
        dataSource.connection.use { conn ->
            conn.prepareStatement("SELECT $COLS FROM validators WHERE id = ?").use { ps ->
                ps.setObject(1, id)
                ps.executeQuery().use { rs ->
                    if (rs.next()) rs.readValidator(codec) else null
                }
            }
        }
    }

    override fun save(validator: Validator, modelTemplateId: UUID, createdBy: UUID?): Validator =
        wrap("save") {
            val now = OffsetDateTime.now(ZoneOffset.UTC)
            dataSource.connection.use { conn ->
                conn.prepareStatement(SQL_INSERT).use { ps ->
                    ps.setObject(1, validator.id)
                    ps.setString(2, validator.name)
                    ps.setString(3, validator.checkType.name)
                    ps.setString(4, validator.targetKind.name)
                    ps.setString(5, validator.targetField)
                    ps.setString(6, codec.encode(validator.rules))
                    ps.setObject(7, modelTemplateId)
                    ps.setObject(8, now)
                    ps.setObject(9, now)
                    ps.setObject(10, createdBy)
                    ps.executeUpdate()
                }
            }
            validator
        }

    override fun update(validator: Validator): Validator = wrap("update") {
        val now = OffsetDateTime.now(ZoneOffset.UTC)
        dataSource.connection.use { conn ->
            conn.prepareStatement(SQL_UPDATE).use { ps ->
                ps.setString(1, validator.name)
                ps.setString(2, validator.checkType.name)
                ps.setString(3, validator.targetKind.name)
                ps.setString(4, validator.targetField)
                ps.setString(5, codec.encode(validator.rules))
                ps.setObject(6, now)
                ps.setObject(7, validator.id)
                if (ps.executeUpdate() == 0) {
                    throw PersistenceException("Validator not found for update: ${validator.id}")
                }
            }
        }
        validator
    }

    override fun deleteById(id: UUID) = wrap("deleteById") {
        dataSource.connection.use { conn ->
            conn.prepareStatement("DELETE FROM validators WHERE id = ?").use { ps ->
                ps.setObject(1, id)
                ps.executeUpdate()
            }
        }
        Unit
    }

    private fun <T> wrap(label: String, block: () -> T): T = try {
        block()
    } catch (e: SQLException) {
        throw PersistenceException("JdbcValidatorRepository.$label failed: ${e.message}", e)
    }

    private companion object {
        // ResultSetMappers.readValidator читает именно эти колонки.
        const val COLS = "id, name, check_type, target_kind, target_field, rules"
        const val SQL_INSERT =
            "INSERT INTO validators (id, name, check_type, target_kind, target_field, rules, " +
                "model_template_id, created_at, updated_at, created_by) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"
        const val SQL_UPDATE =
            "UPDATE validators SET name = ?, check_type = ?, target_kind = ?, " +
                "target_field = ?, rules = ?, updated_at = ? WHERE id = ?"
    }
}
