package com.vargod.service.impl

import com.vargod.domain.AuthorizationException
import com.vargod.domain.Role
import com.vargod.domain.User
import com.vargod.service.AdminUserService
import com.vargod.service.UserService
import java.util.UUID

/**
 * Реализация admin-операций над пользователями. Делегирует на [UserService],
 * добавляя бизнес-правило: запрет на удаление самого себя.
 */
class AdminUserServiceImpl(
    private val userService: UserService,
) : AdminUserService {

    override fun list(): List<User> = userService.listAll()

    override fun create(email: String, plainPassword: String, role: Role): User =
        userService.create(email, plainPassword, role)

    override fun delete(actorId: UUID, targetId: UUID) {
        if (actorId == targetId) {
            throw AuthorizationException("Admin не может удалить самого себя")
        }
        userService.delete(targetId)
    }

    override fun changeRole(id: UUID, newRole: Role): User =
        userService.changeRole(id, newRole)
}
