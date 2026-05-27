package com.vargod.repository.jdbc

import com.vargod.domain.PersistenceException
import com.vargod.domain.Role
import com.vargod.domain.User
import com.vargod.domain.repository.UserRepository
import java.sql.ResultSet
import java.sql.SQLException
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID
import javax.sql.DataSource

/**
 * JDBC-реализация хранения пользователей.
 *
 * Email хранится как введён, но поиск и проверка уникальности идут по LOWER(email)
 * — это поведение зафиксировано уникальным индексом `users_email_lower_uniq` (V2).
 */
class JdbcUserRepository(private val dataSource: DataSource) : UserRepository {

    override fun findById(id: UUID): User? = sql("SELECT $COLS FROM users WHERE id = ?") { ps ->
        ps.setObject(1, id)
        ps.executeQuery().use { rs -> if (rs.next()) rs.toUser() else null }
    }

    override fun findByEmail(email: String): User? =
        sql("SELECT $COLS FROM users WHERE LOWER(email) = LOWER(?)") { ps ->
            ps.setString(1, email)
            ps.executeQuery().use { rs -> if (rs.next()) rs.toUser() else null }
        }

    override fun findAll(): List<User> = sql("SELECT $COLS FROM users ORDER BY created_at") { ps ->
        ps.executeQuery().use { rs ->
            val out = mutableListOf<User>()
            while (rs.next()) out += rs.toUser()
            out
        }
    }

    override fun existsAdmin(): Boolean =
        sql("SELECT 1 FROM users WHERE role = 'ADMIN' LIMIT 1") { ps ->
            ps.executeQuery().use { it.next() }
        }

    override fun save(user: User): User = wrap("save user") {
        // UPSERT по id; на конфликт email рассчитываем что слой выше уже проверил.
        dataSource.connection.use { conn ->
            conn.prepareStatement(
                "INSERT INTO users (id, email, password_hash, role, created_at) " +
                    "VALUES (?, ?, ?, ?, ?) " +
                    "ON CONFLICT (id) DO UPDATE SET email = EXCLUDED.email, " +
                    "password_hash = EXCLUDED.password_hash, role = EXCLUDED.role"
            ).use { ps ->
                ps.setObject(1, user.id)
                ps.setString(2, user.email)
                ps.setString(3, user.passwordHash)
                ps.setString(4, user.role.name)
                ps.setObject(5, OffsetDateTime.ofInstant(user.createdAt, ZoneOffset.UTC))
                ps.executeUpdate()
            }
        }
        user
    }

    override fun updateRole(id: UUID, role: Role): User = wrap("update role") {
        dataSource.connection.use { conn ->
            conn.prepareStatement("UPDATE users SET role = ? WHERE id = ?").use { ps ->
                ps.setString(1, role.name)
                ps.setObject(2, id)
                if (ps.executeUpdate() == 0) throw PersistenceException("User not found for role update: $id")
            }
        }
        findById(id) ?: throw PersistenceException("User vanished after role update: $id")
    }

    override fun deleteById(id: UUID) = wrap("delete user") {
        dataSource.connection.use { conn ->
            conn.prepareStatement("DELETE FROM users WHERE id = ?").use { ps ->
                ps.setObject(1, id)
                ps.executeUpdate()
            }
        }
        Unit
    }

    private fun <T> sql(query: String, block: (java.sql.PreparedStatement) -> T): T = wrap("query") {
        dataSource.connection.use { conn -> conn.prepareStatement(query).use(block) }
    }

    private fun <T> wrap(label: String, block: () -> T): T = try {
        block()
    } catch (e: SQLException) {
        throw PersistenceException("JdbcUserRepository.$label failed: ${e.message}", e)
    }

    private fun ResultSet.toUser(): User = User(
        id = getObject("id", UUID::class.java),
        email = getString("email"),
        passwordHash = getString("password_hash"),
        role = Role.valueOf(getString("role")),
        createdAt = getObject("created_at", OffsetDateTime::class.java).toInstant(),
    )

    private companion object {
        const val COLS = "id, email, password_hash, role, created_at"
    }
}
