package com.vargod.repository.jdbc

import com.vargod.domain.PersistenceException
import com.vargod.repository.jdbc.support.DbTestBase
import com.vargod.testing.EquivalencePartitioning
import com.vargod.testing.ErrorGuessing
import com.vargod.testing.London
import io.mockk.every
import io.mockk.mockk
import io.qameta.allure.Epic
import io.qameta.allure.Feature
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.sql.SQLException
import javax.sql.DataSource

@Feature("Schema migrations")
@DisplayName("SchemaInitializer — classical style (embedded PostgreSQL)")
class SchemaInitializerTest : DbTestBase() {

    @Test
    @EquivalencePartitioning
    fun `initialize - repeated run on migrated database is a no-op`() {
        // Arrange
        val sut = SchemaInitializer(dataSource)

        // Act
        sut.initialize()

        // Assert
        val applied = dataSource.connection.use { conn ->
            conn.createStatement().use { st ->
                st.executeQuery("SELECT COUNT(*) FROM flyway_schema_history WHERE success").use { rs -> rs.next(); rs.getInt(1) }
            }
        }
        assertThat(applied).isEqualTo(7)
    }
}

@London
@Epic("Data access")
@Feature("Schema migrations")
@DisplayName("SchemaInitializer — London style (mocked DataSource)")
class SchemaInitializerLondonTest {

    @Test
    @ErrorGuessing
    fun `initialize - unreachable database gives PersistenceException`() {
        // Arrange
        val dataSource = mockk<DataSource>(relaxed = true)
        every { dataSource.connection } throws SQLException("Connection refused")
        val sut = SchemaInitializer(dataSource)

        // Act
        val error = assertThrows<PersistenceException> { sut.initialize() }

        // Assert
        assertThat(error.message).startsWith("Schema migration failed")
    }
}
