package com.vargod.api.security

import com.fasterxml.jackson.databind.ObjectMapper
import com.vargod.api.web.dto.ApiError
import com.vargod.config.AppConfig
import com.vargod.domain.Role
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter
import org.springframework.security.web.SecurityFilterChain
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource

/**
 * Конфигурация Spring Security 6.x + OAuth2 Resource Server (JWT HS256).
 *
 * Решения:
 *  - stateless (SessionCreationPolicy.STATELESS) — REST API, без HttpSession.
 *  - CSRF выключен — нет cookie-based session'ов, токен в Authorization header.
 *  - CORS — из AppConfig (env-driven список origin'ов).
 *  - 401/403 в формате [com.vargod.api.web.dto.ApiError], как и остальные ошибки.
 */
@Configuration
class SecurityConfig {

    @Bean
    fun jwtDecoder(config: AppConfig): JwtDecoder =
        NimbusJwtDecoder.withSecretKey(JwtService.secretKey(config.jwtSecret)).build()

    @Bean
    fun jwtAuthenticationConverter(): JwtAuthenticationConverter =
        JwtAuthenticationConverter().apply {
            // role: "ADMIN" в claim → authority "ROLE_ADMIN" (по умолчанию у hasRole).
            // JwtGrantedAuthoritiesConverter не используем напрямую, потому что у нас role
            // это скалярная String, а не массив — пишем конвертер вручную.
            setJwtGrantedAuthoritiesConverter { jwt ->
                listOfNotNull(jwt.getClaimAsString("role"))
                    .map { org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_$it") }
            }
            setPrincipalClaimName("sub")
        }

    @Bean
    fun securityFilterChain(
        http: HttpSecurity,
        jwtAuthenticationConverter: JwtAuthenticationConverter,
        objectMapper: ObjectMapper,
        corsConfigurationSource: CorsConfigurationSource,
    ): SecurityFilterChain {
        http
            .csrf { it.disable() }
            .cors { it.configurationSource(corsConfigurationSource) }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .authorizeHttpRequests { auth ->
                auth
                    // Эндпоинты аутентификации — открыты.
                    .requestMatchers("/api/auth/**").permitAll()
                    // Каталог моделей доступен гостю (без токена).
                    .requestMatchers(HttpMethod.GET, "/api/models", "/api/models/**").permitAll()
                    // Публичные эксперименты тоже видны гостю.
                    .requestMatchers(HttpMethod.GET, "/api/experiments", "/api/experiments/*").permitAll()
                    // Swagger UI и OpenAPI JSON — будут включены в P7.
                    .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                    // Админ-эндпоинты — только ADMIN. Содержательно появятся в P3a.
                    .requestMatchers("/api/admin/**").hasRole(Role.ADMIN.name)
                    // Health/info actuator, если когда-нибудь подключим.
                    .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                    // Всё остальное требует аутентификации.
                    .anyRequest().authenticated()
            }
            .oauth2ResourceServer { rs ->
                rs.jwt { jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter) }
                rs.authenticationEntryPoint(jsonAuthEntryPoint(objectMapper))
                rs.accessDeniedHandler(jsonAccessDeniedHandler(objectMapper))
            }
            // Отключаем стандартный логин/basic — у нас только Bearer JWT.
            .formLogin(AbstractHttpConfigurer<*, *>::disable)
            .httpBasic(AbstractHttpConfigurer<*, *>::disable)
        return http.build()
    }

    @Bean
    fun corsConfigurationSource(config: AppConfig): CorsConfigurationSource {
        val cors = CorsConfiguration().apply {
            allowedOrigins = config.corsAllowedOrigins
            allowedMethods = listOf("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
            allowedHeaders = listOf("*")
            // Bearer-токен в Authorization-заголовке, cookie не используем.
            allowCredentials = false
            maxAge = 3600
        }
        return UrlBasedCorsConfigurationSource().apply {
            registerCorsConfiguration("/api/**", cors)
        }
    }

    private fun jsonAuthEntryPoint(mapper: ObjectMapper) =
        org.springframework.security.web.AuthenticationEntryPoint { _, res, ex ->
            res.status = HttpStatus.UNAUTHORIZED.value()
            res.contentType = MediaType.APPLICATION_JSON_VALUE
            mapper.writeValue(
                res.outputStream,
                ApiError("UNAUTHORIZED", ex.message ?: "Authentication required"),
            )
        }

    private fun jsonAccessDeniedHandler(mapper: ObjectMapper) =
        org.springframework.security.web.access.AccessDeniedHandler { _, res, ex ->
            res.status = HttpStatus.FORBIDDEN.value()
            res.contentType = MediaType.APPLICATION_JSON_VALUE
            mapper.writeValue(
                res.outputStream,
                ApiError("FORBIDDEN", ex.message ?: "Access denied"),
            )
        }
}
