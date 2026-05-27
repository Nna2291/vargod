package com.vargod.service.impl

import com.vargod.domain.AuditEntry
import com.vargod.domain.Role
import com.vargod.domain.User
import com.vargod.domain.repository.AuditLogFilter
import com.vargod.domain.repository.AuditLogRepository
import com.vargod.service.AuditActions
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID

/**
 * Контракт: AuditLogService снимает userEmail на момент записи, чтобы
 * запись пережила удаление пользователя (audit_log.user_id выставлен в
 * ON DELETE SET NULL, но user_email — отдельная колонка).
 */
class AuditLogServiceImplTest {

    private val repo = mockk<AuditLogRepository>()
    private val service = AuditLogServiceImpl(repo)

    @Test
    fun `log snapshots actor email and id into the entry`() {
        val captured = slot<AuditEntry>()
        every { repo.save(capture(captured)) } answers { captured.captured }

        val user = User(
            id = UUID.randomUUID(),
            email = "admin@vargod.local",
            passwordHash = "irrelevant",
            role = Role.ADMIN,
            createdAt = Instant.now(),
        )
        val target = UUID.randomUUID()

        service.log(
            actor = user,
            action = AuditActions.USER_DELETE,
            targetType = "USER",
            targetId = target,
            payload = mapOf("email" to "deleted@x.io"),
        )

        val entry = captured.captured
        assertEquals(user.id, entry.userId)
        // Это и есть критическое свойство: email лёг в саму запись, а не подгружается JOIN'ом.
        assertEquals("admin@vargod.local", entry.userEmail)
        assertEquals(AuditActions.USER_DELETE, entry.action)
        assertEquals("USER", entry.targetType)
        assertEquals(target, entry.targetId)
        assertNotNull(entry.payload)
        assertTrue(entry.payload!!.contains("deleted@x.io"))
    }

    @Test
    fun `entry persists user email even after user is later deleted from repository`() {
        val savedEntries = mutableListOf<AuditEntry>()
        every { repo.save(any()) } answers {
            val e = firstArg<AuditEntry>()
            savedEntries += e
            e
        }
        every { repo.find(any()) } answers { savedEntries.toList() }

        val user = User(
            id = UUID.randomUUID(),
            email = "soon-to-be-deleted@vargod.local",
            passwordHash = "irrelevant",
            role = Role.ADMIN,
            createdAt = Instant.now(),
        )
        service.log(actor = user, action = AuditActions.MODEL_CREATE)

        // «Имитация» удаления пользователя: запрашиваем журнал, ничего не
        // подгружая из users; email должен остаться доступным.
        val fetched = service.find(AuditLogFilter())
        assertEquals(1, fetched.size)
        assertEquals("soon-to-be-deleted@vargod.local", fetched.first().userEmail)
        assertEquals(user.id, fetched.first().userId)
    }

    @Test
    fun `log with null actor produces entry without userId or email`() {
        val captured = slot<AuditEntry>()
        every { repo.save(capture(captured)) } answers { captured.captured }

        service.log(actor = null, action = AuditActions.USER_LOGIN_FAIL)

        assertNull(captured.captured.userId)
        assertNull(captured.captured.userEmail)
        assertEquals(AuditActions.USER_LOGIN_FAIL, captured.captured.action)
        verify(exactly = 1) { repo.save(any()) }
    }
}
