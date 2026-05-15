package com.vargod.repository.jdbc

import com.vargod.domain.PersistenceException
import java.sql.SQLException
import javax.sql.DataSource


class SchemaInitializer(private val dataSource: DataSource) {

    fun initialize() {
        val ddl = readSchema()
        try {
            dataSource.connection.use { conn ->
                conn.createStatement().use { it.execute(ddl) }
            }
        } catch (e: SQLException) {
            throw PersistenceException("Schema initialization failed: ${e.message}", e)
        }
    }

    private fun readSchema(): String {
        val resource = javaClass.classLoader.getResource(SCHEMA_RESOURCE)
            ?: throw PersistenceException("Schema resource not found on classpath: $SCHEMA_RESOURCE")
        return resource.readText(Charsets.UTF_8)
    }

    companion object {
        const val SCHEMA_RESOURCE = "db/schema.sql"
    }
}
