package com.vargod.api.web

import com.vargod.api.web.dto.ApiError
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RestController

/**
 * DEPRECATED. P1 убрал примитивный X-User-Id-сессионный поток.
 * Используйте `/api/auth/login` (или `/register`) — оба возвращают JWT-токен,
 * который клиент кладёт в заголовок `Authorization: Bearer <token>`.
 *
 * Этот контроллер оставлен только чтобы старые клиенты не получали 404, а видели
 * 410 Gone с понятным сообщением. Может быть удалён в любой момент.
 */
@RestController
@RequestMapping("/api/session")
class SessionController {

    @RequestMapping(
        value = ["/login", "/guest"],
        method = [RequestMethod.POST, RequestMethod.GET],
    )
    fun gone(): ResponseEntity<ApiError> = ResponseEntity
        .status(HttpStatus.GONE)
        .body(
            ApiError(
                error = "ENDPOINT_REMOVED",
                message = "/api/session/* removed in P1. Use POST /api/auth/login " +
                    "and pass the token as Authorization: Bearer <token>.",
            ),
        )
}
