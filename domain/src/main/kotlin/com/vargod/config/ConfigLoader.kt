package com.vargod.config

import org.slf4j.LoggerFactory
import java.io.File
import java.util.Properties

/**
 * Загрузчик конфигурации из внешнего файла `vargod.properties`.
 *
 * Порядок поиска файла:
 *   1. системное свойство `-Dvargod.config=<путь>`;
 *   2. переменная окружения `VARGOD_CONFIG=<путь>`;
 *   3. файл `./vargod.properties` в рабочей директории;
 *   4. ресурс `vargod.properties` из classpath.
 *
 * Если файл не найден, используются значения по умолчанию.
 * Переменные окружения имеют приоритет над значениями файла — это нужно
 * для запуска в Docker-контейнере, где параметры задаются через env.
 */
object ConfigLoader {

    private val log = LoggerFactory.getLogger(ConfigLoader::class.java)

    fun load(env: Map<String, String> = System.getenv()): AppConfig {
        val props = Properties()
        val source = readProperties(props)
        log.info("Конфигурация загружена из источника: {}", source)

        fun value(key: String, envKey: String, default: String): String =
            env[envKey] ?: props.getProperty(key) ?: default

        fun optional(key: String, envKey: String): String? =
            env[envKey]?.takeIf { it.isNotBlank() }
                ?: props.getProperty(key)?.takeIf { it.isNotBlank() }

        val config = AppConfig(
            dbUrl = value("db.url", "DB_URL", "jdbc:postgresql://localhost:5432/vargod"),
            dbUser = value("db.user", "DB_USER", "vargod"),
            dbPassword = value("db.password", "DB_PASSWORD", "vargod"),
            simulationMaxStep =
                value("simulation.maxStep", "SIMULATION_MAX_STEP", "1.0").toDouble(),
            simulationRelativeTolerance =
                value("simulation.relativeTolerance", "SIMULATION_REL_TOL", "1.0e-10").toDouble(),
            simulationAbsoluteTolerance =
                value("simulation.absoluteTolerance", "SIMULATION_ABS_TOL", "1.0e-10").toDouble(),
            jwtSecret = value(
                "vargod.jwt.secret", "VARGOD_JWT_SECRET",
                "dev-only-change-me-in-production-32bytes-minimum-please",
            ),
            jwtTtlSeconds = value("vargod.jwt.ttlSeconds", "VARGOD_JWT_TTL_SECONDS", "3600").toLong(),
            adminEmail = optional("vargod.admin.email", "VARGOD_ADMIN_EMAIL"),
            adminPassword = optional("vargod.admin.password", "VARGOD_ADMIN_PASSWORD"),
            corsAllowedOrigins = value(
                "vargod.cors.allowedOrigins", "VARGOD_CORS_ALLOWED_ORIGINS",
                "http://localhost:5173,http://localhost:8081",
            ).split(',').map { it.trim() }.filter { it.isNotEmpty() },
        )
        log.info(
            "Параметры конфигурации: db.url={}, simulation.maxStep={}, jwt.ttl={}s, adminBootstrap={}, cors={}",
            config.dbUrl, config.simulationMaxStep, config.jwtTtlSeconds,
            if (config.adminEmail != null) "ENABLED(${config.adminEmail})" else "disabled",
            config.corsAllowedOrigins,
        )
        return config
    }

    private fun readProperties(props: Properties): String {
        val explicit = System.getProperty("vargod.config") ?: System.getenv("VARGOD_CONFIG")
        if (explicit != null) {
            val file = File(explicit)
            if (file.isFile) {
                file.inputStream().use { props.load(it) }
                return file.absolutePath
            }
            log.warn("Указанный конфиг-файл не найден: {}", explicit)
        }
        val cwdFile = File("vargod.properties")
        if (cwdFile.isFile) {
            cwdFile.inputStream().use { props.load(it) }
            return cwdFile.absolutePath
        }
        val classpath = ConfigLoader::class.java.classLoader.getResourceAsStream("vargod.properties")
        if (classpath != null) {
            classpath.use { props.load(it) }
            return "classpath:vargod.properties"
        }
        return "значения по умолчанию (файл не найден)"
    }
}
