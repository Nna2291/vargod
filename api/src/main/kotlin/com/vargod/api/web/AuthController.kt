package com.vargod.api.web

import com.vargod.api.security.CurrentUser
import com.vargod.api.security.JwtService
import com.vargod.api.web.dto.AuthResponse
import com.vargod.api.web.dto.LoginRequest
import com.vargod.api.web.dto.RegisterRequest
import com.vargod.api.web.dto.UserView
import com.vargod.service.AuthService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException

/**
 * Регистрация, логин, /me. Заменяет старый SessionController с заголовком
 * X-User-Id (тот гарантированно небезопасен — клиент мог выдать себя за кого угодно).
 */
@RestController
@RequestMapping("/api/auth")
@Tag(name = "Auth", description = "Регистрация, логин, текущий пользователь")
class AuthController(
    private val authService: AuthService,
    private val jwtService: JwtService,
) {

    private val log = LoggerFactory.getLogger(AuthController::class.java)

    @Operation(summary = "Регистрация нового пользователя (роль RESEARCHER), возвращает JWT")
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    fun register(@RequestBody req: RegisterRequest): AuthResponse {
        val user = authService.register(req.email, req.password)
        val issued = jwtService.issue(user)
        log.info("Регистрация: {}", user.email)
        return AuthResponse(token = issued.token, user = UserView.of(user))
    }

    @Operation(summary = "Логин по email+паролю, возвращает JWT")
    @PostMapping("/login")
    fun login(@RequestBody req: LoginRequest): AuthResponse {
        val user = authService.authenticate(req.email, req.password)
        val issued = jwtService.issue(user)
        return AuthResponse(token = issued.token, user = UserView.of(user))
    }

    @Operation(summary = "Текущий пользователь по JWT")
    @GetMapping("/me")
    fun me(currentUser: CurrentUser?): UserView {
        currentUser ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated")
        return UserView(id = currentUser.id, email = currentUser.email, role = currentUser.role.name)
    }
}
