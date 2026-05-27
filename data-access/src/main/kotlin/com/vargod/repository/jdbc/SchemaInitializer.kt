package com.vargod.repository.jdbc

import com.vargod.domain.PersistenceException
import org.flywaydb.core.Flyway
import org.flywaydb.core.api.FlywayException
import org.slf4j.LoggerFactory
import javax.sql.DataSource


/**
 * Применяет версионированные миграции схемы из classpath:db/migration/V*__*.sql
 * через Flyway. Идемпотентно: уже применённые миграции пропускаются благодаря
 * таблице flyway_schema_history.
 *
 * baselineOnMigrate=true позволяет накатить миграции поверх уже существующей dev-БД,
 * созданной ранним вариантом без Flyway (schema.sql из SchemaInitializer).
 * baselineVersion=1 означает: если БД не пустая и flyway_schema_history отсутствует,
 * считаем V1 (исходная схема) уже применённой и стартуем с V2. Для свежей пустой
 * БД baseline не срабатывает и V1 применяется обычным порядком.
 */
class SchemaInitializer(private val dataSource: DataSource) {

    private val log = LoggerFactory.getLogger(SchemaInitializer::class.java)

    fun initialize() {
        try {
            val flyway = Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .baselineOnMigrate(true)
                .baselineVersion("1")
                .load()

            val result = flyway.migrate()
            log.info(
                "Схема БД актуализирована Flyway: применено миграций {}, начиная с {}",
                result.migrationsExecuted, result.initialSchemaVersion
            )
        } catch (e: FlywayException) {
            log.error("Ошибка миграции схемы БД: {}", e.message, e)
            throw PersistenceException("Schema migration failed: ${e.message}", e)
        }
    }
}
