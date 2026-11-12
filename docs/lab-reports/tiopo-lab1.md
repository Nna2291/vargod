# ТиОПО, ЛР №1 — модульное тестирование Vargod

Объект тестирования — backend проекта Vargod (Kotlin, Gradle multi-project):

| Модуль | Что это | В ЛР1 |
|---|---|---|
| `business-logic` | сервисы, движок валидации, ОДУ-движок и модели | **тестируется** |
| `data-access` | JDBC-репозитории, миграции Flyway | **тестируется** |
| `domain` | сущности, исключения, интерфейсы репозиториев; здесь же общие тестовые хелперы (`testFixtures`) | хелперы + пара тестов |
| `api`, `app-cli` | REST и консольный UI | вне ЛР1 |

Стек тестов: **JUnit 5** (Jupiter + Params), **MockK** (mock'и), **AssertJ** (проверки),
**embedded PostgreSQL 16** (zonky, без Docker), **JaCoCo** (покрытие), **Allure** (отчёт), **Gradle**.

Полная таблица «тест → техника подготовки данных» сгенерирована из исходников:
[tiopo-lab1-techniques.md](tiopo-lab1-techniques.md)
(`python3 scripts/techniques_table.py > docs/lab-reports/tiopo-lab1-techniques.md`).

---

## Как запустить

Нужен JDK 17. Docker не нужен: PostgreSQL для тестов БД поднимается embedded-процессом.

```bash
./scripts/run-tests.sh                     # тесты + покрытие (консоль + HTML) + Allure
./scripts/run-tests.sh --random            # случайный порядок
./scripts/run-tests.sh --seed <seed>       # повторить конкретный случайный порядок
./scripts/run-tests.sh --offline           # без сети: только тесты без БД (mock / in-memory)
./scripts/run-tests.sh --london            # только лондонский стиль (--classical — классический)
./scripts/run-tests.sh --tags technique-boundary-values   # только тесты одной техники
./scripts/run-tests.sh --forks 2 --fork-every 1 --module business-logic --no-report
./scripts/run-tests.sh --threads 4 --module business-logic --no-report
./scripts/run-tests.sh --serve             # после прогона поднять Allure на http://localhost:8090
```

Скрипт — тонкая обёртка над Gradle. То же самое напрямую:

```bash
./gradlew unitTests                        # :domain:test :business-logic:test :data-access:test
                                           # + coverageReport + coverageSummary + allureAggregateReport
./gradlew unitTests -PrandomOrder          # -PrandomSeed=..., -Poffline, -Pthreads=N,
                                           # -PmaxForks=N, -PforkEvery=N, -PincludeTags=..., -PexcludeTags=...
./gradlew coverageSummary                  # только напечатать покрытие (после тестов)
```

По умолчанию скрипт работает в экономном режиме для слабых машин
(`--no-daemon --max-workers=1`, компилятор Kotlin внутри процесса Gradle, heap тестовой JVM 512 МБ).
`--fast` этот режим выключает.

Где отчёты:

| Отчёт | Путь |
|---|---|
| Allure (сводный) | `build/reports/allure-report/allureAggregateReport/index.html`, открывать через HTTP (`--serve`) |
| Покрытие, сводное HTML | `build/reports/jacoco/coverageReport/html/index.html` |
| Покрытие по модулям | `<модуль>/build/reports/jacoco/test/html/index.html` |
| Стандартный отчёт Gradle | `<модуль>/build/reports/tests/test/index.html` |
| Учёт процессов и потоков | `<модуль>/build/test-processes/test/<pid>.pid` |

---

## Где что лежит

```
domain/src/testFixtures/kotlin/com/vargod/testing/
├── Tags.kt                         # аннотации-теги: техники, стиль (@London/@Classical), @DbTest
├── TestTime.kt                     # фиксированные часы (Clock) для детерминированных тестов
├── builders/Builders.kt            # Data Builder'ы: aUser(), anExperiment(), aValidator(), ...
├── mothers/Mothers.kt              # Object Mother'ы: UserMother.admin(), ExperimentMother.publicOf(...)
├── inmemory/InMemoryRepositories.kt# in-memory реализации репозиториев для классических тестов
└── ProcessReportingListener.kt     # считает тестовые JVM и потоки (требование 12)

business-logic/src/test/kotlin/com/vargod/
├── service/impl/*LondonTest.kt     # сервисы, лондонский стиль (MockK)
├── service/impl/*ClassicalTest.kt  # сервисы, классический стиль (реальные движки + in-memory)
├── engine/validation/*Test.kt      # правила валидации, реестр, движок валидации
└── engine/ode/*Test.kt             # ОДУ-модели (общий контракт OdeModelContractTest), реестр, движок

data-access/src/test/kotlin/com/vargod/repository/jdbc/
├── support/DbTestBase.kt           # фикстура: embedded PostgreSQL + очистка таблиц
├── Jdbc*RepositoryTest.kt          # репозитории, классический стиль (настоящая БД)
├── JdbcRepositoriesLondonTest.kt   # репозитории, лондонский стиль (mock JDBC)
├── JsonCodecTest.kt, SchemaInitializerTest.kt
```

---

## Выполнение требований

### 1. Не меньше двух тестов (позитивный + негативный) на каждый public-метод

Один тест-класс на каждый класс основных компонентов. Внутри на каждый публичный метод
есть минимум один позитивный и один негативный тест, имена тестов начинаются с имени
метода (`create - ...`, `getById - ...`), чтобы это было легко проверить.
Шаблонные проверки, общие для семи ОДУ-моделей (код, переменные, параметры, размерность),
вынесены в абстрактный [OdeModelContractTest](../../business-logic/src/test/kotlin/com/vargod/engine/ode/OdeModelContractTest.kt),
его наследует тест-класс каждой модели.

Сознательно не покрыты: `DataSeeder` (заполнение БД эталонными данными), `DataSourceFactory`
(вызывается косвенно из фикстуры БД), `ConfigLoader`. Это инфраструктура, а не бизнес-логика
и не доступ к данным. Модули `api` и `app-cli` вне ЛР1. Приватные методы отдельно не тестируются (п. 6).

### 2. Тесты на исключения

Ожидаемое исключение проверяется так: `assertThrows<T> { ... }` в секции Act, затем проверка сообщения в Assert:

```kotlin
// Act
val error = assertThrows<NotFoundException> { sut.getById(id) }
// Assert
assertThat(error.message).isEqualTo("Experiment not found: id=$id")
```

Примеры: [ExperimentServiceImplLondonTest.kt:110](../../business-logic/src/test/kotlin/com/vargod/service/impl/ExperimentServiceImplLondonTest.kt#L110)
(`ValidationException`), [JdbcRepositoriesLondonTest.kt:158](../../data-access/src/test/kotlin/com/vargod/repository/jdbc/JdbcRepositoriesLondonTest.kt#L158)
(`SQLException` → `PersistenceException` с откатом транзакции).

### 3. Классический и лондонский стиль

| | Классический (детройтский) | Лондонский (mockist) |
|---|---|---|
| Единица | единица поведения, несколько классов вместе | один класс |
| Зависимости | реальные объекты; заменяется только внешняя БД | все зависимости SUT — mock (MockK) |
| Что проверяем | итоговое состояние и результат | взаимодействия: что, с какими аргументами и в каком порядке вызвано |
| Тег | `style-classical` (`@Classical`) | `style-london` (`@London`) |

Один и тот же сценарий в обоих вариантах, для сравнения:

* `ExperimentServiceImpl.runSimulation`, переход PENDING → COMPLETED:
  лондонский, [ExperimentServiceImplLondonTest.kt:249](../../business-logic/src/test/kotlin/com/vargod/service/impl/ExperimentServiceImplLondonTest.kt#L249):
  `verifyOrder { save(RUNNING); solve(...); save(COMPLETED) }`;
  классический, [ExperimentServiceImplClassicalTest.kt:115](../../business-logic/src/test/kotlin/com/vargod/service/impl/ExperimentServiceImplClassicalTest.kt#L115):
  настоящий ОДУ-решатель, а в хранилище статус COMPLETED и результат совпадает с аналитическим решением.
* `ModelServiceImpl`: оба класса в [ModelServiceImplTest.kt](../../business-logic/src/test/kotlin/com/vargod/service/impl/ModelServiceImplTest.kt)
  (лондонский — строка 38, классический — строка 110).
* Репозитории: классические тесты на настоящем PostgreSQL ([JdbcExperimentRepositoryTest.kt](../../data-access/src/test/kotlin/com/vargod/repository/jdbc/JdbcExperimentRepositoryTest.kt))
  и лондонские на mock JDBC ([JdbcRepositoriesLondonTest.kt](../../data-access/src/test/kotlin/com/vargod/repository/jdbc/JdbcRepositoriesLondonTest.kt)).

In-memory репозитории в классических тестах — не mock и не stub: у них нет
запрограммированных ответов и проверок вызовов. Это рабочие упрощённые реализации
интерфейсов `domain.repository`; классическая школа заменяет только внешнюю
разделяемую зависимость (БД).

### 4. Arrange-Act-Assert, fixture и хелперы

Каждый тест разбит комментариями `// Arrange`, `// Act`, `// Assert`; в Act ровно один вызов
тестируемого метода. Фикстуры:

* `@BeforeEach setUp()` создаёт SUT заново для каждого теста (JUnit по умолчанию создаёт новый экземпляр тест-класса на каждый тест);
* `@ExtendWith(MockKExtension::class)` + `@MockK` создаёт mock-зависимости;
* [DbTestBase](../../data-access/src/test/kotlin/com/vargod/repository/jdbc/support/DbTestBase.kt): один PostgreSQL на JVM (`TestDatabase`), `TRUNCATE` перед каждым тестом, Arrange-хелперы `givenUser/givenModel/...`;
* хелперы `TestTime`, `RealEngines`, `Passwords`, builder'ы и mother'ы (п. 7).

Табличные проверки (несколько наборов данных) оформлены как `@ParameterizedTest`,
чтобы и в них Act оставался одним вызовом.

### 5. Переписанный под тестируемость код

`ExperimentServiceImpl`, `ComparisonServiceImpl`, `UserServiceImpl` и `AuditLogServiceImpl`
брали время из `Instant.now()`. Из-за этого `createdAt` нельзя было проверить точно, а результат
`create(...)` зависел от момента запуска. В эти классы внедрён `java.time.Clock`, например
[ExperimentServiceImpl.kt:32](../../business-logic/src/main/kotlin/com/vargod/service/impl/ExperimentServiceImpl.kt#L32).
Значение по умолчанию `Clock.systemUTC()`, поэтому `api` и `app-cli` не менялись. В тестах
подставляются фиксированные часы `TestTime.FIXED_CLOCK`.

### 6. Приватные и защищённые методы

Отдельно не тестируются, только через публичные методы (например, `RuleValidationEngine.resolve`
проверяется через `validate`).

### 7. Data Builder и Object Mother

* **Data Builder** — [Builders.kt](../../domain/src/testFixtures/kotlin/com/vargod/testing/builders/Builders.kt):
  у каждого поля есть значение по умолчанию, тест задаёт только важное для него:
  `anExperiment().ownedBy(owner).forModel(model.id).asPublic().completed().build()`.
* **Object Mother** — [Mothers.kt](../../domain/src/testFixtures/kotlin/com/vargod/testing/mothers/Mothers.kt):
  именованные объекты предметной области: `UserMother.admin()`, `UserMother.legacyWithInvalidHash()`,
  `ModelTemplateMother.logistic()` (с валидаторами), `ExperimentMother.publicOf(owner)`.
  Внутри собраны из builder'ов.
* Лежат в `testFixtures` модуля `domain` и подключаются в другие модули через
  `testImplementation(testFixtures(project(":domain")))`.

### 8. Запуск из командной строки

`./scripts/run-tests.sh` или `./gradlew unitTests` из корня локальной копии репозитория (см. выше).

### 9. Отчёт Allure

Адаптер `allure-junit5` подключён ко всем модулям через плагин `io.qameta.allure`.
Задача `allureAggregateReport` собирает общий отчёт по трём модулям; `unitTests` вызывает её
автоматически, даже если тесты упали (`finalizedBy`). В отчёте тесты сгруппированы
Epic → Feature (вкладка Behaviors), а JUnit-теги (техника, стиль, `db`) видны как Allure-теги.

### 10. Случайный порядок

Используются штатные механизмы JUnit 5: `ClassOrderer$Random` и `MethodOrderer$Random`.
Они задаются конфигурационными параметрами JUnit, а Gradle передаёт их тестовой JVM
системными свойствами, если указан `-PrandomOrder` (см. [build.gradle.kts:131](../../build.gradle.kts#L131)):

```
junit.jupiter.testclass.order.default  = org.junit.jupiter.api.ClassOrderer$Random
junit.jupiter.testmethod.order.default = org.junit.jupiter.api.MethodOrderer$Random
junit.jupiter.execution.order.random.seed = <seed>
```

Seed печатается в лог (`random order enabled, seed = ...`), поэтому любой «упавший» порядок
можно повторить: `-PrandomSeed=<seed>`. Тесты независимы друг от друга (новый SUT и mock'и
на каждый тест, очистка БД в `@BeforeEach`), поэтому от порядка не зависят.

### 11. Режим без доступа к интернету

`./scripts/run-tests.sh --offline` запускает Gradle с `--offline` и `-Poffline`, при этом:

* исключаются тесты с тегом `db` (им нужен PostgreSQL); остаются лондонские тесты
  и классические на in-memory репозиториях;
* в тестовой JVM `java.net`-сокеты направляются в несуществующий SOCKS-прокси
  (`socksProxyHost=127.0.0.1`, порт 9), поэтому тест, который полез бы в сеть, сразу упадёт.

Перед офлайн-прогоном нужен хотя бы один обычный запуск с сетью: Gradle скачает
зависимости, бинарник PostgreSQL и allure-commandline в локальный кэш. Проверка:
отключить сеть на машине и запустить `./scripts/run-tests.sh --offline`.

### 12. Сколько процессов и потоков запускается

Что работает при `./gradlew unitTests`:

| Процесс | Сколько | Чем управляется |
|---|---|---|
| Gradle (демон или одноразовый процесс при `--no-daemon`) | 1 | `org.gradle.daemon`, `--no-daemon` |
| Kotlin compile daemon | 0–1 | `kotlin.compiler.execution.strategy` (`in-process` — без отдельного процесса) |
| **Тестовая JVM** (Gradle Test Executor) | **1 на Test-задачу модуля**, всего 3, по очереди | `Test.maxParallelForks`, `Test.forkEvery` |
| PostgreSQL (только в `data-access`) | postmaster + служебные процессы + по backend-процессу на соединение | embedded-postgres |

Как регулируется (Gradle, [build.gradle.kts:126](../../build.gradle.kts#L126)):

* `maxParallelForks` (`--forks N`): до N тестовых JVM одного модуля работают параллельно,
  тест-классы распределяются между ними; по умолчанию 1;
* `forkEvery` (`--fork-every N`): JVM перезапускается после каждых N тест-классов;
  `--fork-every 1` — отдельный процесс на каждый тест-класс; по умолчанию 0, то есть никогда;
* `--parallel` у Gradle: Test-задачи разных модулей идут одновременно (по умолчанию выключено).

**Потоки** внутри одной тестовой JVM: по умолчанию JUnit выполняет все тесты последовательно
в одном потоке `Test worker`. `--threads N` (`-Pthreads=N`) включает параллельное выполнение
JUnit 5 (`junit.jupiter.execution.parallel.enabled=true`, стратегия `fixed`, N потоков).
Тесты с общей БД при этом сериализуются через `@ResourceLock("postgres")`.

Как проверить: `ProcessReportingListener` регистрируется через ServiceLoader и в каждой
тестовой JVM записывает файл `<pid>.pid` (сколько классов, тестов и потоков). Gradle печатает итог:

```
[:business-logic:test] test JVM processes: 1 (maxParallelForks=1, forkEvery=0, junit threads=1)
    pid 88313: test classes=31, tests=..., threads=1
```

С `--fork-every 1` процессов столько же, сколько тест-классов; с `--threads 4` в строке будет `threads=4`.
PID и имя потока видны и в Allure, в метке `thread` (`88313@host.Test worker(1)`).

### 13. Тесты проходят

`./scripts/run-tests.sh`: в выводе по каждому модулю строка `tests: N, passed: N, failed: 0`.

### 14. Покрытие строк и ветвлений из командной строки

JaCoCo подключён ко всем модулям; после каждой Test-задачи строится `jacocoTestReport`
(HTML, XML, CSV). Задача `coverageSummary` печатает в консоль покрытие **строк** и **ветвлений**
по каждому классу, модулю и итог; `coverageReport` строит сводный HTML по трём модулям.

```
./gradlew coverageSummary          # после тестов; -PcoverageModulesOnly — только итоги по модулям
```

Результат прогона (вставить вывод `coverageSummary`):

```
TODO: вставить итоговые строки MODULE ... и TOTAL ... после запуска
```

### 15. Защита от регрессии, устойчивость к рефакторингу, лёгкость поддержки

* Проверяется наблюдаемое поведение (результат, состояние, сообщение исключения),
  а не детали реализации. Лондонские тесты проверяют только те взаимодействия, которые
  входят в контракт (например, «при ошибке валидации ничего не сохраняется»).
* Builder'ы и mother'ы изолируют тесты от конструкторов сущностей: новое поле
  в `Experiment` правится в одном месте, а не в каждом тесте.
* Время детерминировано (`Clock`), тесты независимы и проходят в любом порядке.
* Общий контракт `OdeModelContractTest` убирает копипасту между семью моделями.

---

## Шпаргалка к защите

**Как фреймворк понимает, что перед ним тест?**
В JVM, как и в C#, тест определяется по аннотации, а не по имени. Gradle компилирует `src/test/kotlin`,
задача `test` с `useJUnitPlatform()` передаёт скомпилированные классы JUnit Platform Launcher'у,
а движок Jupiter через рефлексию ищет методы с `@Test`, `@ParameterizedTest`, `@TestFactory`.
Аналог в C#: тестовый проект (`.csproj` с `Microsoft.NET.Test.Sdk` и xUnit/NUnit), где тест —
метод с атрибутом `[Fact]`/`[Theory]` (xUnit), `[Test]` (NUnit) или `[TestMethod]` (MSTest);
`dotnet test` находит их рефлексией. Иначе устроены Go (функции `TestXxx(t *testing.T)` в файлах
`*_test.go`) и pytest (файлы `test_*.py`, функции `test_*`): там тест определяется соглашением об именах.
Так же было в JUnit 3, где тест — метод `testXxx()` в наследнике `TestCase`.

**Покажи классический и лондонский тест.** `ExperimentServiceImplClassicalTest` / `ExperimentServiceImplLondonTest`,
`runSimulation - PENDING ...` (строки 115 и 249), см. п. 3.

**Какие техники подготовки данных и пример каждой:**

| Техника | Пример |
|---|---|
| Классы эквивалентности | [ExperimentServiceImplLondonTest.kt:110](../../business-logic/src/test/kotlin/com/vargod/service/impl/ExperimentServiceImplLondonTest.kt#L110): валидные параметры / невалидные / несуществующая модель |
| Граничные значения | [ExperimentServiceImplClassicalTest.kt:83](../../business-logic/src/test/kotlin/com/vargod/service/impl/ExperimentServiceImplClassicalTest.kt#L83) и `:97`: `P = 0` при строгой границе `P > 0` отклоняется, `P = Double.MIN_VALUE` проходит; [JdbcAuditLogRepositoryTest.kt:109](../../data-access/src/test/kotlin/com/vargod/repository/jdbc/JdbcAuditLogRepositoryTest.kt#L109): `limit` = 0 / 1 / 1000 / 1001; [UserServiceImplLondonTest.kt:175](../../business-logic/src/test/kotlin/com/vargod/service/impl/UserServiceImplLondonTest.kt#L175): пароль 7 символов |
| Переходы состояний | [ExperimentServiceImplLondonTest.kt:298](../../business-logic/src/test/kotlin/com/vargod/service/impl/ExperimentServiceImplLondonTest.kt#L298): из RUNNING/COMPLETED/FAILED запуск запрещён; `:249`, `:273`: PENDING → RUNNING → COMPLETED / FAILED |
| Таблица решений | [ComparisonServiceImplClassicalTest.kt:97](../../business-logic/src/test/kotlin/com/vargod/service/impl/ComparisonServiceImplClassicalTest.kt#L97): «свой? × публичный?» → доступ; [ValidatorAdminServiceImplTest.kt:318](../../business-logic/src/test/kotlin/com/vargod/service/impl/ValidatorAdminServiceImplTest.kt#L318) |
| Попарное (комбинаторное) | [RangeRuleTest.kt:123](../../business-logic/src/test/kotlin/com/vargod/engine/validation/RangeRuleTest.kt#L123): 3 параметра, 6 наборов вместо 12; [JdbcAuditLogRepositoryTest.kt:80](../../data-access/src/test/kotlin/com/vargod/repository/jdbc/JdbcAuditLogRepositoryTest.kt#L80): 4 критерия фильтра, 6 наборов вместо 16 |
| Предугадывание ошибок | [AuthServiceImplClassicalTest.kt:94](../../business-logic/src/test/kotlin/com/vargod/service/impl/AuthServiceImplClassicalTest.kt#L94): битый хеш `!INVALID` у legacy-пользователя; [JdbcRepositoriesLondonTest.kt:158](../../data-access/src/test/kotlin/com/vargod/repository/jdbc/JdbcRepositoriesLondonTest.kt#L158): сбой посреди транзакции |

Отфильтровать тесты одной техники: `./scripts/run-tests.sh --tags technique-pairwise`.

**Сколько процессов/потоков, как регулировать?** п. 12: по одной тестовой JVM на модуль,
один поток; `maxParallelForks` / `forkEvery` (процессы), `-Pthreads` (потоки JUnit).

**Как устроен случайный порядок?** п. 10: `ClassOrderer$Random` + `MethodOrderer$Random`, seed в логе, `-PrandomSeed` для повтора.

**Почему именно столько тестов?** Задание требует не меньше двух тестов на каждый публичный метод.
В слоях бизнес-логики и доступа к данным около 120 публичных методов, отсюда ~250 тест-методов.
Сверх этого — по одному-двум наглядным примерам на технику.
