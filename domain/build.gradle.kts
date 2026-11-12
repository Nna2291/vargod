// Компонент «Доменное ядро» (общие контракты).
// Сущности, исключения, интерфейсы репозиториев, загрузчик конфигурации.
// Не зависит ни от каких других модулей проекта.

plugins {
    `java-library`
    `java-test-fixtures`
}

dependencies {
    // Фасад логирования доступен всем модулям, которые зависят от domain.
    api("org.slf4j:slf4j-api:2.0.13")

    // Тестовые хелперы (ТиОПО ЛР1), общие для всех модулей через testFixtures(project(":domain")):
    // Data Builder'ы, Object Mother'ы, in-memory репозитории, аннотации-теги техник
    // тест-дизайна и listener учёта тестовых процессов.
    testFixturesApi(platform("org.junit:junit-bom:5.10.2"))
    testFixturesApi("org.junit.jupiter:junit-jupiter-api")
    testFixturesImplementation("org.junit.platform:junit-platform-launcher")
}
