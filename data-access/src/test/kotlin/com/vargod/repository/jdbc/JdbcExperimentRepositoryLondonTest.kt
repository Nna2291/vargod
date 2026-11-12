package com.vargod.repository.jdbc

import com.vargod.domain.PersistenceException
import com.vargod.testing.ErrorGuessing
import com.vargod.testing.London
import com.vargod.testing.builders.anExperiment
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import io.mockk.verifyOrder
import io.qameta.allure.Epic
import io.qameta.allure.Feature
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.SQLException
import javax.sql.DataSource

/**
 * Тот же репозиторий в лондонском стиле: вся JDBC-цепочка
 * DataSource → Connection → PreparedStatement → ResultSet заменена mock'ами.
 * Работает без БД и показывает то, что на настоящем PostgreSQL воспроизвести
 * трудно: сбой посреди транзакции и её откат.
 */
@London
@Epic("Data access")
@Feature("ExperimentRepository")
@DisplayName("JdbcExperimentRepository — London style (mocked JDBC)")
@ExtendWith(MockKExtension::class)
class JdbcExperimentRepositoryLondonTest {

    @MockK private lateinit var dataSource: DataSource
    @MockK(relaxUnitFun = true) private lateinit var connection: Connection
    @MockK(relaxUnitFun = true) private lateinit var statement: PreparedStatement
    @MockK(relaxUnitFun = true) private lateinit var resultSet: ResultSet

    private lateinit var sut: JdbcExperimentRepository

    @BeforeEach
    fun setUp() {
        every { dataSource.connection } returns connection
        every { connection.prepareStatement(any()) } returns statement
        every { statement.executeQuery() } returns resultSet
        sut = JdbcExperimentRepository(dataSource)
    }

    @Test
    @ErrorGuessing
    fun `save - failure in the middle of the transaction rolls back and restores autocommit`() {
        // Arrange
        val experiment = anExperiment().build()
        every { resultSet.next() } returns false // эксперимента ещё нет -> INSERT
        every { statement.executeUpdate() } returns 1 andThenThrows SQLException("deadlock detected")

        // Act
        val error = assertThrows<PersistenceException> { sut.save(experiment) }

        // Assert
        assertThat(error.message).isEqualTo("Failed to save experiment id=${experiment.id}: deadlock detected")
        verifyOrder {
            connection.autoCommit = false
            connection.rollback()
            connection.autoCommit = true
        }
        verify(exactly = 0) { connection.commit() }
    }
}
