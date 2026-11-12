package com.vargod.repository.jdbc.support

import com.vargod.domain.ModelTemplate
import com.vargod.repository.jdbc.DataSourceFactory
import com.vargod.repository.jdbc.JsonCodec
import com.vargod.repository.jdbc.SchemaInitializer
import com.vargod.testing.Classical
import com.vargod.testing.DbTest
import io.qameta.allure.Epic
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.parallel.ResourceLock
import java.util.UUID
import javax.sql.DataSource

/**
 * Один настоящий PostgreSQL 16 на тестовую JVM. Сервер запускается embedded-процессом
 * (бинарники приходят Maven-зависимостью, Docker не нужен), схема накатывается теми же
 * миграциями Flyway, что и в production.
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
 * Фикстура классических тестов репозиториев: перед каждым тестом таблицы очищаются,
 * а данные для Arrange кладутся обычным SQL — мимо тестируемого репозитория.
 * @ResourceLock("postgres") не даёт тестам с БД выполняться параллельно (-Pthreads=N).
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
                    "TRUNCATE TABLE comparison_experiments, comparisons, simulation_results, " +
                        "parameter_sets, experiments, validators, model_templates, users CASCADE",
                )
            }
        }
    }

    /** Пользователь с заданным id: experiments.user_id ссылается на users (FK). */
    protected fun givenUser(id: UUID) {
        dataSource.connection.use { conn ->
            conn.prepareStatement(
                "INSERT INTO users (id, email, password_hash, role, created_at) VALUES (?, ?, '!INVALID', 'RESEARCHER', NOW())",
            ).use { ps ->
                ps.setObject(1, id)
                ps.setString(2, "user-$id@vargod.test")
                ps.executeUpdate()
            }
        }
    }

    /** Шаблон модели: experiments.model_template_id ссылается на model_templates (FK). */
    protected fun givenModel(model: ModelTemplate) {
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
    }

    protected fun rowCount(table: String): Int = dataSource.connection.use { conn ->
        conn.createStatement().use { st ->
            st.executeQuery("SELECT COUNT(*) FROM $table").use { rs -> rs.next(); rs.getInt(1) }
        }
    }
}
