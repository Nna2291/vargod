package com.vargod.api.security

import com.vargod.domain.Role
import org.springframework.core.MethodParameter
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.stereotype.Component
import org.springframework.web.bind.support.WebDataBinderFactory
import org.springframework.web.context.request.NativeWebRequest
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.method.support.ModelAndViewContainer
import java.util.UUID

/**
 * Позволяет контроллерам объявлять параметр типа [CurrentUser] и автоматически
 * получать распарсенного из JWT principal'а. Если запрос анонимный — возвращает
 * `null`; контроллер сам решает, требуется ли пользователь.
 *
 * Маршруты, требующие auth, отсечены в SecurityFilterChain — здесь null безопасен.
 */
@Component
class CurrentUserArgumentResolver : HandlerMethodArgumentResolver {

    override fun supportsParameter(parameter: MethodParameter): Boolean =
        parameter.parameterType == CurrentUser::class.java

    override fun resolveArgument(
        parameter: MethodParameter,
        mavContainer: ModelAndViewContainer?,
        webRequest: NativeWebRequest,
        binderFactory: WebDataBinderFactory?,
    ): CurrentUser? {
        val auth = SecurityContextHolder.getContext().authentication as? JwtAuthenticationToken
            ?: return null
        val jwt = auth.token as Jwt
        val sub = jwt.subject ?: return null
        return CurrentUser(
            id = UUID.fromString(sub),
            email = jwt.getClaimAsString("email") ?: "",
            role = jwt.getClaimAsString("role")
                ?.let { runCatching { Role.valueOf(it) }.getOrNull() }
                ?: Role.RESEARCHER,
        )
    }
}
