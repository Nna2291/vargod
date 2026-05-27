rootProject.name = "vargod"

plugins {
    // Автоматическое получение JDK 17 для тулчейна (нужно Spring Boot 3.x)
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}

include("domain", "data-access", "business-logic", "app-cli", "api")
