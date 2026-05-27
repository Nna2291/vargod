package com.vargod.domain.repository

import com.vargod.domain.Role
import com.vargod.domain.User
import java.util.UUID

/**
 * Контракт хранения пользователей. Реализуется в data-access (JDBC).
 *
 * Email — естественный ключ для логина, регистр-нечувствительный (нормализуется
 * к нижнему регистру на уровне репозитория).
 */
interface UserRepository {
    fun findById(id: UUID): User?
    fun findByEmail(email: String): User?
    fun findAll(): List<User>
    fun existsAdmin(): Boolean
    fun save(user: User): User
    fun updateRole(id: UUID, role: Role): User
    fun deleteById(id: UUID)
}
