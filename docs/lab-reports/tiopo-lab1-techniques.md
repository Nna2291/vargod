# ТиОПО ЛР1 — техники подготовки тестовых данных

> Сгенерировано `python3 scripts/techniques_table.py` по аннотациям над тестами.
> Параметризованный тест (★) — один метод, несколько наборов данных.

## Сводка

| Техника | Тест-методов |
|---|---|
| Классы эквивалентности | 15 |
| Граничные значения | 6 |
| Переходы состояний | 6 |
| Предугадывание ошибок | 3 |
| **Всего тест-методов** | **30** |


### `ExperimentServiceImplTest` — business-logic, стиль: лондонский

Файл: [ExperimentServiceImplTest.kt](../../business-logic/src/test/kotlin/com/vargod/service/impl/ExperimentServiceImplTest.kt)

| Тест | Техника |
|---|---|
| create - valid request is validated and saved as PENDING | Классы эквивалентности |
| create - failed validation throws ValidationException and nothing is saved | Классы эквивалентности |
| getById - returns experiment found by repository | Классы эквивалентности |
| getById - missing experiment throws NotFoundException | Классы эквивалентности |
| listByUser - delegates to repository with the same user id | Классы эквивалентности |
| listByUser - repository failure is propagated unchanged | Предугадывание ошибок |
| listPublic - delegates to repository | Классы эквивалентности |
| listPublic - no public experiments gives empty list | Граничные значения |
| runSimulation - PENDING goes to RUNNING then COMPLETED with result | Переходы состояний |
| runSimulation - COMPLETED experiment cannot be started again | Переходы состояний |
| runSimulation - engine failure moves experiment to FAILED and rethrows | Переходы состояний |
| publish - owner makes private experiment public | Переходы состояний |
| publish - non-owner gets AuthorizationException | Классы эквивалентности |
| unpublish - owner makes public experiment private | Переходы состояний |
| unpublish - non-owner gets AuthorizationException | Классы эквивалентности |

### `JdbcExperimentRepositoryLondonTest` — data-access, стиль: лондонский

Файл: [JdbcExperimentRepositoryLondonTest.kt](../../data-access/src/test/kotlin/com/vargod/repository/jdbc/JdbcExperimentRepositoryLondonTest.kt)

| Тест | Техника |
|---|---|
| save - failure in the middle of the transaction rolls back and restores autocommit | Предугадывание ошибок |

### `JdbcExperimentRepositoryTest` — data-access, стиль: классический (PostgreSQL)

Файл: [JdbcExperimentRepositoryTest.kt](../../data-access/src/test/kotlin/com/vargod/repository/jdbc/JdbcExperimentRepositoryTest.kt)

| Тест | Техника |
|---|---|
| findById - restores saved experiment with parameters and validator snapshot | Классы эквивалентности |
| findById - unknown id gives null | Классы эквивалентности |
| save - second save updates status, stores result and does not duplicate children | Переходы состояний |
| save - name of 256 characters is rejected before touching the database | Граничные значения |
| findAllByUserId - returns only experiments of that user in creation order | Классы эквивалентности |
| findAllByUserId - user without experiments gets empty list | Граничные значения |
| findAllPublic - returns public experiments with their results | Классы эквивалентности |
| findAllPublic - no public experiments gives empty list | Граничные значения |
| findAllPaginated - second page of size 2 holds the oldest experiment | Классы эквивалентности |
| findAllPaginated - limit 0 is rejected | Граничные значения |
| findAllByIds - returns only the requested experiments | Классы эквивалентности |
| findAllByIds - empty id list gives empty result | Граничные значения |
| deleteById - removes experiment together with parameters and result | Классы эквивалентности |
| deleteById - unknown id is a silent no-op | Предугадывание ошибок |
