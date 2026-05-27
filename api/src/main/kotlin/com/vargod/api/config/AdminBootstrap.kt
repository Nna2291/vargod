package com.vargod.api.config

import com.vargod.config.AppConfig
import com.vargod.domain.Role
import com.vargod.domain.repository.UserRepository
import com.vargod.service.AuthService
import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Создаёт первого администратора при старте приложения, если:
 *   1) в users ещё нет ни одного admin'а, и
 *   2) переменные VARGOD_ADMIN_EMAIL и VARGOD_ADMIN_PASSWORD заданы.
 *
 * После первого ADMIN'а дополнительных создаёт сам админ через /api/admin/users.
 * Не упадёт, если переменные не заданы — просто залогирует, что bootstrap пропущен.
 */
@Configuration
class AdminBootstrap {

    private val log = LoggerFactory.getLogger(AdminBootstrap::class.java)

    @Bean
    fun adminBootstrapRunner(
        config: AppConfig,
        userRepository: UserRepository,
        authService: AuthService,
    ) = ApplicationRunner {
        val email = config.adminEmail
        val password = config.adminPassword
        if (email.isNullOrBlank() || password.isNullOrBlank()) {
            log.info("AdminBootstrap: VARGOD_ADMIN_EMAIL/PASSWORD не заданы — пропускаю.")
            return@ApplicationRunner
        }
        if (userRepository.existsAdmin()) {
            log.info("AdminBootstrap: ADMIN уже существует — пропускаю автосоздание.")
            return@ApplicationRunner
        }
        val existing = userRepository.findByEmail(email)
        if (existing != null) {
            log.warn(
                "AdminBootstrap: пользователь {} уже есть, но не ADMIN. Повышаю до ADMIN.",
                email,
            )
            userRepository.updateRole(existing.id, Role.ADMIN)
            return@ApplicationRunner
        }
        // НЕ роняем Spring, если bootstrap-пароль не прошёл валидацию. Иначе любая
        // опечатка в env гарантирует, что приложение не стартует — это плохой UX.
        try {
            authService.register(email = email, plainPassword = password, role = Role.ADMIN)
            log.info("AdminBootstrap: создан первый ADMIN {}", email)
        } catch (e: IllegalArgumentException) {
            log.error(
                "AdminBootstrap: не удалось создать ADMIN {}: {}. " +
                    "Проверьте VARGOD_ADMIN_PASSWORD (≥ 8 символов). " +
                    "Стартую без admin'а — создайте его позже через CLI 'register' или /api/auth/register.",
                email, e.message,
            )
        }
    }
}
