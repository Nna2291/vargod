package com.vargod.api.config

import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Contact
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.info.License
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.security.SecurityScheme
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Конфигурация OpenAPI 3 / Swagger UI.
 *
 * Все эндпоинты, кроме /api/auth и каталога моделей, требуют
 * JWT bearer-токена. Чтобы в Swagger UI можно было «Authorize» и вызывать
 * защищённые методы, регистрируем схему безопасности `bearer-auth` и делаем
 * её требованием по умолчанию для всего API.
 */
@Configuration
class OpenApiConfig {

    @Bean
    fun openApi(): OpenAPI = OpenAPI()
        .info(
            Info()
                .title("Vargod API")
                .description(
                    "REST API платформы Vargod — вычислительные эксперименты " +
                        "над биологическими ОДУ-моделями (SIR, Лотка-Вольтерра, " +
                        "SEIRD, FitzHugh-Nagumo, Hodgkin-Huxley и др.). " +
                        "Авторизация — JWT HS256 в заголовке `Authorization: Bearer <token>`."
                )
                .version("1.0.0")
                .contact(
                    Contact()
                        .name("Vargod (IU7, 2027)")
                        .email("kosmatymaks@gmail.com")
                )
                .license(
                    License()
                        .name("MIT")
                        .url("https://opensource.org/licenses/MIT")
                )
        )
        .addSecurityItem(SecurityRequirement().addList("bearer-auth"))
        .components(
            Components()
                .addSecuritySchemes(
                    "bearer-auth",
                    SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .description("JWT-токен, выдаваемый /api/auth/login и /api/auth/register"),
                )
        )
}
