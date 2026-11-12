// Компонент «Доступ к данным».
// JDBC-реализации репозиториев, фабрика DataSource, инициализатор схемы,
// заполнение БД (DataSeeder). Зависит только от доменного ядра.

plugins {
    `java-library`
}

dependencies {
    api(project(":domain"))

    implementation("org.postgresql:postgresql:42.7.4")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.16.1")

    // Версионированные миграции схемы. До этого момента схема выкатывалась
    // одним schema.sql из SchemaInitializer — теперь источник истины это
    // db/migration/V*__*.sql, а SchemaInitializer всего лишь зовёт Flyway.
    implementation("org.flywaydb:flyway-core:10.21.0")
    runtimeOnly("org.flywaydb:flyway-database-postgresql:10.21.0")

    testImplementation(testFixtures(project(":domain")))
    // Классические (без mock) тесты репозиториев идут на настоящем PostgreSQL 16,
    // который поднимается embedded-процессом: бинарники PostgreSQL приходят как
    // обычная Maven-зависимость, Docker не нужен, после первой загрузки работает офлайн.
    testImplementation("io.zonky.test:embedded-postgres:2.1.0")
    testImplementation(enforcedPlatform("io.zonky.test.postgres:embedded-postgres-binaries-bom:16.4.0"))
}
