# How to add a new ODE model

5 шагов. Допустим, добавляем модель `EXPONENTIAL_DECAY`: `dP/dt = -λ · P`.

## 1. Реализовать `OdeModel`

`business-logic/src/main/kotlin/com/vargod/engine/ode/ExponentialDecayModel.kt`:

```kotlin
package com.vargod.engine.ode

import com.vargod.engine.OdeModel

class ExponentialDecayModel : OdeModel {
    override fun code(): String = CODE
    override fun variables(): List<String> = listOf("P")
    override fun parameters(): List<String> = listOf("lambda")

    override fun derivatives(state: DoubleArray, params: Map<String, Double>, t: Double): DoubleArray {
        require(state.size == 1) { "ExponentialDecay expects 1 state variable [P], got ${state.size}" }
        val p = state[0]
        val lambda = params["lambda"] ?: throw IllegalArgumentException("Missing parameter 'lambda'")
        return doubleArrayOf(-lambda * p)
    }

    companion object { const val CODE = "EXPONENTIAL_DECAY" }
}
```

Принципы:
- `code()` — UPPER_SNAKE_CASE, должен быть стабильным (используется как ключ в БД).
- `variables()` — порядок переменных в `state` массиве.
- `parameters()` — список ожидаемых ключей в `params`.
- Все проверки — `require(...)` с понятным сообщением.

## 2. Зарегистрировать в `MapModelRegistry`

`business-logic/.../engine/ode/MapModelRegistry.kt` — добавьте экземпляр модели в карту:

```kotlin
private val byCode: Map<String, OdeModel> = listOf(
    SirModel(),
    LotkaVolterraModel(),
    // ...
    ExponentialDecayModel(),         // <- здесь
).associateBy(OdeModel::code)
```

## 3. Добавить seed-функцию в `DataSeeder`

`data-access/src/main/kotlin/com/vargod/repository/jdbc/DataSeeder.kt`:

а) В `seedAllIfMissing` дополните список:

```kotlin
"EXPONENTIAL_DECAY" to ::seedExponentialDecay,
```

б) Реализуйте seed-функцию ниже по файлу (по образцу `seedLogistic`):

```kotlin
private fun seedExponentialDecay(conn: Connection) {
    val id = UUID.randomUUID()
    insertModel(
        conn, id, "EXPONENTIAL_DECAY",
        name = "Экспоненциальное затухание",
        description = "dP/dt = -λ·P. Радиоактивный распад, элиминация препарата."
    )
    insertRange(conn, id, "lambda", min = 0.0, minExclusive = true)
    insertRange(conn, id, "P", min = 0.0)
}
```

Идемпотентность обеспечена: `existsByCode(conn, "EXPONENTIAL_DECAY")` отфильтрует повторный запуск.

## 4. Написать unit-тесты

`business-logic/src/test/kotlin/com/vargod/engine/ode/ExponentialDecayModelTest.kt`:

```kotlin
package com.vargod.engine.ode

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import kotlin.math.exp

class ExponentialDecayModelTest {
    private val model = ExponentialDecayModel()

    @Test
    fun `derivatives are -lambda P`() {
        val d = model.derivatives(doubleArrayOf(100.0), mapOf("lambda" to 0.1), 0.0)
        assertEquals(-10.0, d[0], 1e-12)
    }

    @Test
    fun `missing parameter throws`() {
        kotlin.test.assertFails {
            model.derivatives(doubleArrayOf(1.0), emptyMap(), 0.0)
        }
    }
}
```

Желательно ещё end-to-end через `OdeSimulationEngine` (см. `OdeSimulationEngineTest.kt` как образец) и сверка с аналитическим решением `P(t) = P0 · exp(-λ·t)`.

## 5. Solver hints (опционально)

Если модель **жёсткая** (как `HODGKIN_HUXLEY`), задайте рекомендованные параметры интегратора прямо в seed (`recommended_max_step`, `recommended_rel_tol`, `recommended_abs_tol` колонки в `model_templates`, миграция `V7`). Для простых нежёстких моделей оставьте дефолты (`maxStep=1.0`, `relTol=absTol=1e-10`).

## 6. Проверить локально

```bash
./gradlew :business-logic:test :data-access:test :api:assemble
docker compose up -d --build
curl http://localhost:8080/api/models | jq '.[] | .code'
# Должен появиться "EXPONENTIAL_DECAY".
```

В frontend ничего менять не надо — каталог моделей и форма параметров строятся автоматически из ответа `/api/models`.

## Чек-лист PR

- [ ] Класс модели реализует `OdeModel`, тесты покрывают derivatives + missing-param.
- [ ] Регистрация в `MapModelRegistry`.
- [ ] Seed-функция в `DataSeeder`, добавлена в список `seedAllIfMissing`.
- [ ] Запись в [docs/models.md](models.md): уравнения, параметры, типичные значения, ссылка.
- [ ] (Если жёсткая) — solver hints в seed.
- [ ] CHANGELOG.md обновлён в разделе `[Unreleased] / Added`.
