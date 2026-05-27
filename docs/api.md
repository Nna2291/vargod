# API reference

REST API на Spring Boot 3.3. Полная интерактивная спецификация — Swagger UI на
[`/swagger-ui.html`](http://localhost:8080/swagger-ui.html). OpenAPI JSON —
[`/v3/api-docs`](http://localhost:8080/v3/api-docs).

## Аутентификация

JWT HS256. Получите токен через `POST /api/auth/login`, передавайте в заголовке:

```
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
```

Колонка **Auth** в таблицах ниже:

- `—` — гость (без токена).
- `JWT` — любой аутентифицированный пользователь.
- `ADMIN` — только пользователь с ролью `ADMIN`.

Общий формат ошибок (`ApiError`):

```json
{ "error": "NOT_FOUND", "message": "Experiment ... not found" }
```

Стандартные коды:

| HTTP | error | Когда |
| --- | --- | --- |
| 400 | `VALIDATION_ERROR` | Неверные параметры эксперимента, нарушение правила валидатора. |
| 401 | `UNAUTHORIZED` | Нет/невалидный токен на защищённом эндпоинте. |
| 403 | `FORBIDDEN` | Аутентифицирован, но не хватает роли (`ADMIN`) или владелец ресурса. |
| 404 | `NOT_FOUND` | Объект не найден по ID. |
| 410 | `ENDPOINT_REMOVED` | Старые `/api/session/*` (см. ниже). |
| 500 | `INTERNAL_ERROR` | Непредвиденная ошибка; пишется в `logs/vargod-api.log`. |

---

## `/api/auth` — аутентификация

| Метод | Путь | Auth | Описание |
| --- | --- | --- | --- |
| POST | `/api/auth/register` | — | Регистрация (роль `RESEARCHER`). Тело: `{email, password}`. Ответ 201: `{token, user}`. |
| POST | `/api/auth/login` | — | Логин. Тело: `{email, password}`. Ответ 200: `{token, user}`. |
| GET  | `/api/auth/me` | JWT | Текущий пользователь: `{id, email, role}`. |

`token` — JWT, TTL задаётся `VARGOD_JWT_TTL_SECONDS` (по умолчанию 3600 c).

### Legacy: `/api/session/*` (deprecated в P1)

`POST /api/session/login`, `GET /api/session/guest` отвечают `410 Gone` с подсказкой использовать `/api/auth/login`. Эндпоинты сохранены ради явного сообщения старым клиентам и могут быть удалены в любой момент.

---

## `/api/models` — каталог моделей (публичный)

| Метод | Путь | Auth | Описание |
| --- | --- | --- | --- |
| GET | `/api/models` | — | Список всех `ModelTemplate` с описаниями и валидаторами. |
| GET | `/api/models/{id}` | — | Один шаблон модели по UUID. |

Ответ — массив `ModelTemplate`:

```json
{
  "id": "uuid",
  "code": "SIR",
  "name": "SIR — модель эпидемии",
  "description": "...",
  "solverType": "DORMAND_PRINCE_54",
  "validators": [ { "id": "...", "name": "...", "checkType": "RANGE", ... } ],
  "recommendedMaxStep": 1.0,
  "recommendedRelTol": 1.0e-10,
  "recommendedAbsTol": 1.0e-10
}
```

---

## `/api/experiments` — вычислительные эксперименты

| Метод | Путь | Auth | Описание |
| --- | --- | --- | --- |
| GET  | `/api/experiments?scope=public` | — | Опубликованные эксперименты (видны гостю). |
| GET  | `/api/experiments?scope=mine` | JWT | Эксперименты текущего пользователя. |
| GET  | `/api/experiments/{id}` | — | Один эксперимент (если опубликован или принадлежит вызывающему). |
| POST | `/api/experiments` | JWT | Создать. Тело: `CreateExperimentRequest` (см. ниже). 200: `Experiment`. |
| POST | `/api/experiments/{id}/run` | JWT | Запустить симуляцию. Меняет статус `DRAFT → RUNNING → COMPLETED`. |
| POST | `/api/experiments/{id}/publish` | JWT | Сделать публичным (только владелец). |

`CreateExperimentRequest`:

```json
{
  "name": "Базовая SIR",
  "modelTemplateId": "uuid",
  "parameters": { "beta": 0.3, "gamma": 0.1, "N": 1000 },
  "initialState": { "S": 999, "I": 1, "R": 0 },
  "timeStart": 0.0,
  "timeEnd": 100.0,
  "timeStep": 0.1
}
```

Возможные ошибки:

- 400 `VALIDATION_ERROR` — нарушение правила валидатора (например `S+I+R≠N`).
- 403 `FORBIDDEN` — попытка publish/run чужого эксперимента.
- 404 `NOT_FOUND` — нет такого `modelTemplateId` или эксперимента.

---

## `/api/comparisons` — сравнения

| Метод | Путь | Auth | Описание |
| --- | --- | --- | --- |
| GET  | `/api/comparisons` | JWT | Список сравнений текущего пользователя. |
| GET  | `/api/comparisons/{id}` | JWT | Одно сравнение. |
| POST | `/api/comparisons` | JWT | Создать. Тело: `{experimentIds: [uuid, …]}`. |

Бизнес-правила: все эксперименты в сравнении должны принадлежать одной модели (одинаковый `modelTemplateId`) и быть в статусе `COMPLETED`. Иначе — 400 `VALIDATION_ERROR`.

---

## `/api/admin/users` — управление пользователями

| Метод | Путь | Auth | Описание |
| --- | --- | --- | --- |
| GET    | `/api/admin/users` | ADMIN | Список всех пользователей. |
| POST   | `/api/admin/users` | ADMIN | Создать. Тело: `{email, password, role}`. 201: `UserView`. |
| PUT    | `/api/admin/users/{id}/role` | ADMIN | Сменить роль. Тело: `{role: "ADMIN" \| "RESEARCHER"}`. |
| DELETE | `/api/admin/users/{id}` | ADMIN | Удалить (нельзя удалить себя). 204. |

Каждая мутация пишется в `audit_log` (см. ниже).

---

## `/api/admin/validators` — валидаторы

| Метод | Путь | Auth | Описание |
| --- | --- | --- | --- |
| GET    | `/api/admin/validators?modelTemplateId=…` | ADMIN | Список (опциональный фильтр по модели). |
| GET    | `/api/admin/validators/{id}` | ADMIN | Один валидатор. |
| POST   | `/api/admin/validators` | ADMIN | Создать и привязать к шаблону модели. |
| PUT    | `/api/admin/validators/{id}` | ADMIN | Обновить. |
| DELETE | `/api/admin/validators/{id}` | ADMIN | Удалить. 204. |
| GET    | `/api/admin/validator-types` | ADMIN | Доступные типы правил с `paramsSchema` — для динамической формы во фронте. |

`CreateValidatorRequest`:

```json
{
  "name": "S+I+R=N",
  "checkType": "PARAMETER_VALUE",
  "targetKind": "PARAMETER",
  "targetField": "*",
  "rules": { "type": "SUM_EQUALS_FIELD", "fields": ["S","I","R"], "expectedField": "N" },
  "modelTemplateId": "uuid"
}
```

---

## `/api/admin/models` — шаблоны моделей

| Метод | Путь | Auth | Описание |
| --- | --- | --- | --- |
| POST   | `/api/admin/models` | ADMIN | Создать `ModelTemplate`. Тело: `CreateModelTemplateRequest`. |
| PUT    | `/api/admin/models/{id}` | ADMIN | Обновить. Тело: `UpdateModelTemplateRequest`. |
| DELETE | `/api/admin/models/{id}` | ADMIN | Удалить. 204. |

Чтение каталога моделей — публичный `GET /api/models`, см. выше.

---

## `/api/admin/experiments` — инспекция экспериментов

| Метод | Путь | Auth | Описание |
| --- | --- | --- | --- |
| GET    | `/api/admin/experiments?limit=100&offset=0` | ADMIN | Все эксперименты с пагинацией. |
| DELETE | `/api/admin/experiments/{id}` | ADMIN | Force-delete любого эксперимента. 204. |

---

## `/api/admin/audit` — журнал admin-действий

| Метод | Путь | Auth | Описание |
| --- | --- | --- | --- |
| GET | `/api/admin/audit?action=&userId=&from=&to=&limit=100&offset=0` | ADMIN | Записи журнала с фильтрами. |

`from`/`to` — ISO-8601 instant (например `2025-05-24T10:00:00Z`). Записи в `audit_log` хранят `userId`, `userEmail` на момент действия, `action` (`USER_CREATE`, `VALIDATOR_UPDATE`, …), `targetType/targetId`, JSON-`payload`, `createdAt`.

---

## CORS

Контролируется `VARGOD_CORS_ALLOWED_ORIGINS` (через запятую). По умолчанию dev-список:
`http://localhost:5173` (Vite dev) и `http://localhost:8081` (nginx-фронт из compose). `*` — запрещён.
