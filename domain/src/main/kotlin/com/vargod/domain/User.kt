package com.vargod.domain

import java.time.Instant
import java.util.UUID

/**
 * Зарегистрированный пользователь.
 *
 * passwordHash — BCrypt-хеш (60-символьная строка вида "2a$10$..." — без знака
 * доллара в начале, так как KDoc парсер не любит "@", "/" и нестандартные
 * последовательности; реальный формат — стандартный BCrypt).
 *
 * role определяет доступы:
 *  - ADMIN — административные эндпоинты под /api/admin/;
 *  - RESEARCHER — обычное использование (создание экспериментов и сравнений).
 *
 * Гость = отсутствие записи / отсутствие токена; в БД не хранится.
 */
data class User(
    val id: UUID,
    val email: String,
    val passwordHash: String,
    val role: Role,
    val createdAt: Instant,
)

enum class Role { ADMIN, RESEARCHER }
