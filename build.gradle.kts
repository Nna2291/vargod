// Корневой build-файл мультимодульного проекта Vargod.
// Каждый модуль (domain, data-access, business-logic, app-cli, api) компилируется
// в отдельный файл программной сборки (JAR) — отдельную единицу развёртывания.
// Здесь — общая конфигурация, применяемая ко всем модулям, и тестовая
// инфраструктура (ТиОПО, ЛР1): покрытие JaCoCo, отчёт Allure, случайный порядок,
// офлайн-режим, управление числом тестовых процессов. Подробности —
// docs/lab-reports/tiopo-lab1.md.

import org.gradle.api.tasks.testing.logging.TestExceptionFormat

plugins {
    kotlin("jvm") version "1.9.25" apply false
    kotlin("plugin.spring") version "1.9.25" apply false
    id("org.springframework.boot") version "3.3.5" apply false
    id("io.spring.dependency-management") version "1.1.6" apply false
    id("io.qameta.allure") version "2.12.0" apply false
    id("io.qameta.allure-aggregate-report") version "2.12.0"
    jacoco
}

// ---------------------------------------------------------------------------
//  Параметры запуска тестов (передаются как -P<имя>[=значение])
// ---------------------------------------------------------------------------
//  -PrandomOrder            случайный порядок классов и методов (JUnit 5 Random orderers)
//  -PrandomSeed=<long>      то же, но с фиксированным seed (воспроизведение «упавшего» порядка)
//  -Poffline                режим без сети: исключаются тесты с тегом "db",
//                           java.net-сокеты тестовой JVM направляются в «мёртвый» SOCKS-прокси
//  -PincludeTags=a,b        запускать только тесты с указанными JUnit-тегами
//  -PexcludeTags=a,b        исключить тесты с указанными JUnit-тегами
//  -PmaxForks=<n>           число параллельных тестовых JVM на один модуль (Test.maxParallelForks)
//  -PforkEvery=<n>          перезапускать тестовую JVM после каждых n тест-классов (Test.forkEvery)
//  -Pthreads=<n>            параллельное выполнение JUnit 5 внутри одной JVM в n потоков
//  -PtestHeap=<size>        максимальный heap тестовой JVM (по умолчанию 512m)

fun Project.flag(name: String): Boolean =
    findProperty(name)?.toString()?.let { it.isEmpty() || it.toBoolean() } ?: false

fun Project.csv(name: String): List<String> =
    findProperty(name)?.toString()?.split(',')?.map { it.trim() }?.filter { it.isNotEmpty() } ?: emptyList()

val randomSeedProperty: String? = findProperty("randomSeed")?.toString()
val randomOrder: Boolean = flag("randomOrder") || randomSeedProperty != null
val randomSeed: String = randomSeedProperty ?: System.currentTimeMillis().toString()
val offlineMode: Boolean = flag("offline")
val includeTagsList: List<String> = csv("includeTags")
val excludeTagsList: List<String> = csv("excludeTags") + (if (offlineMode) listOf("db") else emptyList())
val maxForks: Int = findProperty("maxForks")?.toString()?.toInt() ?: 1
val forkEveryClasses: Long = findProperty("forkEvery")?.toString()?.toLong() ?: 0L
val junitThreads: Int? = findProperty("threads")?.toString()?.toInt()

/** Модули — объекты тестирования ЛР1: доменное ядро, бизнес-логика, доступ к данным. */
val testedModules = listOf(":domain", ":business-logic", ":data-access")

val allureCommandlineVersion = "2.30.0"
val allureJavaLibVersion = "2.29.0"
val jacocoToolVersion = "0.8.12"

jacoco {
    toolVersion = jacocoToolVersion
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

    dependencies {
        "testImplementation"(platform("org.junit:junit-bom:5.10.2"))
        "testImplementation"("org.junit.jupiter:junit-jupiter")
        "testImplementation"("org.junit.jupiter:junit-jupiter-params")
        "testImplementation"("io.mockk:mockk:1.13.10")
        "testImplementation"("org.jetbrains.kotlin:kotlin-test")
        "testImplementation"("org.assertj:assertj-core:3.26.3")
        "testImplementation"(platform("io.qameta.allure:allure-bom:$allureJavaLibVersion"))
        "testImplementation"("io.qameta.allure:allure-junit5")
        "testRuntimeOnly"("org.junit.platform:junit-platform-launcher")
    }

    // ---- Allure: адаптер JUnit 5 пишет build/allure-results, отчёт строит allure-commandline
    apply(plugin = "io.qameta.allure")
    extensions.configure<io.qameta.allure.gradle.base.AllureExtension> {
        version.set(allureCommandlineVersion)
        (this as ExtensionAware).extensions.configure<io.qameta.allure.gradle.adapter.AllureAdapterExtension> {
            allureJavaVersion.set(allureJavaLibVersion)
            aspectjWeaver.set(false)
            autoconfigure.set(true)
        }
    }

    // ---- JaCoCo: покрытие строк и ветвлений
    apply(plugin = "jacoco")
    extensions.configure<JacocoPluginExtension> {
        toolVersion = jacocoToolVersion
    }

    tasks.withType<Test>().configureEach {
        useJUnitPlatform {
            if (includeTagsList.isNotEmpty()) includeTags(*includeTagsList.toTypedArray())
            if (excludeTagsList.isNotEmpty()) excludeTags(*excludeTagsList.toTypedArray())
        }

        // Тесты всегда перезапускаются по запросу (иначе Gradle считает их up-to-date
        // и не выполняет повторно — это мешает, например, прогону в случайном порядке).
        outputs.upToDateWhen { false }

        // Число тестовых процессов (JVM). По умолчанию Gradle: 1 JVM на Test-задачу модуля.
        maxParallelForks = maxForks
        forkEvery = forkEveryClasses
        // Юнит-тестам много памяти не нужно; потолок спасает слабые машины от OOM.
        maxHeapSize = findProperty("testHeap")?.toString() ?: "512m"

        if (randomOrder) {
            systemProperty("junit.jupiter.testclass.order.default", "org.junit.jupiter.api.ClassOrderer\$Random")
            systemProperty("junit.jupiter.testmethod.order.default", "org.junit.jupiter.api.MethodOrderer\$Random")
            systemProperty("junit.jupiter.execution.order.random.seed", randomSeed)
        }

        if (junitThreads != null) {
            // Потоки — внутри одной тестовой JVM (JUnit Jupiter parallel execution).
            // Тесты, работающие с общей БД, сериализуются через @ResourceLock("postgres").
            systemProperty("junit.jupiter.execution.parallel.enabled", "true")
            systemProperty("junit.jupiter.execution.parallel.mode.default", "concurrent")
            systemProperty("junit.jupiter.execution.parallel.mode.classes.default", "concurrent")
            systemProperty("junit.jupiter.execution.parallel.config.strategy", "fixed")
            systemProperty("junit.jupiter.execution.parallel.config.fixed.parallelism", junitThreads.toString())
        }

        if (offlineMode) {
            // Любая попытка открыть java.net.Socket наружу уйдёт в несуществующий
            // SOCKS-прокси и упадёт сразу — так тест, «случайно» полезший в сеть,
            // будет заметен. Межпроцессная связь Gradle идёт через NIO и не затрагивается.
            systemProperty("socksProxyHost", "127.0.0.1")
            systemProperty("socksProxyPort", "9")
            systemProperty("java.net.useSystemProxies", "false")
        }

        // Учёт тестовых процессов: listener из domain/testFixtures пишет по файлу на PID.
        val pidDir = layout.buildDirectory.dir("test-processes/$name").get().asFile
        systemProperty("vargod.test.pidDir", pidDir.absolutePath)

        testLogging {
            events("failed", "skipped")
            exceptionFormat = TestExceptionFormat.FULL
            showStandardStreams = false
        }

        doFirst {
            pidDir.deleteRecursively()
            if (randomOrder) logger.lifecycle("[$path] random order enabled, seed = $randomSeed (повтор: -PrandomSeed=$randomSeed)")
            if (offlineMode) logger.lifecycle("[$path] offline mode: tests tagged 'db' are excluded, outbound sockets are blocked")
        }

        addTestListener(object : TestListener {
            override fun beforeSuite(suite: TestDescriptor) {}
            override fun beforeTest(testDescriptor: TestDescriptor) {}
            override fun afterTest(testDescriptor: TestDescriptor, result: TestResult) {}
            override fun afterSuite(suite: TestDescriptor, result: TestResult) {
                if (suite.parent == null) {
                    logger.lifecycle(
                        "[{}] tests: {}, passed: {}, failed: {}, skipped: {}",
                        path, result.testCount, result.successfulTestCount,
                        result.failedTestCount, result.skippedTestCount,
                    )
                }
            }
        })

        doLast {
            val files = pidDir.listFiles { f -> f.name.endsWith(".pid") }?.sortedBy { it.name } ?: emptyList()
            if (files.isNotEmpty()) {
                logger.lifecycle(
                    "[{}] test JVM processes: {} (maxParallelForks={}, forkEvery={}, junit threads={})",
                    path, files.size, maxParallelForks, forkEvery, junitThreads ?: 1,
                )
                files.forEach { logger.lifecycle("    pid {}: {}", it.nameWithoutExtension, it.readText().trim().replace("\n", "; ")) }
            }
        }

        finalizedBy(tasks.named("jacocoTestReport"))
    }

    tasks.withType<JacocoReport>().configureEach {
        reports {
            xml.required.set(true)
            csv.required.set(true)
            html.required.set(true)
        }
    }
}

// ---------------------------------------------------------------------------
//  Агрегированные отчёты по модулям — объектам тестирования
// ---------------------------------------------------------------------------

// Корневому проекту нужен репозиторий: отсюда скачивается allure-commandline.
repositories {
    mavenCentral()
}

allure {
    version.set(allureCommandlineVersion)
}

dependencies {
    testedModules.forEach { "allureAggregateReport"(project(it)) }
}

/** Печатает покрытие строк и ветвлений по классам и модулям в консоль (из CSV-отчётов JaCoCo). */
val coverageSummary by tasks.registering {
    group = "verification"
    description = "Prints line and branch coverage per class and per module to the console."
    val modules = testedModules.map { project(it) }
    modules.forEach { m -> mustRunAfter(m.tasks.named("jacocoTestReport")) }
    doLast {
        data class Row(val pkg: String, val cls: String, val lm: Int, val lc: Int, val bm: Int, val bc: Int)
        fun pct(covered: Int, missed: Int): String =
            if (covered + missed == 0) "   n/a" else "%5.1f%%".format(100.0 * covered / (covered + missed))

        val showClasses = !project.flag("coverageModulesOnly")
        var tlm = 0; var tlc = 0; var tbm = 0; var tbc = 0
        val out = StringBuilder()
        out.appendLine()
        out.appendLine("=".repeat(96))
        out.appendLine("%-72s %10s %12s".format("Coverage (JaCoCo)", "Lines", "Branches"))
        out.appendLine("=".repeat(96))
        for (m in modules) {
            val csvFile = m.layout.buildDirectory.file("reports/jacoco/test/jacocoTestReport.csv").get().asFile
            if (!csvFile.exists()) {
                out.appendLine("%-72s %s".format(m.path, "no report (run tests first)"))
                continue
            }
            val rows = csvFile.readLines().drop(1).filter { it.isNotBlank() }.map { line ->
                val c = line.split(',')
                Row(c[1], c[2], c[7].toInt(), c[8].toInt(), c[5].toInt(), c[6].toInt())
            }
            val lm = rows.sumOf { it.lm }; val lc = rows.sumOf { it.lc }
            val bm = rows.sumOf { it.bm }; val bc = rows.sumOf { it.bc }
            tlm += lm; tlc += lc; tbm += bm; tbc += bc
            out.appendLine("%-72s %10s %12s".format("MODULE ${m.path}  (lines $lc/${lc + lm}, branches $bc/${bc + bm})", pct(lc, lm), pct(bc, bm)))
            if (showClasses) {
                // Служебные классы Kotlin (лямбды "new Function1() {...}", DefaultImpls) в списке
                // не показываем — их строки и ветвления всё равно учтены в итогах модуля.
                rows.filterNot { "new " in it.cls || it.cls.endsWith("DefaultImpls") }
                    .sortedWith(compareBy({ it.pkg }, { it.cls })).forEach { r ->
                    out.appendLine("    %-68s %10s %12s".format("${r.pkg}.${r.cls}".removePrefix("com.vargod."), pct(r.lc, r.lm), pct(r.bc, r.bm)))
                }
            }
            out.appendLine("-".repeat(96))
        }
        out.appendLine("%-72s %10s %12s".format("TOTAL  (lines $tlc/${tlc + tlm}, branches $tbc/${tbc + tbm})", pct(tlc, tlm), pct(tbc, tbm)))
        out.appendLine("=".repeat(96))
        logger.lifecycle(out.toString())
    }
}

/**
 * Главная точка входа ЛР1: юнит-тесты domain + business-logic + data-access,
 * затем покрытие (консоль; HTML — по модулям) и Allure. Отчёты строятся и при
 * падении тестов (finalizedBy), чтобы было видно, что именно упало.
 */
val unitTests by tasks.registering {
    group = "verification"
    description = "Runs unit tests of domain, business-logic and data-access, then builds coverage and Allure reports."
    testedModules.forEach { dependsOn("$it:test") }
    finalizedBy(coverageSummary, "allureAggregateReport")
}
