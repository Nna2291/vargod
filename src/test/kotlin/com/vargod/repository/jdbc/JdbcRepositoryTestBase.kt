package com.vargod.repository.jdbc

import com.vargod.domain.Experiment
import com.vargod.domain.ModelTemplate
import com.vargod.domain.Validator
import org.junit.jupiter.api.BeforeEach
import java.sql.Connection
import java.time.OffsetDateTime
import java.time.ZoneOffset
import javax.sql.DataSource


abstract class JdbcRepositoryTestBase {

    protected val dataSource: DataSource = TestPostgres.dataSource
    protected val codec: JsonCodec = JsonCodec()
    protected val experimentRepository = JdbcExperimentRepository(dataSource, codec)
    protected val modelTemplateRepository = JdbcModelTemplateRepository(dataSource, codec)
    protected val comparisonRepository = JdbcComparisonRepository(dataSource, experimentRepository)

    @BeforeEach
    fun truncateAllTables() {
        dataSource.connection.use { conn ->
            conn.createStatement().use { stmt ->
                stmt.execute(
                    "TRUNCATE TABLE comparison_experiments, comparisons, simulation_results, " +
                        "parameter_sets, experiments, validators, model_templates CASCADE"
                )
            }
        }
    }

    protected fun seedModelTemplate(model: ModelTemplate) {
        dataSource.connection.use { conn ->
            conn.prepareStatement(
                "INSERT INTO model_templates (id, code, name, description, solver_type) VALUES (?, ?, ?, ?, ?)"
            ).use { ps ->
                ps.setObject(1, model.id)
                ps.setString(2, model.code)
                ps.setString(3, model.name)
                ps.setString(4, model.description)
                ps.setString(5, model.solverType)
                ps.executeUpdate()
            }
            model.validators.forEach { seedValidator(conn, it, model.id) }
        }
    }

    private fun seedValidator(conn: Connection, validator: Validator, modelTemplateId: java.util.UUID) {
        conn.prepareStatement(
            "INSERT INTO validators (id, name, check_type, target_kind, target_field, rules, model_template_id) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)"
        ).use { ps ->
            ps.setObject(1, validator.id)
            ps.setString(2, validator.name)
            ps.setString(3, validator.checkType.name)
            ps.setString(4, validator.targetKind.name)
            ps.setString(5, validator.targetField)
            ps.setString(6, codec.encode(validator.rules))
            ps.setObject(7, modelTemplateId)
            ps.executeUpdate()
        }
    }

    protected fun seedExperimentShell(experiment: Experiment) {
        dataSource.connection.use { conn ->
            conn.prepareStatement(
                "INSERT INTO experiments (id, name, status, is_public, created_at, user_id, model_template_id) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?)"
            ).use { ps ->
                ps.setObject(1, experiment.id)
                ps.setString(2, experiment.name)
                ps.setString(3, experiment.status.name)
                ps.setBoolean(4, experiment.isPublic)
                ps.setObject(5, OffsetDateTime.ofInstant(experiment.createdAt, ZoneOffset.UTC))
                ps.setObject(6, experiment.userId)
                ps.setObject(7, experiment.modelTemplateId)
                ps.executeUpdate()
            }
        }
    }

    protected fun rowCount(table: String): Int = dataSource.connection.use { conn ->
        conn.createStatement().use { stmt ->
            stmt.executeQuery("SELECT COUNT(*) FROM $table").use { rs ->
                rs.next(); rs.getInt(1)
            }
        }
    }
}
