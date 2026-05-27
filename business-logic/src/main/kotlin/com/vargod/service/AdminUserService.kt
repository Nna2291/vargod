package com.vargod.service

import com.vargod.domain.Role
import com.vargod.domain.User
import java.util.UUID

/**
 * Admin-операции над пользователями. Делегирует основное создание/удаление
 * в [UserService], но добавляет правило самозащиты: admin не может удалить
 * сам себя — иначе при единственном admin'е система окажется без управления.
 */
interface AdminUserService {
    fun list(): List<User>
    fun create(email: String, plainPassword: String, role: Role): User
    fun delete(actorId: UUID, targetId: UUID)
    fun changeRole(id: UUID, newRole: Role): User
}
