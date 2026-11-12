package com.vargod.repository.jdbc

import com.vargod.domain.PersistenceException
import com.vargod.domain.Role
import com.vargod.testing.ErrorGuessing
import com.vargod.testing.EquivalencePartitioning
import com.vargod.testing.London
import com.vargod.testing.builders.anExperiment
import com.vargod.testing.mothers.UserMother
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
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID
import javax.sql.DataSource

/**
 * Лондонский стиль для слоя доступа к данным: вся JDBC-цепочка
 * DataSource → Connection → PreparedStatement → ResultSet заменена mock'ами.
 * Работает без PostgreSQL и без сети; проверяет то, что на настоящей БД
 * воспроизвести трудно: обработку SQLException, откат транзакции, привязку параметров.
 */
@London
@Epic("Data access")
@Feature("UserRepository")
@DisplayName("JdbcUserRepository — London style (mocked JDBC)")
@ExtendWith(MockKExtension::class)
class JdbcUserRepositoryLondonTest {

    @MockK private lateinit var dataSource: DataSource
    @MockK(relaxUnitFun = true) private lateinit var connection: Connection
    @MockK(relaxUnitFun = true) private lateinit var statement: PreparedStatement
    @MockK(relaxUnitFun = true) private lateinit var resultSet: ResultSet

    private lateinit var sut: JdbcUserRepository

    @BeforeEach
    fun setUp() {
        every { dataSource.connection } returns connection
        every { connection.prepareStatement(any()) } returns statement
        every { statement.executeQuery() } returns resultSet
        sut = JdbcUserRepository(dataSource)
    }

    @Test
    @EquivalencePartitioning
    fun `findById - binds id as the only parameter and maps the row`() {
        // Arrange
        val alice = UserMother.researcher()
        every { resultSet.next() } returns true
        every { resultSet.getObject("id", UUID::class.java) } returns alice.id
        every { resultSet.getString("email") } returns alice.email
        every { resultSet.getString("password_hash") } returns alice.passwordHash
        every { resultSet.getString("role") } returns "RESEARCHER"
        every { resultSet.getObject("created_at", OffsetDateTime::class.java) } returns alice.createdAt.atOffset(ZoneOffset.UTC)

        // Act
        val result = sut.findById(alice.id)

        // Assert
        assertThat(result).isEqualTo(alice)
        verify(exactly = 1) { statement.setObject(1, alice.id) }
        verify { connection.close() }
    }

    @Test
    @ErrorGuessing
    fun `findById - SQLException is wrapped into PersistenceException with cause`() {
        // Arrange
        val dbFailure = SQLException("connection reset")
        every { statement.executeQuery() } throws dbFailure

        // Act
        val error = assertThrows<PersistenceException> { sut.findById(UUID.randomUUID()) }

        // Assert
        assertThat(error.message).isEqualTo("JdbcUserRepository.query failed: connection reset")
        assertThat(error.cause).isSameAs(dbFailure)
    }

    @Test
    @EquivalencePartitioning
    fun `updateRole - binds role name before id`() {
        // Arrange
        val id = UUID.randomUUID()
        every { statement.executeUpdate() } returns 1
        every { resultSet.next() } returns true
        every { resultSet.getObject("id", UUID::class.java) } returns id
        every { resultSet.getString(any<String>()) } answers { if (firstArg<String>() == "role") "ADMIN" else "x" }
        every { resultSet.getObject("created_at", OffsetDateTime::class.java) } returns OffsetDateTime.now(ZoneOffset.UTC)

        // Act
        val result = sut.updateRole(id, Role.ADMIN)

        // Assert
        assertThat(result.role).isEqualTo(Role.ADMIN)
        verifyOrder {
            statement.setString(1, "ADMIN")
            statement.setObject(2, id)
        }
    }

    @Test
    @ErrorGuessing
    fun `updateRole - zero updated rows means unknown user`() {
        // Arrange
        val id = UUID.randomUUID()
        every { statement.executeUpdate() } returns 0

        // Act
        val error = assertThrows<PersistenceException> { sut.updateRole(id, Role.ADMIN) }

        // Assert
        assertThat(error.message).contains("User not found for role update: $id")
        verify(exactly = 0) { statement.executeQuery() }
    }
}

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

    @Test
    @EquivalencePartitioning
    fun `findAllByIds - empty id list does not open a connection`() {
        // Arrange
        val noIds = emptyList<UUID>()

        // Act
        val result = sut.findAllByIds(noIds)

        // Assert
        assertThat(result).isEmpty()
        verify(exactly = 0) { dataSource.connection }
    }

    @Test
    @ErrorGuessing
    fun `deleteById - unavailable database gives PersistenceException`() {
        // Arrange
        val id = UUID.randomUUID()
        every { dataSource.connection } throws SQLException("Connection refused")

        // Act
        val error = assertThrows<PersistenceException> { sut.deleteById(id) }

        // Assert
        assertThat(error.message).isEqualTo("Failed to delete experiment id=$id: Connection refused")
    }
}
