package com.vargod.repository.jdbc

import com.vargod.domain.PersistenceException
import com.vargod.domain.repository.AuditLogFilter
import com.vargod.repository.jdbc.support.DbTestBase
import com.vargod.testing.BoundaryValues
import com.vargod.testing.EquivalencePartitioning
import com.vargod.testing.ErrorGuessing
import com.vargod.testing.Pairwise
import com.vargod.testing.TestTime
import com.vargod.testing.builders.anAuditEntry
import com.vargod.testing.mothers.UserMother
import io.qameta.allure.Feature
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import java.util.UUID

@Feature("AuditLogRepository")
@DisplayName("JdbcAuditLogRepository — classical style (embedded PostgreSQL)")
class JdbcAuditLogRepositoryTest : DbTestBase() {

    private lateinit var sut: JdbcAuditLogRepository

    @BeforeEach
    fun setUp() {
        sut = JdbcAuditLogRepository(dataSource)
    }

    @Test
    @EquivalencePartitioning
    fun `save - entry with actor, target and payload is stored as is`() {
        // Arrange
        val admin = givenUser(UserMother.admin())
        val entry = anAuditEntry().by(admin).withAction("USER_DELETE")
            .targeting("USER", UUID.randomUUID()).withPayload("""{"email":"gone@vargod.test"}""").build()

        // Act
        sut.save(entry)

        // Assert
        assertThat(sut.find(AuditLogFilter())).containsExactly(entry)
    }

    @Test
    @ErrorGuessing
    fun `save - reference to a non-existent user violates foreign key`() {
        // Arrange
        val entry = anAuditEntry().byUserId(UUID.randomUUID()).build()

        // Act
        val error = assertThrows<PersistenceException> { sut.save(entry) }

        // Assert
        assertThat(error.message).startsWith("JdbcAuditLogRepository.save audit failed")
    }

    /**
     * Попарное покрытие четырёх необязательных критериев фильтра (action, userId, from, to),
     * каждый «задан / не задан». Полный перебор — 16 наборов, попарный — 6: любая пара
     * критериев встречается во всех четырёх сочетаниях (Y/Y, Y/N, N/Y, N/N).
     *
     * Данные: alice/A/t0, alice/B/t10, bob/A/t20, bob/B/t30; фильтр — action = A,
     * user = alice, from = t10 (включительно), to = t30 (не включительно).
     */
    @ParameterizedTest(name = "action={0}, user={1}, from={2}, to={3} -> {4} entries")
    @CsvSource(
        "false, false, false, false, 4",
        "true,  true,  true,  true,  0",
        "true,  false, false, true,  2",
        "false, true,  true,  false, 1",
        "true,  true,  false, false, 1",
        "false, false, true,  true,  2",
    )
    @Pairwise
    fun `find - pairwise combinations of optional filter criteria`(
        byAction: Boolean, byUser: Boolean, byFrom: Boolean, byTo: Boolean, expected: Int,
    ) {
        // Arrange
        val alice = givenUser()
        val bob = givenUser()
        sut.save(anAuditEntry().by(alice).withAction("A").createdAt(TestTime.minutesAfterNow(0)).build())
        sut.save(anAuditEntry().by(alice).withAction("B").createdAt(TestTime.minutesAfterNow(10)).build())
        sut.save(anAuditEntry().by(bob).withAction("A").createdAt(TestTime.minutesAfterNow(20)).build())
        sut.save(anAuditEntry().by(bob).withAction("B").createdAt(TestTime.minutesAfterNow(30)).build())
        val filter = AuditLogFilter(
            action = if (byAction) "A" else null,
            userId = if (byUser) alice.id else null,
            from = if (byFrom) TestTime.minutesAfterNow(10) else null,
            to = if (byTo) TestTime.minutesAfterNow(30) else null,
        )

        // Act
        val result = sut.find(filter)

        // Assert
        assertThat(result).hasSize(expected)
        assertThat(result.map { it.createdAt }).isSortedAccordingTo(Comparator.reverseOrder())
    }

    /** Граничные значения limit: допустимый диапазон 1..1000. */
    @ParameterizedTest(name = "limit={0} -> valid={1}")
    @CsvSource("0, false", "1, true", "1000, true", "1001, false")
    @BoundaryValues
    fun `find - limit is accepted only within 1 to 1000`(limit: Int, valid: Boolean) {
        // Arrange
        sut.save(anAuditEntry().build())

        // Act
        val outcome = runCatching { sut.find(AuditLogFilter(limit = limit)) }

        // Assert
        assertThat(outcome.isSuccess).isEqualTo(valid)
        if (valid) assertThat(outcome.getOrThrow()).hasSize(1)
    }
}
