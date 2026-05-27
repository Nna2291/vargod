# Security model

Документ описывает модель угроз, текущие защитные меры и явно out-of-scope темы.
Vargod — академический проект, **не production-ready**; список ниже — честный
аудит того, что реализовано и что нет.

## Аутентификация

- **JWT HS256** через Nimbus + Spring Security OAuth2 Resource Server (`SecurityConfig`).
- Секрет — `VARGOD_JWT_SECRET` (минимум 32 байта). Дефолт в `docker-compose.yml` помечен как `dev-only`; для production обязательно задавайте свой:
  ```bash
  openssl rand -base64 48
  ```
- TTL — `VARGOD_JWT_TTL_SECONDS` (по умолчанию 3600 c = 1 час).
- Эндпоинты выдачи токена: `POST /api/auth/register`, `POST /api/auth/login`.
- Client kладёт токен в `Authorization: Bearer <token>`. Sessions / cookies не используются.
- Stateless: `SessionCreationPolicy.STATELESS`, CSRF выключен (нет сессионных cookie).

### Кодирование паролей

- BCrypt с фактором стоимости **10** (`BCryptPasswordEncoder` по умолчанию).
- Хеши хранятся в `users.password_hash` (миграция `V2__users.sql`).

### AdminBootstrap

- При первом старте API (`AdminBootstrap`) создаёт пользователя с ролью `ADMIN`, если в таблице нет ни одного администратора.
- Email/пароль берутся из `VARGOD_ADMIN_EMAIL` / `VARGOD_ADMIN_PASSWORD`. Если пусто — bootstrap не выполняется (зайти в систему в первый раз будет нечем; вручную добавить ADMIN в БД либо задать env и перезапустить).
- На каждом последующем старте перепроверяется только наличие ≥1 ADMIN — существующий пользователь не пересоздаётся и пароль не сбрасывается.

## Авторизация

- Роли в БД: `ADMIN`, `RESEARCHER` (`Role` enum). Гость — любой запрос без валидного JWT.
- На уровне URL (`SecurityConfig.securityFilterChain`):
  - `/api/auth/**` — открыто.
  - `GET /api/models`, `GET /api/models/**` — открыто (каталог).
  - `GET /api/experiments`, `GET /api/experiments/*` — открыто (видны только публичные).
  - `/api/admin/**` — только `ROLE_ADMIN`.
  - Swagger UI (`/swagger-ui.html`, `/swagger-ui/**`, `/v3/api-docs/**`) — открыто.
  - Всё остальное — `authenticated()`.
- На уровне сервиса (`ExperimentService`, `ComparisonService`) — проверка владения ресурсом: только владелец может `run`/`publish`/`delete` свой эксперимент. ADMIN получает force-delete через отдельный admin-эндпоинт.

## Snapshot валидаторов (P2)

При создании эксперимента (`POST /api/experiments`) сервис копирует текущий набор валидаторов модели в `experiment_validator_snapshot` (миграция `V4__experiment_validator_snapshot.sql`). Запуск симуляции и повторная валидация используют именно snapshot, а не «живые» валидаторы. Это обеспечивает воспроизводимость: эксперимент не «протухает», если админ позже отредактировал правило.

## CORS

- Источники из `VARGOD_CORS_ALLOWED_ORIGINS` (через запятую).
- Дефолт: `http://localhost:5173, http://localhost:8081`.
- `*` запрещён в production — нужно явно перечислять.
- `allowCredentials = false` (мы не используем cookies).
- Применяется только к `/api/**`.

## Аудит admin-действий

Каждая мутация в admin-API пишет запись в таблицу `audit_log` (миграция `V6__audit_log.sql`):
актор (`user_id`, `user_email`-снимок), `action` (например `USER_DELETE`, `VALIDATOR_UPDATE`),
тип и ID цели, JSON-`payload` с релевантным контекстом. Чтение — `GET /api/admin/audit`
с фильтрами по action / userId / временному диапазону.

## V3 legacy backfill

Миграция `V3__experiments_user_fk.sql` ввела связь `experiments.user_id → users(id)`. На уже заселённых dev-БД старые эксперименты получают `user_id = NULL` (legacy backfill). `SchemaInitializer` задаёт `flyway.baselineOnMigrate=true` для накатывания миграций на БД без истории Flyway — чтобы не сломать локальные dev-окружения, существовавшие до P0.

## Out of scope

Сознательные ограничения текущей версии:

- **XSS / token storage.** JWT хранится во фронтенде в `localStorage`. Если фронтенд получит XSS-инъекцию, токен будет украден. Mitigation на будущее: HttpOnly cookie + CSRF token либо BFF (backend-for-frontend) с короткоживущим access-токеном.
- **Refresh tokens.** Не реализованы. Сессия = 1 час, после чего пользователь перезаходит.
- **MFA / 2FA.** Не реализовано.
- **Rate limiting.** Нет защиты от брутфорса логина или DDoS. На production нужен reverse-proxy с rate-limit (nginx limit_req / Cloudflare / Caddy).
- **Audit для не-admin действий.** Запуск/публикация экспериментов простыми пользователями не пишутся в `audit_log` — только админские мутации.
- **Логирование PII.** Email пишется в логи (`logs/vargod-api.log`) на каждое действие. Для production — отредактировать `AuthController.log.info(...)` или сменить формат логирования.
- **Шифрование at-rest.** Отвечает СУБД и инфраструктура (volume), приложение само ничего не шифрует.
- **Подпись результатов симуляции.** Нет защиты от подмены `simulation_result` в БД оператором с прямым доступом.
- **CORS preflight для не-API путей.** Swagger UI и статика не покрыты CORS (по умолчанию same-origin).

## Что проверять при code-review безопасности

1. Любой новый эндпоинт — явно добавлен в `SecurityConfig.requestMatchers(...)` или сознательно попадает в `anyRequest().authenticated()`?
2. Любая admin-мутация — пишет в `auditLogService.log(...)` с актором из `CurrentUser`?
3. Любой сервис, читающий/меняющий чужой ресурс — проверяет `currentUser.id == resource.userId` или роль `ADMIN`?
4. Никаких raw-SQL с конкатенацией пользовательского ввода — только PreparedStatement (см. `JdbcTemplate`-обёртки в `data-access`).
5. Новые env-переменные документируются в `.env.example` и **никогда** не коммитятся в `.env`.
