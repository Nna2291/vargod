package com.vargod.service

import com.vargod.domain.Role
import com.vargod.domain.User

/**
 * Аутентификация и регистрация. Не работает с HTTP — никакого JWT здесь нет,
 * выдачу токенов и их разбор делает API-слой (JwtService).
 *
 * Бросает [com.vargod.domain.InvalidCredentialsException] на любую ошибку
 * логина (намеренно без различения «нет такого email» / «неверный пароль»).
 */
interface AuthService {
    /** Регистрация нового пользователя; роль по умолчанию RESEARCHER. */
    fun register(email: String, plainPassword: String, role: Role = Role.RESEARCHER): User

    /** Проверка пароля. Возвращает пользователя при успехе, бросает иначе. */
    fun authenticate(email: String, plainPassword: String): User
}
