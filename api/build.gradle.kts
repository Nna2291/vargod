// Компонент связи GUI и бизнес-логики — REST API на Spring Boot.
// Реализует серверный паттерн MVC (Spring MVC): контроллеры принимают
// HTTP-запросы веб-GUI и делегируют их сервисам бизнес-логики.

plugins {
    id("org.springframework.boot")
    id("io.spring.dependency-management")
    kotlin("plugin.spring")
}

dependencies {
    // Компоненты приложения, собранные отдельными JAR-ами
    implementation(project(":domain"))
    implementation(project(":data-access"))
    implementation(project(":business-logic"))

    implementation("org.springframework.boot:spring-boot-starter-web")
    // JWT-аутентификация: используем стандартный Spring Security + ресурс-сервер
    // на Nimbus (HS256). Никаких самописных OncePerRequestFilter.
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")

    // OpenAPI 3 / Swagger UI (springdoc) — описание REST API и интерактивный
    // explorer на /swagger-ui.html. Версия 2.6.0 совместима с Spring Boot 3.3.x.
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.6.0")

    runtimeOnly("org.postgresql:postgresql:42.7.4")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-test")
}

springBoot {
    mainClass.set("com.vargod.api.VargodApiApplicationKt")
}

// Нужен только исполняемый bootJar; «обычный» jar не собираем.
tasks.named("jar") {
    enabled = false
}
