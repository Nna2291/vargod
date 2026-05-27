package com.vargod.api.config

import com.vargod.api.security.CurrentUserArgumentResolver
import org.springframework.context.annotation.Configuration
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

/**
 * MVC-уровень: подключение argument-resolver'а для [CurrentUser][com.vargod.api.security.CurrentUser].
 *
 * CORS теперь настраивается централизованно в SecurityFilterChain через
 * CorsConfigurationSource (см. SecurityConfig). MVC-уровневый CorsRegistry был
 * избыточным — SecurityFilterChain бы его всё равно перебивал.
 */
@Configuration
class WebConfig(
    private val currentUserArgumentResolver: CurrentUserArgumentResolver,
) : WebMvcConfigurer {

    override fun addArgumentResolvers(resolvers: MutableList<HandlerMethodArgumentResolver>) {
        resolvers.add(currentUserArgumentResolver)
    }
}
