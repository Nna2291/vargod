# Changelog

Все заметные изменения в проекте Vargod фиксируются в этом файле.

Формат соответствует [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
семантика версий — [SemVer](https://semver.org/).

## [Unreleased]

Большой релиз подготовки к публикации на GitHub (фазы P0–P7). Краткое описание
изменений по фазам:

### Added

- **P0 — фундамент**
  - `LICENSE` (MIT).
  - `.env.example`, `app-cli/vargod.properties.example`.
  - Миграции схемы через **Flyway** (`data-access/src/main/resources/db/migration/V*__*.sql`)
    вместо одного `schema.sql`. `V1__init.sql` повторяет историческую схему.
  - GitHub Actions: `ci.yml` (backend `./gradlew test` + frontend `npm run build`
    + docker-build), `dependabot.yml`, шаблоны Issue / PR.
  - Обновлён Gradle wrapper до 8.10.2 (требуется Spring Boot 3.3.5).

- **P1 — настоящая аутентификация**
  - Сущность `User`, `enum Role { ADMIN, RESEARCHER }`, репозиторий + JDBC-реализация.
  - Миграции `V2__users.sql` (таблица + уникальный индекс по `LOWER(email)`),
    `V3__experiments_user_fk.sql` (FK + бэкфилл legacy-пользователей с инвалидным
    хешем пароля).
  - JWT HS256 через **Spring Security 6** + `oauth2-resource-server` (Nimbus),
    BCrypt из `at.favre.lib:bcrypt:0.10.2` (без Spring-зависимостей в business-logic).
  - Контроллер `/api/auth/{register,login,me}`, `AdminBootstrap` для авто-создания
    первого администратора из `VARGOD_ADMIN_EMAIL` / `VARGOD_ADMIN_PASSWORD`.
  - В контроллерах: типизированный `CurrentUser` принципал через
    `CurrentUserArgumentResolver`.
  - В CLI: вход через `login <email> [password]` + `register` команды, пароль
    запрашивается через `Console.readPassword` (без эха).

- **P2 — нормализация framework'а валидаторов**
  - `ConstraintRule.paramsSchema()` + `validateParams(params)`.
  - `RangeRule` вынесен в реестр (был зашит в `RuleValidationEngine`).
  - Все 4 правила (RANGE, SUM_EQUALS_VALUE, SUM_EQUALS_FIELD, ORDERED) объявляют
    `RuleParamsSchema` — admin UI рендерит формы автоматически.
  - Миграция `V4__experiment_validator_snapshot.sql`: snapshot валидаторов на
    момент создания эксперимента (`experiments.validator_snapshot TEXT`).

- **P3a — Admin API**
  - Миграции `V5__validators_admin.sql` (`created_at/updated_at/created_by`),
    `V6__audit_log.sql` (журнал админских действий).
  - Доменные репозитории `ValidatorRepository`, `AuditLogRepository` + JDBC.
  - Сервисы: `AdminUserService`, `ValidatorAdminService`, `AuditLogService`.
  - Контроллеры `/api/admin/{users,validators,models,experiments,audit}` + endpoint
    `GET /api/admin/validator-types` для динамических форм во фронте.
  - Шаблон действий аудита `AuditActions` (whitelist 13 значений).

- **P4 — четыре новые ODE-модели**
  - `SeirdModel` (SEIRD: восприимчивые/латентные/инфицированные/выздоровевшие/умершие).
  - `FitzHughNagumoModel` (упрощённая нейронная динамика).
  - `HodgkinHuxleyModel` (полная модель потенциала действия — 4 уравнения,
    с обработкой устранимых сингулярностей в `alpha_m`/`alpha_n`).
  - `RosenzweigMacArthurModel` (хищник-жертва с насыщением).
  - На `ModelTemplate` появились `recommendedMaxStep/RelTol/AbsTol`. Миграция `V7`.
  - `OdeSimulationEngine` принимает per-call tolerances из модели (для HH).
  - `DataSeeder` перешёл с `seedIfEmpty()` на per-code идемпотентный
    `seedAllIfMissing()` — новые модели засеваются на существующей БД.

- **P5 — Admin-панель на фронтенде**
  - Tailwind v3 + shadcn/ui (компоненты в дереве проекта под
    `frontend/src/view/components/ui/`).
  - TanStack Query + TanStack Table + react-hook-form + zod.
  - Маршруты `/admin/{dashboard,users,models,validators,experiments,audit}`,
    защищённые `RequireRole role="ADMIN"`.
  - `ValidatorBuilderForm` — динамическая форма по `paramsSchema` с сервера.
  - `/login`, `/register` страницы. `AdminLayout` с боковой навигацией.

- **P6 — редизайн фронтенда**
  - Все существующие страницы и компоненты переведены на Tailwind + shadcn.
  - Тёмная тема по умолчанию (system), переключатель в навбаре, persist в localStorage.
  - Recharts перетемизированы через `hsl(var(--*))` CSS-переменные.

- **P7 — release polish**
  - Документация в `docs/` (architecture.md, api.md, security.md, models.md,
    how-to-add-model.md), c4-диаграммы встроены.
  - Архивный академический отчёт перенесён в `docs/lab-reports/lab2-lab7.md`.
  - Переписан корневой `README.md` в продуктовом стиле.
  - OpenAPI/Swagger через **springdoc 2.6.0** на `/swagger-ui.html`.
  - `CONTRIBUTING.md`, `CHANGELOG.md`, `CODE_OF_CONDUCT.md`.

### Changed (breaking)

- **Удалён заголовок `X-User-Id`.** Все API-вызовы используют
  `Authorization: Bearer <JWT>`. Старый endpoint `/api/session/{login,guest}`
  возвращает `410 Gone` с сообщением о переходе на `/api/auth/login`.
- **CLI `login` теперь принимает email + пароль**, а не произвольный UUID.
- **CORS** перешёл с `*` на env-driven список origin'ов
  (`VARGOD_CORS_ALLOWED_ORIGINS`).
- **Frontend session shape** изменён (ключ `vargod-session-v2`) —
  существующие сессии будут разлогинены при первом открытии.
- **`DataSeeder.seedIfEmpty()` сохранён как алиас** к `seedAllIfMissing()`,
  но поведение «всё или ничего» убрано — каждая модель сеется независимо.

### Removed

- Файл `data-access/src/main/resources/db/schema.sql` сохранён только как
  пометка-перенаправление на `db/migration/V1__init.sql`.
- `SessionController` обнулён (см. breaking-изменения выше).
- `frontend/src/model/api/session.ts` удалён (вызывал гон-эндпоинты).

### Migration notes

- **Существующие dev-БД**: Flyway включён с `baselineOnMigrate=true`. Если у вас
  уже есть схема старого вида — Flyway пометит её как baseline и применит
  только новые миграции. Альтернатива: сбросить локальную БД (для академического
  проекта проще).
- **CLI / API смена auth**: после миграции на JWT существующие записи в
  `experiments.user_id` ссылаются на «legacy-...@vargod.local» пользователей,
  созданных V3-миграцией. Войти под ними нельзя (пароль `!INVALID`), но
  публичные эксперименты сохраняются.

[Unreleased]: https://github.com/Nna2291/vargod/compare/main...HEAD
