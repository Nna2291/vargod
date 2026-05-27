package com.vargod.repository.jdbc

import org.testcontainers.containers.PostgreSQLContainer
import javax.sql.DataSource


object TestPostgres {

    private val container: PostgreSQLContainer<Nothing> =
        PostgreSQLContainer<Nothing>("postgres:16-alpine").apply {
            withDatabaseName("vargod_test")
            withUsername("vargod")
            withPassword("vargod")
            start()
        }

    val dataSource: DataSource = DataSourceFactory.postgres(
        container.jdbcUrl, container.username, container.password,
    )

    init {
        SchemaInitializer(dataSource).initialize()
        Runtime.getRuntime().addShutdownHook(Thread { container.stop() })
    }
}
