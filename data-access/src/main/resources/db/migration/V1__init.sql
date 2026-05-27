-- ============================================================
--  Vargod — начальная схема БД.
--  Соответствует ER-диаграмме в diagrams/schema.dbml.
--  Управляется Flyway: после применения попадает в flyway_schema_history.
--  IF NOT EXISTS не используется — Flyway сам отслеживает применённые миграции.
-- ============================================================

CREATE TABLE model_templates (
    id          UUID         PRIMARY KEY,
    code        VARCHAR(50)  UNIQUE NOT NULL,
    name        VARCHAR(255) NOT NULL,
    description TEXT,
    solver_type VARCHAR(20)  NOT NULL
);

CREATE TABLE validators (
    id                UUID         PRIMARY KEY,
    name              VARCHAR(255) NOT NULL,
    check_type        VARCHAR(20)  NOT NULL,
    target_kind       VARCHAR(20)  NOT NULL,
    target_field      VARCHAR(100) NOT NULL,
    rules             TEXT         NOT NULL,
    model_template_id UUID         REFERENCES model_templates(id) ON DELETE CASCADE
);

CREATE TABLE experiments (
    id                UUID                     PRIMARY KEY,
    name              VARCHAR(255),
    status            VARCHAR(20)              NOT NULL,
    is_public         BOOLEAN                  NOT NULL DEFAULT FALSE,
    created_at        TIMESTAMP WITH TIME ZONE NOT NULL,
    user_id           UUID                     NOT NULL,
    model_template_id UUID                     NOT NULL REFERENCES model_templates(id)
);

CREATE TABLE parameter_sets (
    id            UUID   PRIMARY KEY,
    "values"      TEXT   NOT NULL,
    t_start       DOUBLE PRECISION NOT NULL,
    t_end         DOUBLE PRECISION NOT NULL,
    step          DOUBLE PRECISION NOT NULL,
    experiment_id UUID   NOT NULL UNIQUE REFERENCES experiments(id) ON DELETE CASCADE
);

CREATE TABLE simulation_results (
    id            UUID                     PRIMARY KEY,
    data          TEXT                     NOT NULL,
    created_at    TIMESTAMP WITH TIME ZONE NOT NULL,
    experiment_id UUID                     NOT NULL UNIQUE REFERENCES experiments(id) ON DELETE CASCADE
);

CREATE TABLE comparisons (
    id         UUID                     PRIMARY KEY,
    name       VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    user_id    UUID                     NOT NULL
);

CREATE TABLE comparison_experiments (
    comparison_id UUID NOT NULL REFERENCES comparisons(id) ON DELETE CASCADE,
    experiment_id UUID NOT NULL REFERENCES experiments(id) ON DELETE CASCADE,
    position      INT  NOT NULL,
    PRIMARY KEY (comparison_id, experiment_id)
);
