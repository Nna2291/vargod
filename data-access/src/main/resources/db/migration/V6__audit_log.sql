-- ============================================================
--  V6 — Журнал admin-действий (P3a).
--
--  Назначение: дать admin'у/аудитору ленту того, кто и что менял в системе.
--  user_email — снимок на момент записи: пользователь может быть удалён
--  (FK SET NULL), но запись должна оставаться читаемой.
--  payload — произвольный JSON (детали действия, прежние/новые значения).
--
--  Все три индекса оправданы типичными запросами:
--    - created_at DESC — лента «последних N записей»;
--    - user_id — «что делал пользователь X»;
--    - action — фильтр по типу действия.
-- ============================================================

CREATE TABLE audit_log (
    id          UUID PRIMARY KEY,
    user_id     UUID REFERENCES users(id) ON DELETE SET NULL,
    user_email  VARCHAR(255),
    action      VARCHAR(50) NOT NULL,
    target_type VARCHAR(50),
    target_id   UUID,
    payload     TEXT,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL
);
CREATE INDEX audit_log_created_at_idx ON audit_log (created_at DESC);
CREATE INDEX audit_log_user_id_idx    ON audit_log (user_id);
CREATE INDEX audit_log_action_idx     ON audit_log (action);
