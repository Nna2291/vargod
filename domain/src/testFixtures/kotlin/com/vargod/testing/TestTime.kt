package com.vargod.testing

import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

/** Фиксированное «сейчас» для детерминированных тестов. */
object TestTime {
    /** Момент без наносекунд: PostgreSQL хранит timestamptz с точностью до микросекунд. */
    val NOW: Instant = Instant.parse("2026-01-15T10:00:00Z")

    /** Часы, всегда возвращающие [NOW]; внедряются в сервисы вместо системных. */
    val FIXED_CLOCK: Clock = Clock.fixed(NOW, ZoneOffset.UTC)

    fun minutesAfterNow(minutes: Long): Instant = NOW.plusSeconds(minutes * 60)
}
