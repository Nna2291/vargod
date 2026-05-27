// Корневой build-файл мультимодульного проекта Vargod.
// Каждый модуль (domain, data-access, business-logic, app-cli, api) компилируется
// в отдельный файл программной сборки (JAR) — отдельную единицу развёртывания.
// Здесь — только общая конфигурация, применяемая ко всем модулям.

plugins {
    kotlin("jvm") version "1.9.25" apply false
    kotlin("plugin.spring") version "1.9.25" apply false
    id("org.springframework.boot") version "3.3.5" apply false
    id("io.spring.dependency-management") version "1.1.6" apply false
}

subprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")

    group = "com.vargod"
    version = "1.0.0"

    repositories {
        mavenCentral()
    }

    // Единый тулчейн Java 17 для всех модулей (требование Spring Boot 3.x).
    extensions.configure<org.gradle.api.plugins.JavaPluginExtension> {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(17))
        }
    }

    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
        kotlinOptions {
            jvmTarget = "17"
            freeCompilerArgs = listOf("-Xjsr305=strict")
        }
    }

    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
    }

    dependencies {
        "testImplementation"(platform("org.junit:junit-bom:5.10.2"))
        "testImplementation"("org.junit.jupiter:junit-jupiter")
        "testImplementation"("io.mockk:mockk:1.13.10")
        "testImplementation"("org.jetbrains.kotlin:kotlin-test")
    }
}
