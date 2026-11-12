# ТиОПО, ЛР №1 — модульное тестирование Vargod

## Объект тестирования

Backend проекта Vargod (Kotlin, Gradle multi-project). Основные классы двух компонентов:

| Компонент | Основной класс | Публичные методы |
|---|---|---|
| Бизнес-логика (`business-logic`) | `ExperimentServiceImpl` — создание экспериментов, запуск симуляции, публикация | `create`, `getById`, `listByUser`, `listPublic`, `runSimulation`, `publish`, `unpublish` |
| Доступ к данным (`data-access`) | `JdbcExperimentRepository` — хранение экспериментов в PostgreSQL | `findById`, `save`, `findAllByUserId`, `findAllPublic`, `findAllPaginated`, `findAllByIds`, `deleteById` |

Тесты (30):

| Тест-класс | Стиль | Тестов |
|---|---|---|
| [ExperimentServiceImplTest](../../business-logic/src/test/kotlin/com/vargod/service/impl/ExperimentServiceImplTest.kt) | лондонский (MockK) | 15 |
| [JdbcExperimentRepositoryTest](../../data-access/src/test/kotlin/com/vargod/repository/jdbc/JdbcExperimentRepositoryTest.kt) | классический (настоящий PostgreSQL) | 14 |
| [JdbcExperimentRepositoryLondonTest](../../data-access/src/test/kotlin/com/vargod/repository/jdbc/JdbcExperimentRepositoryLondonTest.kt) | лондонский (mock JDBC) | 1 |

Стек: JUnit 5, MockK, AssertJ, embedded PostgreSQL 16 (zonky, без Docker), JaCoCo, Allure, Gradle.
Таблица «тест → техника»: [tiopo-lab1-techniques.md](tiopo-lab1-techniques.md).

## Как запустить

Нужен JDK 17 (на macOS: `brew install --cask temurin@17`).

```bash
./scripts/run-tests.sh                     # тесты + покрытие в консоль + отчёт Allure
./scripts/run-tests.sh --random            # случайный порядок (seed печатается)
./scripts/run-tests.sh --seed <seed>       # повторить тот же порядок
./scripts/run-tests.sh --offline           # без сети: только тесты без БД
./scripts/run-tests.sh --london            # только лондонский стиль (--classical — классический)
./scripts/run-tests.sh --forks 2 --fork-every 1 --no-report   # процессы
./scripts/run-tests.sh --threads 4 --no-report                # потоки
./scripts/run-tests.sh --serve             # Allure на http://localhost:8090
```

Напрямую через Gradle: `./gradlew unitTests` (флаги: `-PrandomOrder`, `-PrandomSeed=`, `-Poffline`,
`-Pthreads=`, `-PmaxForks=`, `-PforkEvery=`, `-PincludeTags=`).

Отчёты: Allure — `build/reports/allure-report/allureAggregateReport/`, покрытие —
`<модуль>/build/reports/jacoco/test/html/index.html`, отчёт Gradle — `<модуль>/build/reports/tests/test/index.html`.

## Выполнение требований

**1. Тесты на каждый public-метод.** У каждого основного класса свой тест-класс; на каждый из
7 публичных методов есть позитивный и негативный тест. Имя теста начинается с имени метода
(`create - ...`, `findById - ...`).

**2. Исключения.** Ожидаемое исключение ловится `assertThrows<T> { ... }` в секции Act,
сообщение проверяется в Assert, например
[ExperimentServiceImplTest.kt:141](../../business-logic/src/test/kotlin/com/vargod/service/impl/ExperimentServiceImplTest.kt#L141)
(`NotFoundException`) и [JdbcExperimentRepositoryLondonTest.kt:56](../../data-access/src/test/kotlin/com/vargod/repository/jdbc/JdbcExperimentRepositoryLondonTest.kt#L56)
(`SQLException` → `PersistenceException`).

**3. Классический и лондонский стиль.**

* Лондонский — [ExperimentServiceImplTest](../../business-logic/src/test/kotlin/com/vargod/service/impl/ExperimentServiceImplTest.kt):
  все зависимости сервиса (два репозитория, два движка) — mock'и MockK. Проверяются вызовы и
  их порядок, например `verifyOrder { save(RUNNING); solve(...); save(COMPLETED) }` (строка 216).
* Классический — [JdbcExperimentRepositoryTest](../../data-access/src/test/kotlin/com/vargod/repository/jdbc/JdbcExperimentRepositoryTest.kt):
  без mock/stub, настоящий PostgreSQL; проверяется состояние базы после вызова.
* Один и тот же класс в обоих стилях — `JdbcExperimentRepository`: классические тесты на БД и
  лондонский [JdbcExperimentRepositoryLondonTest](../../data-access/src/test/kotlin/com/vargod/repository/jdbc/JdbcExperimentRepositoryLondonTest.kt),
  где вся JDBC-цепочка замокана, чтобы проверить откат транзакции.

**4. Arrange-Act-Assert и fixture.** Каждый тест разбит на `// Arrange`, `// Act`, `// Assert`,
в Act один вызов. Фикстуры: `@BeforeEach setUp()` создаёт тестируемый объект заново;
`@ExtendWith(MockKExtension::class)` + `@MockK` создают mock'и;
[DbTestBase](../../data-access/src/test/kotlin/com/vargod/repository/jdbc/support/DbTestBase.kt) поднимает
один PostgreSQL на JVM и очищает таблицы перед каждым тестом.

**5. Переписанный код.** `ExperimentServiceImpl.create` брал время из `Instant.now()`, поэтому
`createdAt` нельзя было проверить. В сервис внедрён `java.time.Clock` (значение по умолчанию
`Clock.systemUTC()`, вызывающий код не менялся), в тесте — фиксированные часы `TestTime.FIXED_CLOCK`.

**6. Приватные методы** отдельно не тестируются, только через публичные.

**7. Data Builder и Object Mother** — в `domain/src/testFixtures`:
* [Builders.kt](../../domain/src/testFixtures/kotlin/com/vargod/testing/builders/Builders.kt):
  `anExperiment().ownedBy(owner).asPublic().completed().build()` — задаём только важные поля;
* [Mothers.kt](../../domain/src/testFixtures/kotlin/com/vargod/testing/mothers/Mothers.kt):
  `ExperimentMother.publicOf(owner)`, `ModelTemplateMother.logistic()`, `CreateExperimentRequestMother.negativePopulation(...)`.

**8. Запуск из командной строки** — `./scripts/run-tests.sh` или `./gradlew unitTests`.

**9. Allure** — адаптер `allure-junit5`, отчёт `allureAggregateReport` строится автоматически
после тестов. Техника и стиль каждого теста видны в отчёте как теги.

**10. Случайный порядок.** Штатные механизмы JUnit 5 `ClassOrderer$Random` и `MethodOrderer$Random`,
включаются флагом `-PrandomOrder` (Gradle передаёт их тестовой JVM системными свойствами,
[build.gradle.kts:150](../../build.gradle.kts#L150)). Seed печатается в лог, повтор — `-PrandomSeed=<seed>`.

**11. Без интернета.** `--offline`: Gradle работает из локального кэша, тесты с тегом `db`
(им нужен PostgreSQL) исключаются, исходящие сокеты тестовой JVM блокируются. Остаются тесты
на mock'ах — они проходят. Перед этим нужен один обычный запуск с сетью.

**12. Процессы и потоки.** По умолчанию Gradle запускает **одну тестовую JVM на модуль**
(всего 2, по очереди), внутри неё тесты идут **в одном потоке** `Test worker`. Регулируется:
* `maxParallelForks` (`--forks N`) — до N тестовых JVM на модуль параллельно;
* `forkEvery` (`--fork-every N`) — новая JVM после каждых N тест-классов (`1` — процесс на класс);
* `-Pthreads=N` — параллельное выполнение JUnit 5 внутри JVM в N потоков.

Gradle печатает фактическое число процессов и потоков, например
`[:business-logic:test] test JVM processes: 1 ... pid 17993: test classes=1, tests=15, threads=1`.
Кроме того, в `data-access` работает процесс PostgreSQL.

**13. Тесты проходят** — в выводе по каждому модулю `tests: N, passed: N, failed: 0`.

**14. Покрытие строк и ветвлений.** JaCoCo; задача `coverageSummary` печатает в консоль покрытие
строк и ветвлений по классам и итог. Считается по основным классам и их зависимостям внутри
компонента ([build.gradle.kts:59](../../build.gradle.kts#L59)): `ExperimentServiceImpl` + движки
симуляции и валидации; `JdbcExperimentRepository` + маппинг строк и JSON.

**15. Принципы.** Проверяется наблюдаемое поведение, а не реализация; builder'ы и mother'ы
изолируют тесты от конструкторов сущностей; время детерминировано; тесты независимы друг от друга.

## Техники подготовки тестовых данных

| Техника | Пример |
|---|---|
| Классы эквивалентности | [ExperimentServiceImplTest.kt:80](../../business-logic/src/test/kotlin/com/vargod/service/impl/ExperimentServiceImplTest.kt#L80) / `:107` — корректные и некорректные параметры эксперимента |
| Граничные значения | [JdbcExperimentRepositoryTest.kt:93](../../data-access/src/test/kotlin/com/vargod/repository/jdbc/JdbcExperimentRepositoryTest.kt#L93) — имя длиной 256 при максимуме 255; `:184` — `limit = 0` при допустимых 1..1000; `:214` — пустой список id |
| Переходы состояний | [ExperimentServiceImplTest.kt:216](../../business-logic/src/test/kotlin/com/vargod/service/impl/ExperimentServiceImplTest.kt#L216) — PENDING → RUNNING → COMPLETED; `:255` — → FAILED; `:240` — из COMPLETED запуск запрещён |
| Предугадывание ошибок | [JdbcExperimentRepositoryLondonTest.kt:56](../../data-access/src/test/kotlin/com/vargod/repository/jdbc/JdbcExperimentRepositoryLondonTest.kt#L56) — сбой БД посреди транзакции |

## Шпаргалка к защите

**Как фреймворк понимает, что перед ним тест?** По аннотации, через рефлексию. Gradle компилирует
`src/test/kotlin`, задача `test` с `useJUnitPlatform()` отдаёт классы JUnit Platform, а движок
Jupiter ищет методы с `@Test`. В C# так же: тестовый проект (`.csproj` с xUnit/NUnit), тест — метод
с атрибутом `[Fact]` (xUnit) или `[Test]` (NUnit), `dotnet test` находит их рефлексией. В Go и pytest
наоборот — по соглашению об именах (`TestXxx` в `*_test.go`, `test_*` в `test_*.py`).

**Сколько процессов/потоков?** Одна тестовая JVM на модуль, один поток; `--forks`/`--fork-every` — процессы,
`--threads` — потоки (п. 12).

**Как устроен случайный порядок?** `ClassOrderer$Random` + `MethodOrderer$Random`, seed в логе (п. 10).
