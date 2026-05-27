// Компонент «Технологический UI» — консольное приложение (REPL).
// Связывает компоненты доступа к данным и бизнес-логики в работающее приложение.

plugins {
    application
}

application {
    mainClass.set("com.vargod.ui.cli.AppKt")
}

dependencies {
    implementation(project(":domain"))
    implementation(project(":data-access"))
    implementation(project(":business-logic"))

    // Реализация SLF4J — система логирования (подключается только в рантайме)
    runtimeOnly("ch.qos.logback:logback-classic:1.5.6")
    runtimeOnly("org.postgresql:postgresql:42.7.4")
}

// Внешний конфиг-файл кладётся в дистрибутив рядом с bin/ и lib/ (вне JAR-ов),
// чтобы параметры можно было менять без пересборки приложения.
distributions {
    named("main") {
        contents {
            from("vargod.properties")
        }
    }
}
