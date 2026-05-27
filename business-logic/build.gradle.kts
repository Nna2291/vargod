// Компонент «Бизнес-логика».
// Сервисы предметной области + движки симуляции (ОДУ) и валидации.
// Зависит только от доменного ядра.

plugins {
    `java-library`
}

dependencies {
    api(project(":domain"))

    implementation("org.apache.commons:commons-math3:3.6.1")

    // Нужен AuditLogServiceImpl (P3a) для сериализации payload-журнала.
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.16.1")

    // Pure-Java реализация BCrypt без зависимости от Spring — business-logic
    // не тянет Spring-runtime, поэтому используем at.favre.lib вместо
    // spring-security-crypto. Совместимо с любыми BCrypt-хешами вида $2a/$2b.
    implementation("at.favre.lib:bcrypt:0.10.2")

    testImplementation(testFixtures(project(":domain")))
}
