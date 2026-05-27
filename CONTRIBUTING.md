# Contributing to Vargod

Спасибо за интерес. Vargod — академический проект, но мы рады PR любого размера:
от опечаток в документации до новых ODE-моделей и улучшений UI.

## TL;DR

1. Форкните, заведите ветку `feat/<short-name>` или `fix/<short-name>` от `main`.
2. Запустите `./gradlew check` локально.
3. Откройте PR с заполненным шаблоном (`.github/PULL_REQUEST_TEMPLATE.md`).
4. CI должен быть зелёным. Ревьювер сольёт после approve.

## Dev environment

### Минимум

- **JDK 17** (рекомендуется Temurin / OpenJDK).
- **Docker** + **Docker Compose** (для PostgreSQL и end-to-end).
- **Node.js 20+** и `npm` — для фронтенда.
- `git`.

### Быстрый старт

```bash
git clone <your-fork>
cd vargod
cp .env.example .env
docker compose up -d db        # только БД, чтобы прогнать тесты
./gradlew :domain:test :business-logic:test :data-access:test :api:test
```

Полный стек:

```bash
docker compose up -d --build   # db + api + cli + frontend
open http://localhost:8081     # UI
open http://localhost:8080/swagger-ui.html
```

### Frontend dev-режим (hot reload)

```bash
cd frontend
npm install
npm run dev                    # Vite на http://localhost:5173, proxy на API:8080
```

## Структура репозитория

| Каталог | Что внутри |
| --- | --- |
| `domain/` | Доменные типы, исключения, интерфейсы. Без зависимостей. |
| `business-logic/` | Сервисы, ODE-движок, валидация. |
| `data-access/` | JDBC-репозитории, миграции Flyway, `DataSeeder`. |
| `api/` | Spring Boot REST + Security + springdoc. |
| `app-cli/` | CLI-приложение. |
| `frontend/` | React 18 + Vite + Tailwind + shadcn/ui. |
| `diagrams/` | PlantUML, BPMN, DBML — исходники диаграмм. |
| `img/` | Сгенерированные SVG диаграмм. |
| `docs/` | Продуктовая документация. |

## Тесты

- **JUnit 5** (Jupiter) — фреймворк.
- **Mockk** — мокинг.
- **Testcontainers** — интеграционные тесты репозиториев против настоящего Postgres в Docker.
- Запуск всего: `./gradlew check` (или per-module: `./gradlew :api:test`).
- Покрытие — не enforce'им, но новый код должен иметь хотя бы happy-path и один error-path тест.

## Миграции БД (Flyway)

Все изменения схемы — через миграции в `data-access/src/main/resources/db/migration/`. Соглашение по именованию:

```
V<n>__<short_snake_description>.sql
```

Текущий максимум — `V7`. Следующая миграция должна быть `V8__<...>.sql`. Никогда не редактируйте существующие миграции после мерджа в `main` — Flyway сравнивает чек-суммы и упадёт на следующем старте.

Если миграция требует backfill — оформляйте отдельным `V<n+1>__backfill_<...>.sql` или комментируйте инструкцию в `docs/` для миграции prod-данных.

## Как добавить ODE-модель

См. подробное HOWTO: [docs/how-to-add-model.md](docs/how-to-add-model.md).

Кратко:

1. Реализовать `OdeModel` в `business-logic/.../engine/ode/`.
2. Зарегистрировать в `MapModelRegistry`.
3. Добавить seed-функцию в `DataSeeder`.
4. Unit-тесты (derivatives + missing-param).
5. Запись в [docs/models.md](docs/models.md) и `CHANGELOG.md`.

## Commit-сообщения

Без жёсткой schema, но желательно:

- Императив, кратко: `Add SEIRD model`, `Fix CORS for prod origins`.
- Crash-причина в теле, если фикс не очевиден.
- Связь с issue: `Closes #42`.

## Pull request

- Один PR — одно логическое изменение. Большие фичи можно разбивать по этапам.
- Шаблон: `.github/PULL_REQUEST_TEMPLATE.md`.
- CI (`.github/workflows/ci.yml`) запускает `./gradlew build` — должен быть зелёным.
- Если меняли C4-диаграммы — экспортируйте SVG: `docs/architecture.md` ссылается на файлы в `img/`.
- Если меняли API — обновите `docs/api.md` и проверьте, что Swagger UI генерирует ожидаемое (поднимите API локально и зайдите на `/swagger-ui.html`).

## Code style

- **Kotlin:** официальный стиль (idiomatic Kotlin). Длина строки ~120. Прагматичные сокращения допустимы.
- **TypeScript / React:** shadcn-стиль (компоненты в `frontend/src/components/`), Tailwind utility-first, никаких inline-style без причины.
- **Комментарии:** RU/EN — на ваше усмотрение, но KDoc на публичные API желательно писать на том же языке, что и существующие.

## Issue tracker

GitHub Issues. Шаблоны — `.github/ISSUE_TEMPLATE/`. Не стесняйтесь репортить «странное поведение» с шагами воспроизведения и логом `logs/vargod-api.log`.

## Code of conduct

[CODE_OF_CONDUCT.md](CODE_OF_CONDUCT.md) — Contributor Covenant 2.1. Будьте взаимоуважительны.

## Лицензия contribution'ов

Отправляя PR, вы соглашаетесь с тем, что ваш вклад распространяется под [MIT License](LICENSE).
