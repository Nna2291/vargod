package com.vargod.repository.jdbc

import org.postgresql.ds.PGSimpleDataSource
import javax.sql.DataSource

/**
 * Builds a PostgreSQL DataSource for the data access component. Repository
 * implementations depend only on javax.sql.DataSource — swapping in any other
 * JDBC driver requires changes only here.
 */
object DataSourceFactory {

    fun postgres(jdbcUrl: String, user: String, password: String): DataSource =
        PGSimpleDataSource().apply {
            setURL(jdbcUrl)
            this.user = user
            this.password = password
        }
}
