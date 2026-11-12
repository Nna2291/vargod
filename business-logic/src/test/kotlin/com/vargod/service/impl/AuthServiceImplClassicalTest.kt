package com.vargod.service.impl

import com.vargod.domain.EmailAlreadyTakenException
import com.vargod.domain.InvalidCredentialsException
import com.vargod.domain.Role
import com.vargod.support.Passwords
import com.vargod.testing.Classical
import com.vargod.testing.EquivalencePartitioning
import com.vargod.testing.ErrorGuessing
import com.vargod.testing.TestTime
import com.vargod.testing.inmemory.InMemoryUserRepository
import com.vargod.testing.mothers.UserMother
import io.qameta.allure.Epic
import io.qameta.allure.Feature
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

@Classical
@Epic("Business logic")
@Feature("AuthService")
@DisplayName("AuthServiceImpl — classical style (real UserService + in-memory repository)")
class AuthServiceImplClassicalTest {

    private lateinit var users: InMemoryUserRepository
    private lateinit var sut: AuthServiceImpl

    @BeforeEach
    fun setUp() {
        users = InMemoryUserRepository()
        val userService = UserServiceImpl(users, bcryptCost = Passwords.TEST_COST, clock = TestTime.FIXED_CLOCK)
        sut = AuthServiceImpl(users, userService)
    }

    @Test
    @EquivalencePartitioning
    fun `register - default role is RESEARCHER and user can log in afterwards`() {
        // Arrange
        val email = "new@vargod.test"

        // Act
        val registered = sut.register(email, "password-1")

        // Assert
        assertThat(registered.role).isEqualTo(Role.RESEARCHER)
        assertThat(users.findByEmail(email)).isEqualTo(registered)
        assertThat(sut.authenticate(email, "password-1")).isEqualTo(registered)
    }

    @Test
    @EquivalencePartitioning
    fun `register - second registration with the same email is refused`() {
        // Arrange
        sut.register("dup@vargod.test", "password-1")

        // Act
        val error = assertThrows<EmailAlreadyTakenException> { sut.register("dup@vargod.test", "password-2") }

        // Assert
        assertThat(error.message).contains("dup@vargod.test")
        assertThat(users.count()).isEqualTo(1)
    }

    @Test
    @EquivalencePartitioning
    fun `authenticate - email lookup is case-insensitive`() {
        // Arrange
        val registered = sut.register("Mixed.Case@vargod.test", "password-1")

        // Act
        val result = sut.authenticate("mixed.case@VARGOD.test", "password-1")

        // Assert
        assertThat(result).isEqualTo(registered)
    }

    @Test
    @EquivalencePartitioning
    fun `authenticate - wrong password is refused`() {
        // Arrange
        sut.register("bob@vargod.test", "password-1")

        // Act
        val error = assertThrows<InvalidCredentialsException> { sut.authenticate("bob@vargod.test", "password-2") }

        // Assert
        assertThat(error.message).isEqualTo("Invalid email or password")
    }

    @Test
    @ErrorGuessing
    fun `authenticate - legacy user with invalid hash cannot log in`() {
        // Arrange
        val legacy = users.save(UserMother.legacyWithInvalidHash())

        // Act
        val error = assertThrows<InvalidCredentialsException> { sut.authenticate(legacy.email, "!INVALID") }

        // Assert
        assertThat(error.message).isEqualTo("Invalid email or password")
    }
}
