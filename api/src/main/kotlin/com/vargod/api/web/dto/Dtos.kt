package com.vargod.api.web.dto

import com.vargod.domain.User
import java.util.UUID

// ============================================================
//  Эксперименты / сравнения
// ============================================================

/** Тело запроса на создание сравнения экспериментов. */
data class CreateComparisonRequest(val experimentIds: List<UUID>)

// ============================================================
//  Auth (P1) — пришли на смену старому SessionController.
//  LoginRequest/RegisterRequest принимают email+пароль; в ответ всегда JWT.
// ============================================================

data class RegisterRequest(val email: String, val password: String)

data class LoginRequest(val email: String, val password: String)

/** Полный ответ на /register и /login: токен + минимальное представление пользователя. */
data class AuthResponse(val token: String, val user: UserView)

/** Безопасное представление пользователя для отдачи наружу (без passwordHash!). */
data class UserView(
    val id: UUID,
    val email: String,
    val role: String,
) {
    companion object {
        fun of(user: User): UserView =
            UserView(id = user.id, email = user.email, role = user.role.name)
    }
}

// ============================================================
//  Общие
// ============================================================

/** Унифицированный JSON-ответ при ошибке. */
data class ApiError(
    val error: String,
    val message: String,
    val details: List<String> = emptyList(),
)
