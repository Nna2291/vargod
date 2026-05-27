package com.vargod.service

import com.vargod.domain.Role
import com.vargod.domain.User
import java.util.UUID

/**
 * Управление пользователями (создание, обновление роли, выборки).
 * Используется как admin-эндпоинтами (P3a), так и AuthService для регистрации.
 */
interface UserService {
    fun getById(id: UUID): User
    fun getByEmail(email: String): User?
    fun listAll(): List<User>
    fun create(email: String, plainPassword: String, role: Role): User
    fun changeRole(id: UUID, newRole: Role): User
    fun delete(id: UUID)
}
