package com.vargod.api.web.admin

import com.vargod.api.security.CurrentUser
import com.vargod.api.web.dto.UserView
import com.vargod.domain.Role
import com.vargod.service.AdminUserService
import com.vargod.service.AuditActions
import com.vargod.service.AuditLogService
import com.vargod.service.UserService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

/**
 * Admin-CRUD пользователей. Доступ ограничен SecurityConfig'ом на уровне URL —
 * сюда попадают только ADMIN'ы. Каждая мутация пишется в audit_log.
 */
@RestController
@RequestMapping("/api/admin/users")
@Tag(name = "Admin: Users", description = "CRUD пользователей (только ADMIN)")
class AdminUserController(
    private val adminUserService: AdminUserService,
    private val userService: UserService,
    private val auditLogService: AuditLogService,
) {

    private val log = LoggerFactory.getLogger(AdminUserController::class.java)

    @Operation(summary = "Список всех пользователей")
    @GetMapping
    fun list(): List<UserView> = adminUserService.list().map(UserView::of)

    @Operation(summary = "Создать пользователя с заданной ролью")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(@RequestBody req: CreateUserRequest, currentUser: CurrentUser): UserView {
        val role = parseRole(req.role)
        val user = adminUserService.create(req.email, req.password, role)
        log.info("Admin {} создал пользователя {} (role={})", currentUser.email, user.email, user.role)
        auditLogService.log(
            actor = toUser(currentUser),
            action = AuditActions.USER_CREATE,
            targetType = "USER",
            targetId = user.id,
            payload = mapOf("email" to user.email, "role" to user.role.name),
        )
        return UserView.of(user)
    }

    @Operation(summary = "Сменить роль пользователя")
    @PutMapping("/{id}/role")
    fun changeRole(
        @PathVariable id: UUID,
        @RequestBody req: ChangeRoleRequest,
        currentUser: CurrentUser,
    ): UserView {
        val newRole = parseRole(req.role)
        val updated = adminUserService.changeRole(id, newRole)
        log.info("Admin {} сменил роль пользователя {} на {}", currentUser.email, updated.email, newRole)
        auditLogService.log(
            actor = toUser(currentUser),
            action = AuditActions.USER_ROLE_CHANGE,
            targetType = "USER",
            targetId = updated.id,
            payload = mapOf("newRole" to newRole.name),
        )
        return UserView.of(updated)
    }

    @Operation(summary = "Удалить пользователя (нельзя себя самого)")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(@PathVariable id: UUID, currentUser: CurrentUser) {
        // Снимок email до удаления — чтобы попасть в audit_log.payload.
        val target = userService.getById(id)
        adminUserService.delete(actorId = currentUser.id, targetId = id)
        log.info("Admin {} удалил пользователя {}", currentUser.email, target.email)
        auditLogService.log(
            actor = toUser(currentUser),
            action = AuditActions.USER_DELETE,
            targetType = "USER",
            targetId = id,
            payload = mapOf("email" to target.email),
        )
    }

    private fun parseRole(raw: String): Role = try {
        Role.valueOf(raw.uppercase())
    } catch (e: IllegalArgumentException) {
        throw IllegalArgumentException("Unknown role '$raw'. Allowed: ${Role.values().joinToString()}")
    }

    private fun toUser(cu: CurrentUser) = userService.getById(cu.id)
}
