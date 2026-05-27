package com.vargod.service.impl

import at.favre.lib.crypto.bcrypt.BCrypt
import com.vargod.domain.InvalidCredentialsException
import com.vargod.domain.Role
import com.vargod.domain.User
import com.vargod.domain.repository.UserRepository
import com.vargod.service.AuthService
import com.vargod.service.UserService
import org.slf4j.LoggerFactory

/**
 * Аутентификация через BCrypt.
 *
 * Намеренно одинаково медленно реагирует на «email не существует» и «пароль не
 * совпал» — для пропуска шага сравнения хеша даже на несуществующем email
 * используется dummy-проверка с фиктивным хешем (constant-time-ish защита
 * от user-enumeration через тайминги).
 */
class AuthServiceImpl(
    private val userRepository: UserRepository,
    private val userService: UserService,
) : AuthService {

    private val log = LoggerFactory.getLogger(AuthServiceImpl::class.java)
    private val verifier = BCrypt.verifyer()

    override fun register(email: String, plainPassword: String, role: Role): User {
        val user = userService.create(email = email, plainPassword = plainPassword, role = role)
        log.info("Зарегистрирован пользователь {} с ролью {}", user.email, user.role)
        return user
    }

    override fun authenticate(email: String, plainPassword: String): User {
        val user = userRepository.findByEmail(email)
        if (user == null) {
            // Симулируем работу с хешем, чтобы атакующий не отличал «нет email»
            // от «есть email, неверный пароль» по времени ответа.
            verifier.verify(plainPassword.toCharArray(), DUMMY_HASH)
            log.warn("Неуспешная попытка входа: email '{}' не найден", email)
            throw InvalidCredentialsException()
        }
        val ok = try {
            verifier.verify(plainPassword.toCharArray(), user.passwordHash).verified
        } catch (e: IllegalArgumentException) {
            // Битый хеш в БД (например, '!INVALID' от legacy-бэкфилла V3).
            log.warn("Неверный формат хеша для пользователя {}: {}", user.email, e.message)
            false
        }
        if (!ok) {
            log.warn("Неуспешная попытка входа: неверный пароль для {}", user.email)
            throw InvalidCredentialsException()
        }
        log.info("Успешный вход: {} (роль {})", user.email, user.role)
        return user
    }

    private companion object {
        // Заранее сгенерированный BCrypt-хеш произвольной строки. Нужен только
        // для constant-time-подобной симуляции при отсутствии пользователя.
        const val DUMMY_HASH =
            "\$2a\$10\$abcdefghijklmnopqrstuvOQGYsxlMlpAFFwpQ0SOR8gnxBxv9JFpe"
    }
}
