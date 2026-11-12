package com.vargod.repository.jdbc

import com.vargod.domain.PersistenceException
import com.vargod.domain.Role
import com.vargod.repository.jdbc.support.DbTestBase
import com.vargod.testing.EquivalencePartitioning
import com.vargod.testing.ErrorGuessing
import com.vargod.testing.TestTime
import com.vargod.testing.builders.aUser
import com.vargod.testing.mothers.UserMother
import io.qameta.allure.Feature
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.UUID

@Feature("UserRepository")
@DisplayName("JdbcUserRepository — classical style (embedded PostgreSQL)")
class JdbcUserRepositoryTest : DbTestBase() {

    private lateinit var sut: JdbcUserRepository

    @BeforeEach
    fun setUp() {
        sut = JdbcUserRepository(dataSource)
    }

    @Test
    @EquivalencePartitioning
    fun `findById - returns stored user`() {
        // Arrange
        val alice = givenUser(UserMother.researcher())

        // Act
        val result = sut.findById(alice.id)

        // Assert
        assertThat(result).isEqualTo(alice)
    }

    @Test
    @EquivalencePartitioning
    fun `findById - unknown id gives null`() {
        // Arrange
        givenUser()

        // Act
        val result = sut.findById(UUID.randomUUID())

        // Assert
        assertThat(result).isNull()
    }

    @Test
    @EquivalencePartitioning
    fun `findByEmail - search ignores letter case`() {
        // Arrange
        val alice = givenUser(aUser().withEmail("Alice@Vargod.test").build())

        // Act
        val result = sut.findByEmail("alice@VARGOD.TEST")

        // Assert
        assertThat(result).isEqualTo(alice)
    }

    @Test
    @EquivalencePartitioning
    fun `findByEmail - unknown email gives null`() {
        // Arrange
        givenUser(UserMother.researcher())

        // Act
        val result = sut.findByEmail("nobody@vargod.test")

        // Assert
        assertThat(result).isNull()
    }

    @Test
    @EquivalencePartitioning
    fun `findAll - users are ordered by registration time`() {
        // Arrange
        val later = givenUser(aUser().createdAt(TestTime.minutesAfterNow(10)).build())
        val earlier = givenUser(aUser().createdAt(TestTime.minutesAfterNow(1)).build())

        // Act
        val result = sut.findAll()

        // Assert
        assertThat(result).containsExactly(earlier, later)
    }

    @Test
    @EquivalencePartitioning
    fun `findAll - empty table gives empty list`() {
        // Arrange
        val noUsers = rowCount("users")

        // Act
        val result = sut.findAll()

        // Assert
        assertThat(noUsers).isZero()
        assertThat(result).isEmpty()
    }

    @Test
    @EquivalencePartitioning
    fun `existsAdmin - true when at least one admin is stored`() {
        // Arrange
        givenUser(UserMother.researcher())
        givenUser(UserMother.admin())

        // Act
        val result = sut.existsAdmin()

        // Assert
        assertThat(result).isTrue()
    }

    @Test
    @EquivalencePartitioning
    fun `existsAdmin - false when only researchers are stored`() {
        // Arrange
        givenUser(UserMother.researcher())

        // Act
        val result = sut.existsAdmin()

        // Assert
        assertThat(result).isFalse()
    }

    @Test
    @EquivalencePartitioning
    fun `save - second save with the same id updates email and role`() {
        // Arrange
        val original = sut.save(UserMother.researcher())
        val changed = original.copy(email = "renamed@vargod.test", role = Role.ADMIN)

        // Act
        sut.save(changed)

        // Assert
        assertThat(sut.findById(original.id)).isEqualTo(changed)
        assertThat(rowCount("users")).isEqualTo(1)
    }

    @Test
    @ErrorGuessing
    fun `save - email differing only in case violates unique index`() {
        // Arrange
        sut.save(aUser().withEmail("dup@vargod.test").build())
        val duplicate = aUser().withEmail("DUP@vargod.test").build()

        // Act
        val error = assertThrows<PersistenceException> { sut.save(duplicate) }

        // Assert
        assertThat(error.message).contains("users_email_lower_uniq")
    }

    @Test
    @EquivalencePartitioning
    fun `updateRole - returns user with the new role`() {
        // Arrange
        val alice = givenUser(UserMother.researcher())

        // Act
        val result = sut.updateRole(alice.id, Role.ADMIN)

        // Assert
        assertThat(result).isEqualTo(alice.copy(role = Role.ADMIN))
    }

    @Test
    @ErrorGuessing
    fun `updateRole - unknown user gives PersistenceException`() {
        // Arrange
        val id = UUID.randomUUID()

        // Act
        val error = assertThrows<PersistenceException> { sut.updateRole(id, Role.ADMIN) }

        // Assert
        assertThat(error.message).contains("User not found for role update")
    }

    @Test
    @EquivalencePartitioning
    fun `deleteById - removes the user`() {
        // Arrange
        val alice = givenUser(UserMother.researcher())

        // Act
        sut.deleteById(alice.id)

        // Assert
        assertThat(sut.findById(alice.id)).isNull()
    }

    @Test
    @ErrorGuessing
    fun `deleteById - unknown id does not touch other users`() {
        // Arrange
        givenUser(UserMother.researcher())

        // Act
        sut.deleteById(UUID.randomUUID())

        // Assert
        assertThat(rowCount("users")).isEqualTo(1)
    }
}
