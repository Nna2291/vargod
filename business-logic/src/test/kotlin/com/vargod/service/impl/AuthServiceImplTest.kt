package com.vargod.service.impl

import at.favre.lib.crypto.bcrypt.BCrypt
import com.vargod.domain.InvalidCredentialsException
import com.vargod.domain.Role
import com.vargod.domain.User
import com.vargod.domain.repository.UserRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID

class AuthServiceImplTest {

    private val repo = mockk<UserRepository>(relaxUnitFun = true)
    // cost=4 — минимально допустимый для BCrypt, тесты быстрые.
    private val userService = UserServiceImpl(repo, bcryptCost = 4)
    private val auth = AuthServiceImpl(repo, userService)

    @Test
    fun `register hashes password with bcrypt and persists user`() {
        every { repo.findByEmail("a@b.c") } returns null
        val captured = slot<User>()
        every { repo.save(capture(captured)) } answers { captured.captured }

        val user = auth.register("a@b.c", "password123")

        assertEquals("a@b.c", user.email)
        assertEquals(Role.RESEARCHER, user.role)
        // BCrypt-хеши начинаются с $2 (id алгоритма) и ровно 60 символов.
        assertTrue(captured.captured.passwordHash.startsWith("\$2"))
        assertEquals(60, captured.captured.passwordHash.length)
    }

    @Test
    fun `authenticate succeeds when password matches hash`() {
        val user = userFixture("c@d.e", plainPassword = "myStrongPass")
        every { repo.findByEmail("c@d.e") } returns user

        val verified = auth.authenticate("c@d.e", "myStrongPass")
        assertNotNull(verified)
        assertEquals(user.id, verified.id)
    }

    @Test
    fun `authenticate fails with InvalidCredentials on wrong password`() {
        val user = userFixture("x@y.z", plainPassword = "correctPass")
        every { repo.findByEmail("x@y.z") } returns user

        assertThrows(InvalidCredentialsException::class.java) {
            auth.authenticate("x@y.z", "wrongPass")
        }
    }

    @Test
    fun `authenticate fails with InvalidCredentials when email not found`() {
        every { repo.findByEmail("nobody@nowhere.io") } returns null

        // Намеренно тот же exception, что и при wrong-password —
        // защита от user-enumeration через различия в ответах.
        assertThrows(InvalidCredentialsException::class.java) {
            auth.authenticate("nobody@nowhere.io", "anything")
        }
    }

    @Test
    fun `authenticate gracefully handles legacy invalid hash`() {
        // V3-миграция бэкфиллит legacy-пользователей с password_hash = '!INVALID'.
        // BCrypt verify бросит IllegalArgumentException — AuthServiceImpl должен
        // конвертировать это в InvalidCredentialsException, а не в 500.
        val legacy = User(
            id = UUID.randomUUID(),
            email = "legacy@vargod.local",
            passwordHash = "!INVALID",
            role = Role.RESEARCHER,
            createdAt = Instant.now(),
        )
        every { repo.findByEmail("legacy@vargod.local") } returns legacy

        assertThrows(InvalidCredentialsException::class.java) {
            auth.authenticate("legacy@vargod.local", "anything")
        }
    }

    private fun userFixture(email: String, plainPassword: String): User = User(
        id = UUID.randomUUID(),
        email = email,
        passwordHash = BCrypt.withDefaults().hashToString(4, plainPassword.toCharArray()),
        role = Role.RESEARCHER,
        createdAt = Instant.now(),
    )
}
