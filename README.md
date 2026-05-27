# Vargod

[![CI](https://github.com/Nna2291/vargod/actions/workflows/ci.yml/badge.svg)](https://github.com/Nna2291/vargod/actions/workflows/ci.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)
[![Kotlin](https://img.shields.io/badge/Kotlin-1.9.25-7F52FF?logo=kotlin)](https://kotlinlang.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.5-6DB33F?logo=springboot)](https://spring.io/projects/spring-boot)
[![React](https://img.shields.io/badge/React-18-61DAFB?logo=react)](https://react.dev/)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-336791?logo=postgresql)](https://www.postgresql.org/)
[![Status](https://img.shields.io/badge/status-academic-blue)](docs/lab-reports/lab2-lab7.md)

> Веб-платформа для вычислительных экспериментов над биологическими ОДУ-моделями.
> Выберите модель (SIR, Лотка-Вольтерра, SEIRD, Hodgkin-Huxley и др.), задайте параметры,
> запустите симуляцию, получите интерактивный график. Сравните несколько прогонов на одних осях.

**English**: A web platform for running computational experiments on classical biological ODE models
(SIR epidemics, Lotka–Volterra, SEIRD, FitzHugh–Nagumo, Hodgkin–Huxley, Rosenzweig–MacArthur, logistic growth).
Pick a model, set parameters, run the simulation, view an interactive chart, and compare multiple runs side-by-side.
Built for researchers, students and educators who want quick numerical experiments without writing code.

---

## Screenshots

<!-- TODO: добавить реальные скриншоты после `docker compose up -d --build` и зайти на http://localhost:8081 -->

| Каталог моделей | Эксперимент с графиком |
| --- | --- |
| ![Catalog](docs/screenshots/catalog.png) | ![Experiment chart](docs/screenshots/experiment-chart.png) |

| Admin: пользователи | Admin: валидаторы | Логин |
| --- | --- | --- |
| ![Admin users](docs/screenshots/admin-users.png) | ![Admin validators](docs/screenshots/admin-validators.png) | ![Login](docs/screenshots/login.png) |

---

## Quick start

Нужны Docker и Docker Compose. Java/Node ставить локально не обязательно.

```bash
cp .env.example .env
docker compose up -d --build
open http://localhost:8081
```

Сервисы:

- Frontend (nginx + React build) — http://localhost:8081
- API (Spring Boot) — http://localhost:8080
- Swagger UI — http://localhost:8080/swagger-ui.html
- PostgreSQL — `localhost:5432` (пользователь/база из `.env`)

Чтобы создать первого администратора при первом старте, заполните в `.env`:

```
VARGOD_ADMIN_EMAIL=admin@example.com
VARGOD_ADMIN_PASSWORD=change-me
```

`AdminBootstrap` создаст пользователя с ролью `ADMIN`, если ни одного ещё нет.

---

## Architecture

5 backend-модулей (Kotlin, Gradle multi-project) + один React-фронт:

| Модуль | Назначение |
| --- | --- |
| `domain` | Доменные типы (`Experiment`, `ModelTemplate`, `Validator`, …), исключения, репозиторий-интерфейсы. Без зависимостей. |
| `business-logic` | Сервисы (`ExperimentService`, `ComparisonService`, `ModelService`, `AuthService`, …), реестр ОДУ-моделей, движок валидации, ODE-симулятор поверх Apache Commons Math. |
| `data-access` | JDBC-репозитории, Flyway-миграции, `DataSeeder` для эталонных шаблонов моделей. |
| `api` | Spring Boot REST: контроллеры, Spring Security + JWT (HS256), springdoc-openapi, admin-эндпоинты. |
| `app-cli` | CLI-приложение для скриптовых сценариев и smoke-тестов. |
| `frontend` | React 18 + TypeScript + Vite + Tailwind + shadcn/ui + TanStack Query/Table + Recharts. |

Подробнее: [docs/architecture.md](docs/architecture.md) — C4 (L1–L4), сиквенс- и BPMN-диаграммы.

---

## Biological models

| Код | Модель | 1-строчное описание |
| --- | --- | --- |
| `SIR` | SIR-эпидемия | S/I/R-компартменты, контактный коэффициент β и скорость выздоровления γ. |
| `SEIRD` | SEIRD-эпидемия | Расширение SIR: инкубация (E) и смертность (D). |
| `LOTKA_VOLTERRA` | Хищник-жертва (Лотка-Вольтерра) | Классическая 2D-система с линейной функциональной реакцией. |
| `ROSENZWEIG_MACARTHUR` | Хищник-жертва (Rosenzweig-MacArthur) | Логистический рост жертвы + насыщающаяся реакция II типа. |
| `LOGISTIC` | Логистический рост | Одиночная популяция с ёмкостью среды K. |
| `FITZHUGH_NAGUMO` | FitzHugh-Nagumo | 2D-упрощение Hodgkin-Huxley: возбудимая мембрана. |
| `HODGKIN_HUXLEY` | Hodgkin-Huxley | Каноническая 4D-модель потенциала действия (1952). Жёсткая. |

Уравнения, параметры, дефолты, ссылки: [docs/models.md](docs/models.md).

---

## Auth model

Аутентификация по JWT HS256: `POST /api/auth/register` или `POST /api/auth/login` возвращают токен, клиент кладёт его в `Authorization: Bearer <token>`. Роли: `ADMIN` и `RESEARCHER`; гость — это любой запрос без токена и видит только публичные эндпоинты (каталог моделей, опубликованные эксперименты). Подробнее об угрозах, граничных условиях и out-of-scope (XSS, refresh-токены, MFA, rate-limit): [docs/security.md](docs/security.md).

---

## API

Полный список эндпоинтов с auth-требованиями и форматами: [docs/api.md](docs/api.md).

Интерактивный обзор и «Try it out» — Swagger UI на [/swagger-ui.html](http://localhost:8080/swagger-ui.html). OpenAPI JSON — на `/v3/api-docs`. JWT можно вставить через кнопку **Authorize** в правом верхнем углу.

---

## Development

### Запуск тестов

```bash
./gradlew :domain:test :business-logic:test :data-access:test :api:test
```

Доменные и сервисные тесты — JUnit 5 + Mockk. Репозиторные тесты используют Testcontainers (нужен Docker).

### Frontend в dev-режиме

```bash
cd frontend
npm install
npm run dev
```

Dev-сервер Vite на http://localhost:5173 проксирует API на http://localhost:8080. CORS уже настроен (`VARGOD_CORS_ALLOWED_ORIGINS` включает `5173` и `8081`).

### Добавить новую ODE-модель

Короткое HOWTO: [docs/how-to-add-model.md](docs/how-to-add-model.md). По шагам: реализовать `OdeModel`, зарегистрировать в `MapModelRegistry`, добавить seed-функцию в `DataSeeder`, написать unit-тесты.

### Сборка отдельных модулей

```bash
./gradlew :api:bootJar          # исполняемый JAR Spring Boot
./gradlew :app-cli:installDist  # CLI-дистрибутив
./gradlew assemble              # всё сразу
```

---

## Status

Академический проект (МГТУ им. Н. Э. Баумана, кафедра ИУ7, курс «Проектирование программного обеспечения», 2026/2027). **Не production-ready.** Развивается итеративно по этапам P0–P7; история и обоснования решений — в [docs/lab-reports/lab2-lab7.md](docs/lab-reports/lab2-lab7.md) и [CHANGELOG.md](CHANGELOG.md).

Известные ограничения (см. [docs/security.md](docs/security.md)):

- JWT хранится в `localStorage` — уязвимо к XSS. Refresh-токены, MFA, rate-limit — out of scope.
- CORS строго ограничен env-списком; `*` запрещён.
- Нет горизонтального масштабирования воркеров симуляции — один эксперимент = один поток API.

---

## License

[MIT](LICENSE). Используйте, форкайте, отправляйте PR — см. [CONTRIBUTING.md](CONTRIBUTING.md) и [CODE_OF_CONDUCT.md](CODE_OF_CONDUCT.md).

## Acknowledgments

- [Apache Commons Math](https://commons.apache.org/proper/commons-math/) — численное решение ОДУ (Dormand-Prince 5(4)).
- [Spring Boot](https://spring.io/projects/spring-boot), [Spring Security](https://spring.io/projects/spring-security), [springdoc-openapi](https://springdoc.org/).
- [PostgreSQL](https://www.postgresql.org/), [Flyway](https://flywaydb.org/), [HikariCP](https://github.com/brettwooldridge/HikariCP).
- [React](https://react.dev/), [Vite](https://vitejs.dev/), [Tailwind CSS](https://tailwindcss.com/), [shadcn/ui](https://ui.shadcn.com/), [Radix UI](https://www.radix-ui.com/).
- [TanStack Query](https://tanstack.com/query) & [Table](https://tanstack.com/table), [Recharts](https://recharts.org/), [Zustand](https://zustand-demo.pmnd.rs/), [Zod](https://zod.dev/).
- Авторы моделей: Kermack & McKendrick (SIR, 1927), Lotka (1925) & Volterra (1926), Hodgkin & Huxley (1952), FitzHugh (1961) & Nagumo (1962), Rosenzweig & MacArthur (1963).
