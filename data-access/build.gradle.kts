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
    testImplementation("org.testcontainers:postgresql:1.20.4")
    testImplementation("org.testcontainers:junit-jupiter:1.20.4")
}
