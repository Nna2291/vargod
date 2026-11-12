# ТиОПО ЛР1 — техники подготовки тестовых данных

> Сгенерировано `python3 scripts/techniques_table.py` по аннотациям над тестами.
> Параметризованный тест (★) — один метод, несколько наборов данных.
> Тесты базового класса `OdeModelContractTest` наследуют все 7 классов ОДУ-моделей.

## Сводка

| Техника | Тест-методов |
|---|---|
| Классы эквивалентности | 145 |
| Предугадывание ошибок | 53 |
| Граничные значения | 44 |
| Переходы состояний | 11 |
| Таблица решений | 5 |
| Попарное (комбинаторное) | 2 |
| **Всего тест-методов** | **260** |


### `DomainExceptionsTest` — domain, стиль: классический

Файл: [DomainExceptionsTest.kt](../../domain/src/test/kotlin/com/vargod/domain/DomainExceptionsTest.kt)

| Тест | Техника |
|---|---|
| NotFoundException - message names entity and id | Классы эквивалентности |
| ValidationException - message lists only failed validators | Классы эквивалентности |
| PersistenceException - keeps the original cause | Классы эквивалентности |

### `FitzHughNagumoModelTest` — business-logic, стиль: классический

Файл: [FitzHughNagumoModelTest.kt](../../business-logic/src/test/kotlin/com/vargod/engine/ode/FitzHughNagumoModelTest.kt)

| Тест | Техника |
|---|---|
| derivatives - known point matches FitzHugh-Nagumo equations | Классы эквивалентности |
| derivatives - zero time constant tau is rejected | Граничные значения |

### `HodgkinHuxleyModelTest` — business-logic, стиль: классический

Файл: [HodgkinHuxleyModelTest.kt](../../business-logic/src/test/kotlin/com/vargod/engine/ode/HodgkinHuxleyModelTest.kt)

| Тест | Техника |
|---|---|
| derivatives - removable singularity at V = -55 is continuous | Граничные значения |
| solve - injected current of 10 produces an action potential within 50 ms | Классы эквивалентности |

### `LogisticGrowthModelTest` — business-logic, стиль: классический

Файл: [LogisticGrowthModelTest.kt](../../business-logic/src/test/kotlin/com/vargod/engine/ode/LogisticGrowthModelTest.kt)

| Тест | Техника |
|---|---|
| derivatives - population exactly at carrying capacity does not grow | Граничные значения |
| derivatives - zero carrying capacity is rejected | Граничные значения |

### `LotkaVolterraModelTest` — business-logic, стиль: классический

Файл: [LotkaVolterraModelTest.kt](../../business-logic/src/test/kotlin/com/vargod/engine/ode/LotkaVolterraModelTest.kt)

| Тест | Техника |
|---|---|
| derivatives - non-trivial equilibrium x = gamma over delta, y = alpha over beta is stationary | Классы эквивалентности |

### `MapModelRegistryTest` — business-logic, стиль: классический

Файл: [MapModelRegistryTest.kt](../../business-logic/src/test/kotlin/com/vargod/engine/ode/MapModelRegistryTest.kt)

| Тест | Техника |
|---|---|
| getByCode - returns the registered model | Классы эквивалентности |
| getByCode - unknown code gives IllegalArgumentException | Классы эквивалентности |
| listAll - contains all seven production models keyed by code | Классы эквивалентности |
| listAll - empty registry is empty | Граничные значения |
| constructor - two models with the same code are rejected | Предугадывание ошибок |

### `OdeModelContractTest` — business-logic, стиль: классический

Файл: [OdeModelContractTest.kt](../../business-logic/src/test/kotlin/com/vargod/engine/ode/OdeModelContractTest.kt)

| Тест | Техника |
|---|---|
| code - equals the documented model code | Классы эквивалентности |
| code - does not collide with codes of other models | Предугадывание ошибок |
| variables - are declared in solver order | Классы эквивалентности |
| variables - share no names with parameters | Предугадывание ошибок |
| parameters - are declared as documented | Классы эквивалентности |
| parameters - the last declared parameter is required by derivatives | Классы эквивалентности |
| derivatives - valid point gives one finite derivative per variable | Классы эквивалентности |
| derivatives - state one element shorter is rejected | Граничные значения |

### `OdeSimulationEngineClassicalTest` — business-logic, стиль: классический

Файл: [OdeSimulationEngineTest.kt](../../business-logic/src/test/kotlin/com/vargod/engine/ode/OdeSimulationEngineTest.kt)

| Тест | Техника |
|---|---|
| solve - logistic growth follows the analytical solution | Классы эквивалентности |
| solve - step equal to the whole horizon gives exactly two samples | Граничные значения |
| solve - zero-length horizon is rejected | Граничные значения |
| solve - missing initial value of a state variable is reported | Предугадывание ошибок |

### `OdeSimulationEngineLondonTest` — business-logic, стиль: лондонский

Файл: [OdeSimulationEngineTest.kt](../../business-logic/src/test/kotlin/com/vargod/engine/ode/OdeSimulationEngineTest.kt)

| Тест | Техника |
|---|---|
| solve - recommended max step from template overrides engine default | Классы эквивалентности |
| solve - model returning wrong number of derivatives is reported | Предугадывание ошибок |

### `RosenzweigMacArthurModelTest` — business-logic, стиль: классический

Файл: [RosenzweigMacArthurModelTest.kt](../../business-logic/src/test/kotlin/com/vargod/engine/ode/RosenzweigMacArthurModelTest.kt)

| Тест | Техника |
|---|---|
| derivatives - known point matches equations with type II functional response | Классы эквивалентности |

### `SeirdModelTest` — business-logic, стиль: классический

Файл: [SeirdModelTest.kt](../../business-logic/src/test/kotlin/com/vargod/engine/ode/SeirdModelTest.kt)

| Тест | Техника |
|---|---|
| derivatives - known point matches SEIRD equations and conserves population | Классы эквивалентности |

### `SirModelTest` — business-logic, стиль: классический

Файл: [SirModelTest.kt](../../business-logic/src/test/kotlin/com/vargod/engine/ode/SirModelTest.kt)

| Тест | Техника |
|---|---|
| derivatives - known point matches SIR equations and conserves population | Классы эквивалентности |
| derivatives - zero population N is rejected | Граничные значения |

### `MapConstraintRuleRegistryTest` — business-logic, стиль: классический

Файл: [MapConstraintRuleRegistryTest.kt](../../business-logic/src/test/kotlin/com/vargod/engine/validation/MapConstraintRuleRegistryTest.kt)

| Тест | Техника |
|---|---|
| getByType - returns the registered rule instance | Классы эквивалентности |
| getByType - unknown type gives IllegalArgumentException | Классы эквивалентности |
| listAll - keeps registration order and keys rules by type | Классы эквивалентности |
| listAll - registry without rules is empty | Граничные значения |
| constructor - duplicate rule type is rejected | Предугадывание ошибок |
| constructor - blank rule type is rejected | Граничные значения |

### `OrderedRuleTest` — business-logic, стиль: классический

Файл: [OrderedRuleTest.kt](../../business-logic/src/test/kotlin/com/vargod/engine/validation/OrderedRuleTest.kt)

| Тест | Техника |
|---|---|
| type - is ORDERED | Классы эквивалентности |
| type - differs from every other production rule | Предугадывание ошибок |
| paramsSchema - requires fields and direction with fixed allowed values | Классы эквивалентности |
| paramsSchema - reports both missing required parameters | Предугадывание ошибок |
| check - strict ordering in requested direction | Классы эквивалентности |
| check - a single field is below the minimum of two | Граничные значения |
| check - unknown direction is reported | Предугадывание ошибок |
| validateParams - two fields are the minimal valid configuration | Граничные значения |
| validateParams - one field is rejected | Граничные значения |

### `RangeRuleTest` — business-logic, стиль: классический

Файл: [RangeRuleTest.kt](../../business-logic/src/test/kotlin/com/vargod/engine/validation/RangeRuleTest.kt)

| Тест | Техника |
|---|---|
| type - is RANGE | Классы эквивалентности |
| type - differs from every other production rule | Предугадывание ошибок |
| paramsSchema - declares four optional fields | Классы эквивалентности |
| paramsSchema - rejects non-numeric bound | Предугадывание ошибок |
| check - pairwise combinations of bound strictness and value position | Попарное (комбинаторное) |
| check - value one ulp below inclusive minimum is rejected | Граничные значения |
| check - missing field value is reported | Предугадывание ошибок |
| check - non-numeric value is reported | Предугадывание ошибок |
| check - range without any bound is a configuration error | Предугадывание ошибок |
| validateParams - min and max in order give no errors | Классы эквивалентности |
| validateParams - min equal to max is allowed | Граничные значения |
| validateParams - neither min nor max is rejected | Классы эквивалентности |

### `RuleParamsSchemaTest` — business-logic, стиль: классический

Файл: [RuleParamsSchemaTest.kt](../../business-logic/src/test/kotlin/com/vargod/engine/validation/RuleParamsSchemaTest.kt)

| Тест | Техника |
|---|---|
| validate - value of the declared type is accepted ★ | Классы эквивалентности |
| validate - value of another type is rejected with parameter name ★ | Классы эквивалентности |
| validate - INTEGER accepts a double only when it is whole ★ | Граничные значения |
| validate - missing required parameter is reported | Классы эквивалентности |
| validate - string outside allowed values is rejected | Классы эквивалентности |

### `RuleValidationEngineClassicalTest` — business-logic, стиль: классический

Файл: [RuleValidationEngineTest.kt](../../business-logic/src/test/kotlin/com/vargod/engine/validation/RuleValidationEngineTest.kt)

| Тест | Техника |
|---|---|
| validate - all satisfied validators give one PASS per validator | Классы эквивалентности |
| validate - violated RANGE gives FAIL with message from the rule | Классы эквивалентности |
| validate - no validators gives no results | Граничные значения |
| validate - unknown rule type gives FAIL instead of exception | Предугадывание ошибок |

### `RuleValidationEngineLondonTest` — business-logic, стиль: лондонский

Файл: [RuleValidationEngineTest.kt](../../business-logic/src/test/kotlin/com/vargod/engine/validation/RuleValidationEngineTest.kt)

| Тест | Техника |
|---|---|
| validate - RANGE validator gets targetField injected as field parameter | Классы эквивалентности |
| validate - CONSTRAINT rules are passed to the rule unchanged | Классы эквивалентности |
| validate - IllegalArgumentException from rule becomes FAIL with its message | Предугадывание ошибок |

### `SumEqualsFieldRuleTest` — business-logic, стиль: классический

Файл: [SumEqualsFieldRuleTest.kt](../../business-logic/src/test/kotlin/com/vargod/engine/validation/SumEqualsFieldRuleTest.kt)

| Тест | Техника |
|---|---|
| type - is SUM_EQUALS_FIELD | Классы эквивалентности |
| type - differs from the similar SUM_EQUALS_VALUE rule | Предугадывание ошибок |
| paramsSchema - tolerance is the only optional parameter | Классы эквивалентности |
| paramsSchema - expectedField must be a string | Предугадывание ошибок |
| check - sum equal to referenced field passes | Классы эквивалентности |
| check - difference exactly equal to tolerance passes | Граничные значения |
| check - difference just above tolerance fails | Граничные значения |
| check - missing expectedField parameter is reported | Предугадывание ошибок |
| validateParams - complete configuration has no errors | Классы эквивалентности |
| validateParams - fields given as plain string are rejected | Классы эквивалентности |

### `SumEqualsValueRuleTest` — business-logic, стиль: классический

Файл: [SumEqualsValueRuleTest.kt](../../business-logic/src/test/kotlin/com/vargod/engine/validation/SumEqualsValueRuleTest.kt)

| Тест | Техника |
|---|---|
| type - is SUM_EQUALS_VALUE | Классы эквивалентности |
| type - differs from the similar SUM_EQUALS_FIELD rule | Предугадывание ошибок |
| paramsSchema - fields and expected are required | Классы эквивалентности |
| paramsSchema - expected given as text is rejected | Предугадывание ошибок |
| check - fractions summing to one pass | Классы эквивалентности |
| check - difference twice the default tolerance fails | Граничные значения |
| check - missing fields parameter is reported | Предугадывание ошибок |
| validateParams - complete configuration has no errors | Классы эквивалентности |
| validateParams - missing expected is reported | Классы эквивалентности |

### `AdminUserServiceImplLondonTest` — business-logic, стиль: лондонский

Файл: [AdminUserServiceImplTest.kt](../../business-logic/src/test/kotlin/com/vargod/service/impl/AdminUserServiceImplTest.kt)

| Тест | Техника |
|---|---|
| list - delegates to UserService listAll | Классы эквивалентности |
| list - no users gives empty list | Граничные значения |
| create - delegates to UserService create with the same arguments | Классы эквивалентности |
| create - EmailAlreadyTakenException is propagated | Классы эквивалентности |
| delete - admin deletes another user via UserService | Таблица решений |
| delete - admin cannot delete own account and UserService is not called | Таблица решений |
| changeRole - delegates to UserService | Классы эквивалентности |
| changeRole - NotFoundException is propagated | Классы эквивалентности |

### `AuditLogServiceImplLondonTest` — business-logic, стиль: лондонский

Файл: [AuditLogServiceImplTest.kt](../../business-logic/src/test/kotlin/com/vargod/service/impl/AuditLogServiceImplTest.kt)

| Тест | Техника |
|---|---|
| log - entry carries snapshot of actor id and email and serialized payload | Классы эквивалентности |
| log - payload serialization failure does not break the action, payload becomes null | Предугадывание ошибок |
| log - repository failure is propagated to the caller | Предугадывание ошибок |
| find - passes the filter to repository unchanged | Классы эквивалентности |
| find - repository rejection of invalid limit is propagated | Граничные значения |

### `AuthServiceImplClassicalTest` — business-logic, стиль: классический

Файл: [AuthServiceImplClassicalTest.kt](../../business-logic/src/test/kotlin/com/vargod/service/impl/AuthServiceImplClassicalTest.kt)

| Тест | Техника |
|---|---|
| register - default role is RESEARCHER and user can log in afterwards | Классы эквивалентности |
| register - second registration with the same email is refused | Классы эквивалентности |
| authenticate - email lookup is case-insensitive | Классы эквивалентности |
| authenticate - wrong password is refused | Классы эквивалентности |
| authenticate - legacy user with invalid hash cannot log in | Предугадывание ошибок |

### `ComparisonServiceImplClassicalTest` — business-logic, стиль: классический

Файл: [ComparisonServiceImplClassicalTest.kt](../../business-logic/src/test/kotlin/com/vargod/service/impl/ComparisonServiceImplClassicalTest.kt)

| Тест | Техника |
|---|---|
| create - exactly two experiments is the minimal valid comparison | Граничные значения |
| create - empty id list is rejected and nothing is stored | Граничные значения |
| create - access to the second experiment follows ownership and publicity rule | Таблица решений |

### `ComparisonServiceImplLondonTest` — business-logic, стиль: лондонский

Файл: [ComparisonServiceImplLondonTest.kt](../../business-logic/src/test/kotlin/com/vargod/service/impl/ComparisonServiceImplLondonTest.kt)

| Тест | Техника |
|---|---|
| create - own experiments of one model are loaded once and saved as comparison | Классы эквивалентности |
| create - private experiment of another user gives AuthorizationException | Таблица решений |
| create - single experiment is below the minimum of two | Граничные значения |
| create - duplicated experiment id is rejected before loading | Предугадывание ошибок |
| create - missing experiment gives NotFoundException with its id | Классы эквивалентности |
| create - experiments of different models cannot be compared | Классы эквивалентности |
| getById - returns comparison found by repository | Классы эквивалентности |
| getById - missing comparison gives NotFoundException | Классы эквивалентности |
| listByUser - delegates to repository | Классы эквивалентности |
| listByUser - user without comparisons gets empty list | Граничные значения |

### `ExperimentServiceImplClassicalTest` — business-logic, стиль: классический

Файл: [ExperimentServiceImplClassicalTest.kt](../../business-logic/src/test/kotlin/com/vargod/service/impl/ExperimentServiceImplClassicalTest.kt)

| Тест | Техника |
|---|---|
| create - valid parameters produce stored PENDING experiment with validator snapshot | Классы эквивалентности |
| create - population exactly at exclusive lower bound 0 is rejected | Граничные значения |
| create - smallest positive population just above the bound is accepted | Граничные значения |
| runSimulation - PENDING logistic experiment becomes COMPLETED with numeric result | Переходы состояний |
| runSimulation - COMPLETED experiment cannot be run again and keeps its result | Переходы состояний |
| runSimulation - model without ODE implementation moves experiment to FAILED | Переходы состояний |
| publish - private experiment of owner becomes public in storage | Переходы состояний |
| publish - stranger cannot publish and experiment stays private | Классы эквивалентности |

### `ExperimentServiceImplLondonTest` — business-logic, стиль: лондонский

Файл: [ExperimentServiceImplLondonTest.kt](../../business-logic/src/test/kotlin/com/vargod/service/impl/ExperimentServiceImplLondonTest.kt)

| Тест | Техника |
|---|---|
| create - valid request is validated against model validators and saved as PENDING | Классы эквивалентности |
| create - failed validation throws ValidationException and nothing is saved | Классы эквивалентности |
| create - unknown model throws NotFoundException before validation | Классы эквивалентности |
| create - zero step is rejected without touching repositories | Граничные значения |
| getById - returns experiment found by repository | Классы эквивалентности |
| getById - missing experiment throws NotFoundException | Классы эквивалентности |
| listByUser - delegates to repository with the same user id | Классы эквивалентности |
| listByUser - repository failure is propagated unchanged | Предугадывание ошибок |
| listPublic - delegates to repository | Классы эквивалентности |
| listPublic - no public experiments gives empty list | Граничные значения |
| runSimulation - PENDING goes to RUNNING then COMPLETED with result | Переходы состояний |
| runSimulation - engine failure moves experiment to FAILED and rethrows | Переходы состояний |
| runSimulation - only PENDING may be started, other states are rejected ★ | Переходы состояний |
| runSimulation - experiment without parameter set is rejected | Предугадывание ошибок |
| runSimulation - deleted model template gives NotFoundException and status stays PENDING | Предугадывание ошибок |
| publish - owner makes private experiment public | Переходы состояний |
| publish - already public experiment is returned as is without saving | Переходы состояний |
| publish - non-owner gets AuthorizationException | Классы эквивалентности |
| unpublish - owner makes public experiment private | Переходы состояний |
| unpublish - non-owner gets AuthorizationException | Классы эквивалентности |

### `ModelServiceImplLondonTest` — business-logic, стиль: лондонский

Файл: [ModelServiceImplTest.kt](../../business-logic/src/test/kotlin/com/vargod/service/impl/ModelServiceImplTest.kt)

| Тест | Техника |
|---|---|
| listAll - returns catalog from repository | Классы эквивалентности |
| listAll - repository failure is propagated | Предугадывание ошибок |
| getById - returns model found by repository | Классы эквивалентности |
| getById - missing model gives NotFoundException | Классы эквивалентности |

### `ModelServiceImplClassicalTest` — business-logic, стиль: классический

Файл: [ModelServiceImplTest.kt](../../business-logic/src/test/kotlin/com/vargod/service/impl/ModelServiceImplTest.kt)

| Тест | Техника |
|---|---|
| listAll - returns catalog ordered by model code | Классы эквивалентности |
| listAll - empty catalog gives empty list | Граничные значения |
| getById - returns model with its validators | Классы эквивалентности |
| getById - unknown id gives NotFoundException | Классы эквивалентности |

### `UserServiceImplLondonTest` — business-logic, стиль: лондонский

Файл: [UserServiceImplLondonTest.kt](../../business-logic/src/test/kotlin/com/vargod/service/impl/UserServiceImplLondonTest.kt)

| Тест | Техника |
|---|---|
| getById - returns user from repository | Классы эквивалентности |
| getById - unknown id gives NotFoundException | Классы эквивалентности |
| getByEmail - returns user from repository | Классы эквивалентности |
| getByEmail - unknown email gives null instead of exception | Классы эквивалентности |
| listAll - delegates to repository | Классы эквивалентности |
| listAll - repository failure is propagated | Предугадывание ошибок |
| create - checks email uniqueness then saves user with bcrypt hash | Классы эквивалентности |
| create - taken email gives EmailAlreadyTakenException and nothing is saved | Классы эквивалентности |
| create - password of 7 characters is one below the minimum | Граничные значения |
| changeRole - delegates to repository and returns updated user | Классы эквивалентности |
| changeRole - unknown user error from repository is propagated | Предугадывание ошибок |
| delete - existing user is looked up then deleted | Классы эквивалентности |
| delete - unknown user gives NotFoundException and deleteById is not called | Классы эквивалентности |

### `ValidatorAdminServiceImplLondonTest` — business-logic, стиль: лондонский

Файл: [ValidatorAdminServiceImplTest.kt](../../business-logic/src/test/kotlin/com/vargod/service/impl/ValidatorAdminServiceImplTest.kt)

| Тест | Техника |
|---|---|
| list - passes model filter to repository | Классы эквивалентности |
| list - without filter asks repository for all validators and may get none | Граничные значения |
| get - returns validator found by id | Классы эквивалентности |
| get - unknown id gives NotFoundException | Классы эквивалентности |
| create - RANGE rules are checked by RANGE rule and then saved | Классы эквивалентности |
| create - every rule error becomes a FAIL result and nothing is saved | Классы эквивалентности |
| create - CONSTRAINT without type is rejected without asking registry | Предугадывание ошибок |
| update - existing validator with valid rules is updated | Классы эквивалентности |
| update - unknown validator gives NotFoundException before rules are checked | Классы эквивалентности |
| delete - existing validator is deleted | Классы эквивалентности |
| delete - unknown validator gives NotFoundException and nothing is deleted | Классы эквивалентности |
| listAvailableTypes - types are sorted and carry rule schemas | Классы эквивалентности |
| listAvailableTypes - empty registry gives empty list | Граничные значения |

### `ValidatorAdminServiceImplClassicalTest` — business-logic, стиль: классический

Файл: [ValidatorAdminServiceImplTest.kt](../../business-logic/src/test/kotlin/com/vargod/service/impl/ValidatorAdminServiceImplTest.kt)

| Тест | Техника |
|---|---|
| create - RANGE with min equal to max is a valid degenerate range | Граничные значения |
| create - decision table of validator configuration outcomes ★ | Таблица решений |

### `JdbcAuditLogRepositoryTest` — data-access, стиль: классический (PostgreSQL)

Файл: [JdbcAuditLogRepositoryTest.kt](../../data-access/src/test/kotlin/com/vargod/repository/jdbc/JdbcAuditLogRepositoryTest.kt)

| Тест | Техника |
|---|---|
| save - entry with actor, target and payload is stored as is | Классы эквивалентности |
| save - reference to a non-existent user violates foreign key | Предугадывание ошибок |
| find - pairwise combinations of optional filter criteria | Попарное (комбинаторное) |
| find - limit is accepted only within 1 to 1000 ★ | Граничные значения |

### `JdbcComparisonRepositoryTest` — data-access, стиль: классический (PostgreSQL)

Файл: [JdbcComparisonRepositoryTest.kt](../../data-access/src/test/kotlin/com/vargod/repository/jdbc/JdbcComparisonRepositoryTest.kt)

| Тест | Техника |
|---|---|
| save - findById returns experiments in the order they were given | Классы эквивалентности |
| save - comparison without experiments is rejected | Граничные значения |
| findById - unknown id gives null | Классы эквивалентности |
| findById - re-saving with fewer experiments replaces the list | Классы эквивалентности |
| findAllByUserId - returns only comparisons of that user | Классы эквивалентности |
| findAllByUserId - user without comparisons gets empty list | Граничные значения |

### `JdbcExperimentRepositoryTest` — data-access, стиль: классический (PostgreSQL)

Файл: [JdbcExperimentRepositoryTest.kt](../../data-access/src/test/kotlin/com/vargod/repository/jdbc/JdbcExperimentRepositoryTest.kt)

| Тест | Техника |
|---|---|
| save - new experiment with parameters and validator snapshot is fully restored by findById | Классы эквивалентности |
| save - second save updates status, stores result and does not duplicate children | Переходы состояний |
| save - name of 256 characters is rejected before touching the database | Граничные значения |
| save - reference to unknown model is rolled back as a whole | Предугадывание ошибок |
| findById - unknown id gives null | Классы эквивалентности |
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

### `JdbcModelTemplateRepositoryTest` — data-access, стиль: классический (PostgreSQL)

Файл: [JdbcModelTemplateRepositoryTest.kt](../../data-access/src/test/kotlin/com/vargod/repository/jdbc/JdbcModelTemplateRepositoryTest.kt)

| Тест | Техника |
|---|---|
| findAll - returns templates ordered by code, each with its own validators | Классы эквивалентности |
| findAll - empty catalog gives empty list | Классы эквивалентности |
| findById - returns template with solver hints | Классы эквивалентности |
| findById - unknown id gives null | Классы эквивалентности |
| save - existing template metadata is updated, validators are kept | Классы эквивалентности |
| save - second template with an existing code violates unique constraint | Предугадывание ошибок |
| deleteById - removes template and cascades its validators | Классы эквивалентности |
| deleteById - template used by an experiment cannot be deleted | Предугадывание ошибок |

### `JdbcUserRepositoryLondonTest` — data-access, стиль: лондонский

Файл: [JdbcRepositoriesLondonTest.kt](../../data-access/src/test/kotlin/com/vargod/repository/jdbc/JdbcRepositoriesLondonTest.kt)

| Тест | Техника |
|---|---|
| findById - binds id as the only parameter and maps the row | Классы эквивалентности |
| findById - SQLException is wrapped into PersistenceException with cause | Предугадывание ошибок |
| updateRole - binds role name before id | Классы эквивалентности |
| updateRole - zero updated rows means unknown user | Предугадывание ошибок |

### `JdbcExperimentRepositoryLondonTest` — data-access, стиль: лондонский

Файл: [JdbcRepositoriesLondonTest.kt](../../data-access/src/test/kotlin/com/vargod/repository/jdbc/JdbcRepositoriesLondonTest.kt)

| Тест | Техника |
|---|---|
| save - failure in the middle of the transaction rolls back and restores autocommit | Предугадывание ошибок |
| findAllByIds - empty id list does not open a connection | Классы эквивалентности |
| deleteById - unavailable database gives PersistenceException | Предугадывание ошибок |

### `JdbcUserRepositoryTest` — data-access, стиль: классический (PostgreSQL)

Файл: [JdbcUserRepositoryTest.kt](../../data-access/src/test/kotlin/com/vargod/repository/jdbc/JdbcUserRepositoryTest.kt)

| Тест | Техника |
|---|---|
| findById - returns stored user | Классы эквивалентности |
| findById - unknown id gives null | Классы эквивалентности |
| findByEmail - search ignores letter case | Классы эквивалентности |
| findByEmail - unknown email gives null | Классы эквивалентности |
| findAll - users are ordered by registration time | Классы эквивалентности |
| findAll - empty table gives empty list | Классы эквивалентности |
| existsAdmin - true when at least one admin is stored | Классы эквивалентности |
| existsAdmin - false when only researchers are stored | Классы эквивалентности |
| save - second save with the same id updates email and role | Классы эквивалентности |
| save - email differing only in case violates unique index | Предугадывание ошибок |
| updateRole - returns user with the new role | Классы эквивалентности |
| updateRole - unknown user gives PersistenceException | Предугадывание ошибок |
| deleteById - removes the user | Классы эквивалентности |
| deleteById - unknown id does not touch other users | Предугадывание ошибок |

### `JdbcValidatorRepositoryTest` — data-access, стиль: классический (PostgreSQL)

Файл: [JdbcValidatorRepositoryTest.kt](../../data-access/src/test/kotlin/com/vargod/repository/jdbc/JdbcValidatorRepositoryTest.kt)

| Тест | Техника |
|---|---|
| findAll - with model id returns only that model's validators | Классы эквивалентности |
| findAll - without model id returns validators of all models | Классы эквивалентности |
| findById - JSON rules are restored with their types | Классы эквивалентности |
| findById - unknown id gives null | Классы эквивалентности |
| save - stored validator is bound to the model | Классы эквивалентности |
| save - unknown model violates foreign key | Предугадывание ошибок |
| update - name and rules are replaced | Классы эквивалентности |
| update - unknown validator gives PersistenceException | Предугадывание ошибок |
| deleteById - removes the validator | Классы эквивалентности |
| deleteById - unknown id leaves other validators intact | Предугадывание ошибок |

### `JsonCodecTest` — data-access, стиль: классический

Файл: [JsonCodecTest.kt](../../data-access/src/test/kotlin/com/vargod/repository/jdbc/JsonCodecTest.kt)

| Тест | Техника |
|---|---|
| encode - map is written as compact JSON | Классы эквивалентности |
| encode - object without serializable properties gives PersistenceException | Предугадывание ошибок |
| decodeDoubleMap - integer values are converted to doubles | Классы эквивалентности |
| decodeDoubleMap - truncated JSON gives PersistenceException | Предугадывание ошибок |
| decodeValidators - snapshot written by encodeValidators is restored | Классы эквивалентности |
| decodeValidators - unknown enum value gives PersistenceException | Предугадывание ошибок |
| decodeSimulationData - data written by encodeSimulationData is restored | Классы эквивалентности |
| decodeSimulationData - JSON without timePoints gives PersistenceException | Предугадывание ошибок |

### `SchemaInitializerTest` — data-access, стиль: классический (PostgreSQL)

Файл: [SchemaInitializerTest.kt](../../data-access/src/test/kotlin/com/vargod/repository/jdbc/SchemaInitializerTest.kt)

| Тест | Техника |
|---|---|
| initialize - repeated run on migrated database is a no-op | Классы эквивалентности |

### `SchemaInitializerLondonTest` — data-access, стиль: лондонский

Файл: [SchemaInitializerTest.kt](../../data-access/src/test/kotlin/com/vargod/repository/jdbc/SchemaInitializerTest.kt)

| Тест | Техника |
|---|---|
| initialize - unreachable database gives PersistenceException | Предугадывание ошибок |
