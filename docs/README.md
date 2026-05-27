# Vargod docs index

Документация проекта — продуктовая (для пользователей и контрибьюторов).
Академические отчёты по лабораторным работам — в `lab-reports/`.

## Содержание

| Документ | О чём |
| --- | --- |
| [architecture.md](architecture.md) | Архитектура: 5 backend-модулей + frontend, C4 (L1–L4), сиквенсы, BPMN. |
| [api.md](api.md) | Список REST-эндпоинтов: метод, путь, auth, тело запроса, ответ, ошибки. |
| [security.md](security.md) | JWT HS256, BCrypt, AdminBootstrap, CORS, snapshot валидаторов, out-of-scope. |
| [models.md](models.md) | Каталог 7 ODE-моделей: уравнения, переменные, параметры, дефолты, ссылки. |
| [how-to-add-model.md](how-to-add-model.md) | HOWTO: как добавить новую ODE-модель за 5 шагов. |
| [lab-reports/lab2-lab7.md](lab-reports/lab2-lab7.md) | Архивный отчёт ЛР2–ЛР7 (Russian). |

## См. также

- [Корневой README.md](../README.md) — pitch, quick start, скриншоты.
- [CONTRIBUTING.md](../CONTRIBUTING.md) — dev environment, commit conventions, как добавлять миграции/тесты.
- [CHANGELOG.md](../CHANGELOG.md) — что изменилось по этапам P0–P7.
- [CODE_OF_CONDUCT.md](../CODE_OF_CONDUCT.md) — Contributor Covenant 2.1.
- Swagger UI: http://localhost:8080/swagger-ui.html (после `docker compose up`).
