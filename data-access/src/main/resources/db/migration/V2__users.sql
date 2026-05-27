-- ============================================================
--  V2 — Пользователи.
--  P1: введение реальной аутентификации (JWT + BCrypt) и ролей.
--  До этого момента userId был просто UUID, передаваемый клиентом
--  в заголовке X-User-Id без какой-либо записи в БД.
-- ============================================================

CREATE TABLE users (
    id            UUID                     PRIMARY KEY,
    -- LOWER(email) нормализуется на уровне приложения; уникальность по lower-case.
    email         VARCHAR(255)             NOT NULL,
    password_hash VARCHAR(72)              NOT NULL,
    role          VARCHAR(20)              NOT NULL,
    created_at    TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE UNIQUE INDEX users_email_lower_uniq ON users (LOWER(email));
CREATE INDEX users_role_idx ON users (role);
