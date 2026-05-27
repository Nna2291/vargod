package com.vargod.config

/**
 * Типизированная конфигурация приложения Vargod.
 *
 * Загружается из внешнего файла `vargod.properties` через [ConfigLoader],
 * с переопределением переменными окружения (приоритет — у env).
 * Используется и технологическим UI (CLI), и API-сервером.
 */
data class AppConfig(
    /** Строка подключения к базе данных (JDBC URL). */
    val dbUrl: String,
    val dbUser: String,
    val dbPassword: String,
    /** Ключевой параметр бизнес-логики: максимальный шаг ОДУ-решателя. */
    val simulationMaxStep: Double,
    /** Относительная точность ОДУ-решателя. */
    val simulationRelativeTolerance: Double,
    /** Абсолютная точность ОДУ-решателя. */
    val simulationAbsoluteTolerance: Double,
    // ----- Auth (P1) -----
    /** Секрет для подписи JWT (HS256). Минимум 32 байта. */
    val jwtSecret: String,
    /** Время жизни токена в секундах (по умолчанию час). */
    val jwtTtlSeconds: Long,
    /** Email первого администратора. Если null/пусто — автосоздание не выполняется. */
    val adminEmail: String?,
    /** Пароль первого администратора (plain, превратится в BCrypt-хеш). */
    val adminPassword: String?,
    /** Список разрешённых CORS-origin'ов через запятую. `*` — только для dev. */
    val corsAllowedOrigins: List<String>,
)
