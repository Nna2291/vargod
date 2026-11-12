package com.vargod.service.impl

import com.vargod.domain.EmailAlreadyTakenException
import com.vargod.domain.NotFoundException
import com.vargod.domain.PersistenceException
import com.vargod.domain.Role
import com.vargod.domain.User
import com.vargod.domain.repository.UserRepository
import com.vargod.support.Passwords
import com.vargod.testing.BoundaryValues
import com.vargod.testing.EquivalencePartitioning
import com.vargod.testing.ErrorGuessing
import com.vargod.testing.London
import com.vargod.testing.TestTime
import com.vargod.testing.builders.aUser
import com.vargod.testing.mothers.UserMother
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.just
import io.mockk.runs
import io.mockk.slot
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
import java.util.UUID

@London
@Epic("Business logic")
@Feature("UserService")
@DisplayName("UserServiceImpl — London style (MockK)")
@ExtendWith(MockKExtension::class)
class UserServiceImplLondonTest {

    @MockK private lateinit var userRepository: UserRepository

    private lateinit var sut: UserServiceImpl

    @BeforeEach
    fun setUp() {
        sut = UserServiceImpl(userRepository, bcryptCost = Passwords.TEST_COST, clock = TestTime.FIXED_CLOCK)
    }

    // ---------------------------------------------------------------- getById / getByEmail / listAll

    @Test
    @EquivalencePartitioning
    fun `getById - returns user from repository`() {
        // Arrange
        val user = UserMother.researcher()
        every { userRepository.findById(user.id) } returns user

        // Act
        val result = sut.getById(user.id)

        // Assert
        assertThat(result).isSameAs(user)
    }

    @Test
    @EquivalencePartitioning
    fun `getById - unknown id gives NotFoundException`() {
        // Arrange
        val id = UUID.randomUUID()
        every { userRepository.findById(id) } returns null

        // Act
        val error = assertThrows<NotFoundException> { sut.getById(id) }

        // Assert
        assertThat(error.message).isEqualTo("User not found: id=$id")
    }

    @Test
    @EquivalencePartitioning
    fun `getByEmail - returns user from repository`() {
        // Arrange
        val user = UserMother.researcher()
        every { userRepository.findByEmail(user.email) } returns user

        // Act
        val result = sut.getByEmail(user.email)

        // Assert
        assertThat(result).isSameAs(user)
    }

    @Test
    @EquivalencePartitioning
    fun `getByEmail - unknown email gives null instead of exception`() {
        // Arrange
        every { userRepository.findByEmail("ghost@vargod.test") } returns null

        // Act
        val result = sut.getByEmail("ghost@vargod.test")

        // Assert
        assertThat(result).isNull()
    }

    @Test
    @EquivalencePartitioning
    fun `listAll - delegates to repository`() {
        // Arrange
        val users = listOf(UserMother.admin(), UserMother.researcher())
        every { userRepository.findAll() } returns users

        // Act
        val result = sut.listAll()

        // Assert
        assertThat(result).isEqualTo(users)
    }

    @Test
    @ErrorGuessing
    fun `listAll - repository failure is propagated`() {
        // Arrange
        every { userRepository.findAll() } throws PersistenceException("db down")

        // Act
        val error = assertThrows<PersistenceException> { sut.listAll() }

        // Assert
        assertThat(error.message).isEqualTo("db down")
    }

    // ---------------------------------------------------------------- create

    @Test
    @EquivalencePartitioning
    fun `create - checks email uniqueness then saves user with bcrypt hash`() {
        // Arrange
        val saved = slot<User>()
        every { userRepository.findByEmail("new@vargod.test") } returns null
        every { userRepository.save(capture(saved)) } answers { firstArg() }

        // Act
        val result = sut.create("new@vargod.test", "s3cret-pass", Role.RESEARCHER)

        // Assert
        assertThat(result).isEqualTo(saved.captured)
        assertThat(result.passwordHash).startsWith("\$2a\$04\$").hasSize(60)
        assertThat(Passwords.matches("s3cret-pass", result.passwordHash)).isTrue()
        assertThat(result.createdAt).isEqualTo(TestTime.NOW)
        verifyOrder {
            userRepository.findByEmail("new@vargod.test")
            userRepository.save(any())
        }
    }

    @Test
    @EquivalencePartitioning
    fun `create - taken email gives EmailAlreadyTakenException and nothing is saved`() {
        // Arrange
        every { userRepository.findByEmail("alice@vargod.test") } returns UserMother.researcher()

        // Act
        val error = assertThrows<EmailAlreadyTakenException> { sut.create("alice@vargod.test", "password-1", Role.RESEARCHER) }

        // Assert
        assertThat(error.message).contains("alice@vargod.test")
        verify(exactly = 0) { userRepository.save(any()) }
    }

    @Test
    @BoundaryValues
    fun `create - password of 7 characters is one below the minimum`() {
        // Arrange
        val sevenChars = "1234567"

        // Act
        val error = assertThrows<IllegalArgumentException> { sut.create("a@vargod.test", sevenChars, Role.RESEARCHER) }

        // Assert
        assertThat(error.message).contains("8")
        verify(exactly = 0) { userRepository.findByEmail(any()) }
    }

    // ---------------------------------------------------------------- changeRole

    @Test
    @EquivalencePartitioning
    fun `changeRole - delegates to repository and returns updated user`() {
        // Arrange
        val promoted = aUser().asAdmin().build()
        every { userRepository.updateRole(promoted.id, Role.ADMIN) } returns promoted

        // Act
        val result = sut.changeRole(promoted.id, Role.ADMIN)

        // Assert
        assertThat(result.role).isEqualTo(Role.ADMIN)
        verify(exactly = 1) { userRepository.updateRole(promoted.id, Role.ADMIN) }
    }

    @Test
    @ErrorGuessing
    fun `changeRole - unknown user error from repository is propagated`() {
        // Arrange
        val id = UUID.randomUUID()
        every { userRepository.updateRole(id, any()) } throws PersistenceException("User not found for role update: $id")

        // Act
        val error = assertThrows<PersistenceException> { sut.changeRole(id, Role.ADMIN) }

        // Assert
        assertThat(error.message).contains(id.toString())
    }

    // ---------------------------------------------------------------- delete

    @Test
    @EquivalencePartitioning
    fun `delete - existing user is looked up then deleted`() {
        // Arrange
        val user = UserMother.researcher()
        every { userRepository.findById(user.id) } returns user
        every { userRepository.deleteById(user.id) } just runs

        // Act
        sut.delete(user.id)

        // Assert
        verifyOrder {
            userRepository.findById(user.id)
            userRepository.deleteById(user.id)
        }
    }

    @Test
    @EquivalencePartitioning
    fun `delete - unknown user gives NotFoundException and deleteById is not called`() {
        // Arrange
        val id = UUID.randomUUID()
        every { userRepository.findById(id) } returns null

        // Act
        val error = assertThrows<NotFoundException> { sut.delete(id) }

        // Assert
        assertThat(error.message).contains(id.toString())
        verify(exactly = 0) { userRepository.deleteById(any()) }
    }
}
