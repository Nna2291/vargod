package com.vargod.service.impl

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import com.vargod.domain.AuditEntry
import com.vargod.domain.PersistenceException
import com.vargod.domain.repository.AuditLogFilter
import com.vargod.domain.repository.AuditLogRepository
import com.vargod.service.AuditActions
import com.vargod.testing.BoundaryValues
import com.vargod.testing.Classical
import com.vargod.testing.EquivalencePartitioning
import com.vargod.testing.ErrorGuessing
import com.vargod.testing.London
import com.vargod.testing.Pairwise
import com.vargod.testing.TestTime
import com.vargod.testing.builders.anAuditEntry
import com.vargod.testing.inmemory.InMemoryAuditLogRepository
import com.vargod.testing.inmemory.InMemoryUserRepository
import com.vargod.testing.mothers.UserMother
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import io.qameta.allure.Epic
import io.qameta.allure.Feature
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import java.util.UUID

@London
@Epic("Business logic")
@Feature("AuditLogService")
@DisplayName("AuditLogServiceImpl — London style (MockK)")
@ExtendWith(MockKExtension::class)
class AuditLogServiceImplLondonTest {

    @MockK private lateinit var auditLogRepository: AuditLogRepository

    private lateinit var sut: AuditLogServiceImpl

    @BeforeEach
    fun setUp() {
        sut = AuditLogServiceImpl(auditLogRepository, clock = TestTime.FIXED_CLOCK)
    }

    @Test
    @EquivalencePartitioning
    fun `log - entry carries snapshot of actor id and email and serialized payload`() {
        // Arrange
        val admin = UserMother.admin()
        val targetId = UUID.randomUUID()
        val saved = slot<AuditEntry>()
        every { auditLogRepository.save(capture(saved)) } answers { firstArg() }

        // Act
        val result = sut.log(admin, AuditActions.USER_DELETE, "USER", targetId, mapOf("email" to "gone@vargod.test"))

        // Assert
        assertThat(result).isEqualTo(saved.captured)
        assertThat(result.userId).isEqualTo(admin.id)
        assertThat(result.userEmail).isEqualTo(admin.email)
        assertThat(result.action).isEqualTo(AuditActions.USER_DELETE)
        assertThat(result.targetType).isEqualTo("USER")
        assertThat(result.targetId).isEqualTo(targetId)
        assertThat(result.payload).isEqualTo("""{"email":"gone@vargod.test"}""")
        assertThat(result.createdAt).isEqualTo(TestTime.NOW)
    }

    @Test
    @ErrorGuessing
    fun `log - payload serialization failure does not break the action, payload becomes null`() {
        // Arrange
        val brokenMapper = mockk<ObjectMapper>()
        every { brokenMapper.writeValueAsString(any()) } throws object : JsonProcessingException("boom") {}
        every { auditLogRepository.save(any()) } answers { firstArg() }
        val sutWithBrokenMapper = AuditLogServiceImpl(auditLogRepository, brokenMapper, TestTime.FIXED_CLOCK)

        // Act
        val result = sutWithBrokenMapper.log(UserMother.admin(), AuditActions.MODEL_UPDATE, payload = mapOf("k" to "v"))

        // Assert
        assertThat(result.payload).isNull()
        verify(exactly = 1) { auditLogRepository.save(match { it.payload == null }) }
    }

    @Test
    @ErrorGuessing
    fun `log - repository failure is propagated to the caller`() {
        // Arrange
        every { auditLogRepository.save(any()) } throws PersistenceException("audit_log is read-only")

        // Act
        val error = assertThrows<PersistenceException> { sut.log(null, AuditActions.USER_LOGIN_FAIL) }

        // Assert
        assertThat(error.message).isEqualTo("audit_log is read-only")
    }

    @Test
    @EquivalencePartitioning
    fun `find - passes the filter to repository unchanged`() {
        // Arrange
        val filter = AuditLogFilter(action = AuditActions.USER_CREATE, limit = 10, offset = 20)
        val entries = listOf(anAuditEntry().withAction(AuditActions.USER_CREATE).build())
        every { auditLogRepository.find(filter) } returns entries

        // Act
        val result = sut.find(filter)

        // Assert
        assertThat(result).isEqualTo(entries)
        verify(exactly = 1) { auditLogRepository.find(filter) }
    }

    @Test
    @BoundaryValues
    fun `find - repository rejection of invalid limit is propagated`() {
        // Arrange
        val filter = AuditLogFilter(limit = 0)
        every { auditLogRepository.find(filter) } throws IllegalArgumentException("limit must be in 1..1000, got 0")

        // Act
        val error = assertThrows<IllegalArgumentException> { sut.find(filter) }

        // Assert
        assertThat(error.message).contains("got 0")
    }
}
