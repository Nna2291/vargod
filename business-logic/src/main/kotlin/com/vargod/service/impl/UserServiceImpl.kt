package com.vargod.service.impl

import at.favre.lib.crypto.bcrypt.BCrypt
import com.vargod.domain.EmailAlreadyTakenException
import com.vargod.domain.NotFoundException
import com.vargod.domain.Role
import com.vargod.domain.User
import com.vargod.domain.repository.UserRepository
import com.vargod.service.UserService
import java.time.Clock
import java.time.Instant
import java.util.UUID

/**
 * Бизнес-логика CRUD пользователей.
 * Хеширование паролей — BCrypt с cost 10 (дефолт), приемлемо для академического
 * проекта; в production обычно держат 12.
 */
class UserServiceImpl(
    private val userRepository: UserRepository,
    private val bcryptCost: Int = 10,
    /** Источник времени для createdAt (внедряется ради детерминированных тестов). */
    private val clock: Clock = Clock.systemUTC(),
) : UserService {

    override fun getById(id: UUID): User =
        userRepository.findById(id) ?: throw NotFoundException("User", id)

    override fun getByEmail(email: String): User? = userRepository.findByEmail(email)

    override fun listAll(): List<User> = userRepository.findAll()

    override fun create(email: String, plainPassword: String, role: Role): User {
        require(email.isNotBlank()) { "Email не может быть пустым" }
        require(plainPassword.length >= MIN_PASSWORD_LEN) {
            "Пароль должен быть не короче $MIN_PASSWORD_LEN символов"
        }
        if (userRepository.findByEmail(email) != null) throw EmailAlreadyTakenException(email)

        val hash = BCrypt.withDefaults().hashToString(bcryptCost, plainPassword.toCharArray())
        val user = User(
            id = UUID.randomUUID(),
            email = email.trim(),
            passwordHash = hash,
            role = role,
            createdAt = Instant.now(clock),
        )
        return userRepository.save(user)
    }

    override fun changeRole(id: UUID, newRole: Role): User = userRepository.updateRole(id, newRole)

    override fun delete(id: UUID) {
        // findById для строгого 404 вместо тихого no-op.
        userRepository.findById(id) ?: throw NotFoundException("User", id)
        userRepository.deleteById(id)
    }

    private companion object {
        const val MIN_PASSWORD_LEN = 8
    }
}
