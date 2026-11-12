package com.vargod.service.impl

import com.vargod.domain.AuthorizationException
import com.vargod.domain.EmailAlreadyTakenException
import com.vargod.domain.NotFoundException
import com.vargod.domain.Role
import com.vargod.service.UserService
import com.vargod.support.Passwords
import com.vargod.testing.BoundaryValues
import com.vargod.testing.Classical
import com.vargod.testing.DecisionTable
import com.vargod.testing.EquivalencePartitioning
import com.vargod.testing.London
import com.vargod.testing.TestTime
import com.vargod.testing.builders.aUser
import com.vargod.testing.inmemory.InMemoryUserRepository
import com.vargod.testing.mothers.UserMother
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.just
import io.mockk.runs
import io.mockk.verify
import io.qameta.allure.Epic
import io.qameta.allure.Feature
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import java.util.UUID

/**
 * AdminUserServiceImpl — тонкая обёртка над UserService с одним правилом
 * («admin не удаляет сам себя»), поэтому оба стиля собраны в одном файле
 * двумя тест-классами — так удобно сравнивать их бок о бок.
 */
@London
@Epic("Business logic")
@Feature("AdminUserService")
@DisplayName("AdminUserServiceImpl — London style (MockK)")
@ExtendWith(MockKExtension::class)
class AdminUserServiceImplLondonTest {

    @MockK private lateinit var userService: UserService

    private lateinit var sut: AdminUserServiceImpl

    private val actor = UUID.randomUUID()

    @BeforeEach
    fun setUp() {
        sut = AdminUserServiceImpl(userService)
    }

    @Test
    @EquivalencePartitioning
    fun `list - delegates to UserService listAll`() {
        // Arrange
        val users = listOf(UserMother.admin(), UserMother.researcher())
        every { userService.listAll() } returns users

        // Act
        val result = sut.list()

        // Assert
        assertThat(result).isEqualTo(users)
    }

    @Test
    @BoundaryValues
    fun `list - no users gives empty list`() {
        // Arrange
        every { userService.listAll() } returns emptyList()

        // Act
        val result = sut.list()

        // Assert
        assertThat(result).isEmpty()
    }

    @Test
    @EquivalencePartitioning
    fun `create - delegates to UserService create with the same arguments`() {
        // Arrange
        val created = aUser().asAdmin().build()
        every { userService.create("root@vargod.test", "password-1", Role.ADMIN) } returns created

        // Act
        val result = sut.create("root@vargod.test", "password-1", Role.ADMIN)

        // Assert
        assertThat(result).isSameAs(created)
    }

    @Test
    @EquivalencePartitioning
    fun `create - EmailAlreadyTakenException is propagated`() {
        // Arrange
        every { userService.create(any(), any(), any()) } throws EmailAlreadyTakenException("root@vargod.test")

        // Act
        val error = assertThrows<EmailAlreadyTakenException> { sut.create("root@vargod.test", "password-1", Role.ADMIN) }

        // Assert
        assertThat(error.message).contains("root@vargod.test")
    }

    @Test
    @DecisionTable
    fun `delete - admin deletes another user via UserService`() {
        // Arrange
        val target = UUID.randomUUID()
        every { userService.delete(target) } just runs

        // Act
        sut.delete(actor, target)

        // Assert
        verify(exactly = 1) { userService.delete(target) }
    }

    @Test
    @DecisionTable
    fun `delete - admin cannot delete own account and UserService is not called`() {
        // Arrange
        val self = actor

        // Act
        val error = assertThrows<AuthorizationException> { sut.delete(actor, self) }

        // Assert
        assertThat(error.message).contains("самого себя")
        verify(exactly = 0) { userService.delete(any()) }
    }

    @Test
    @EquivalencePartitioning
    fun `changeRole - delegates to UserService`() {
        // Arrange
        val demoted = aUser().asResearcher().build()
        every { userService.changeRole(demoted.id, Role.RESEARCHER) } returns demoted

        // Act
        val result = sut.changeRole(demoted.id, Role.RESEARCHER)

        // Assert
        assertThat(result).isSameAs(demoted)
    }

    @Test
    @EquivalencePartitioning
    fun `changeRole - NotFoundException is propagated`() {
        // Arrange
        val id = UUID.randomUUID()
        every { userService.changeRole(id, any()) } throws NotFoundException("User", id)

        // Act
        val error = assertThrows<NotFoundException> { sut.changeRole(id, Role.ADMIN) }

        // Assert
        assertThat(error.message).contains(id.toString())
    }
}
