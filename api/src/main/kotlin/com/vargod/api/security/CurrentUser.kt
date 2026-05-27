package com.vargod.api.security

import com.vargod.domain.Role
import java.util.UUID

/**
 * Типизированный principal, который контроллеры получают через
 * [@AuthenticationPrincipal][org.springframework.security.core.annotation.AuthenticationPrincipal]
 * или явный аргумент. Содержит распарсенные данные из JWT — без сетевого вызова в БД.
 */
data class CurrentUser(
    val id: UUID,
    val email: String,
    val role: Role,
) {
    fun isAdmin(): Boolean = role == Role.ADMIN
}
