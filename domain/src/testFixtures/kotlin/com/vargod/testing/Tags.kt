package com.vargod.testing

import org.junit.jupiter.api.Tag

/**
 * JUnit-теги, которыми размечены все тесты (ТиОПО, ЛР1).
 *
 * Каждая аннотация ниже — JUnit 5 meta-annotation над [Tag]. Это даёт три вещи:
 *  1. техника подготовки тестовых данных записана прямо над тестом (задание 3);
 *  2. allure-junit5 превращает JUnit-теги в Allure-теги — они видны в отчёте;
 *  3. запуск можно фильтровать: `./gradlew unitTests -PincludeTags=technique-boundary-values`.
 */
object Tags {
    // --- Техники тест-дизайна (подготовки тестовых данных) ---
    const val EQUIVALENCE_PARTITIONING = "technique-equivalence-partitioning"
    const val BOUNDARY_VALUES = "technique-boundary-values"
    const val STATE_TRANSITION = "technique-state-transition"
    const val DECISION_TABLE = "technique-decision-table"
    const val PAIRWISE = "technique-pairwise"
    const val ERROR_GUESSING = "technique-error-guessing"

    // --- Школа юнит-тестирования ---
    /** Классическая (детройтская) школа: реальные объекты, проверка состояния, без mock/stub. */
    const val CLASSICAL = "style-classical"
    /** Лондонская (mockist) школа: все зависимости SUT заменены mock-объектами MockK. */
    const val LONDON = "style-london"

    // --- Внешние ресурсы ---
    /** Тесту нужен настоящий PostgreSQL (embedded-процесс). Исключается в режиме -Poffline. */
    const val DB = "db"
}

/** Классы эквивалентности: по одному представителю из каждого класса входных данных. */
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@Tag(Tags.EQUIVALENCE_PARTITIONING)
annotation class EquivalencePartitioning

/** Анализ граничных значений: значения на границе, сразу до и сразу после неё. */
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@Tag(Tags.BOUNDARY_VALUES)
annotation class BoundaryValues

/** Переходы состояний: допустимые и недопустимые переходы конечного автомата сущности. */
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@Tag(Tags.STATE_TRANSITION)
annotation class StateTransition

/** Таблица решений: комбинации условий → ожидаемое действие (каждый столбец — тест). */
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@Tag(Tags.DECISION_TABLE)
annotation class DecisionTable

/** Попарное (частный случай комбинаторного): каждая пара значений любых двух параметров встречается хотя бы раз. */
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@Tag(Tags.PAIRWISE)
annotation class Pairwise

/** Предугадывание ошибок: типичные сбои (битые данные, отказ БД, null, неверный формат). */
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@Tag(Tags.ERROR_GUESSING)
annotation class ErrorGuessing

/** Тест-класс написан в классическом стиле (без mock/stub). */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@java.lang.annotation.Inherited
@Tag(Tags.CLASSICAL)
annotation class Classical

/** Тест-класс написан в лондонском стиле (все зависимости — mock). */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@java.lang.annotation.Inherited
@Tag(Tags.LONDON)
annotation class London

/** Тест-класс работает с настоящей БД PostgreSQL. */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@java.lang.annotation.Inherited
@Tag(Tags.DB)
annotation class DbTest
