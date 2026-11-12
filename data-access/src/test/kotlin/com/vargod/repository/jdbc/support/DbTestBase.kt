package com.vargod.repository.jdbc.support

import com.vargod.domain.Experiment
import com.vargod.domain.ModelTemplate
import com.vargod.domain.User
import com.vargod.domain.Validator
import com.vargod.repository.jdbc.DataSourceFactory
import com.vargod.repository.jdbc.JsonCodec
import com.vargod.repository.jdbc.SchemaInitializer
import com.vargod.testing.Classical
import com.vargod.testing.DbTest
import com.vargod.testing.builders.aUser
import io.qameta.allure.Epic
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.parallel.ResourceLock
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID
import javax.sql.DataSource

/**
 * Один настоящий PostgreSQL 16 на тестовую JVM.
 *
 * Сервер запускается embedded-процессом (бинарники — Maven-зависимость zonky,
 * Docker не нужен), схема накатывается теми же Flyway-миграциями, что и в production.
 * DataSource строится production-фабрикой [DataSourceFactory].
 */
object TestDatabase {
    private val server: EmbeddedPostgres = EmbeddedPostgres.builder().start()

    val dataSource: DataSource = DataSourceFactory.postgres(
        server.getJdbcUrl("postgres", "postgres"), "postgres", "postgres",
    )

    init {
        SchemaInitializer(dataSource).initialize()
        Runtime.getRuntime().addShutdownHook(Thread { server.close() })
    }
}

/**
 * Фикстура классических тестов репозиториев: перед каждым тестом все таблицы
 * очищаются, Arrange-хелперы кладут данные «мимо» тестируемого репозитория
 * обычным SQL — чтобы проверять репозиторий, не опираясь на него же.
 *
 * @ResourceLock("postgres") сериализует тесты с БД, если включено параллельное
 * выполнение JUnit (-Pthreads=N): база одна на JVM.
 */
@Classical
@DbTest
@Epic("Data access")
@ResourceLock("postgres")
abstract class DbTestBase {

    protected val dataSource: DataSource = TestDatabase.dataSource
    protected val codec = JsonCodec()

    @BeforeEach
    fun cleanDatabase() {
        dataSource.connection.use { conn ->
            conn.createStatement().use {
                it.execute(
                    "TRUNCATE TABLE audit_log, comparison_experiments, comparisons, simulation_results, " +
                        "parameter_sets, experiments, validators, model_templates, users CASCADE",
                )
            }
        }
    }

    protected fun givenUser(user: User = aUser().build()): User {
        dataSource.connection.use { conn ->
            conn.prepareStatement(
                "INSERT INTO users (id, email, password_hash, role, created_at) VALUES (?, ?, ?, ?, ?)",
            ).use { ps ->
                ps.setObject(1, user.id)
                ps.setString(2, user.email)
                ps.setString(3, user.passwordHash)
                ps.setString(4, user.role.name)
                ps.setObject(5, OffsetDateTime.ofInstant(user.createdAt, ZoneOffset.UTC))
                ps.executeUpdate()
            }
        }
        return user
    }

    protected fun givenUserWithId(id: UUID): User = givenUser(aUser().withId(id).build())

    protected fun givenModel(model: ModelTemplate): ModelTemplate {
        dataSource.connection.use { conn ->
            conn.prepareStatement(
                "INSERT INTO model_templates (id, code, name, description, solver_type) VALUES (?, ?, ?, ?, ?)",
            ).use { ps ->
                ps.setObject(1, model.id)
                ps.setString(2, model.code)
                ps.setString(3, model.name)
                ps.setString(4, model.description)
                ps.setString(5, model.solverType)
                ps.executeUpdate()
            }
        }
        model.validators.forEach { givenValidator(it, model.id) }
        return model
    }

    protected fun givenValidator(validator: Validator, modelId: UUID): Validator {
        dataSource.connection.use { conn ->
            conn.prepareStatement(
                "INSERT INTO validators (id, name, check_type, target_kind, target_field, rules, model_template_id) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?)",
            ).use { ps ->
                ps.setObject(1, validator.id)
                ps.setString(2, validator.name)
                ps.setString(3, validator.checkType.name)
                ps.setString(4, validator.targetKind.name)
                ps.setString(5, validator.targetField)
                ps.setString(6, codec.encode(validator.rules))
                ps.setObject(7, modelId)
                ps.executeUpdate()
            }
        }
        return validator
    }

    /** Строка experiments без дочерних parameter_sets/simulation_results. */
    protected fun givenExperimentRow(experiment: Experiment): Experiment {
        dataSource.connection.use { conn ->
            conn.prepareStatement(
                "INSERT INTO experiments (id, name, status, is_public, created_at, user_id, model_template_id) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?)",
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
        return experiment
    }

    protected fun rowCount(table: String): Int = dataSource.connection.use { conn ->
        conn.createStatement().use { st ->
            st.executeQuery("SELECT COUNT(*) FROM $table").use { rs -> rs.next(); rs.getInt(1) }
        }
    }
}
