-- ============================================================
--  V5 — Метаданные для admin-CRUD валидаторов (P3a).
--
--  До этого валидаторы создавались только через DataSeeder и считались
--  иммутабельными. Теперь admin может их редактировать/удалять через
--  /api/admin/validators/* — нам нужен аудит created_at/updated_at и
--  ссылка на пользователя-создателя (для UI «кто это сделал»).
--
--  created_by — ON DELETE SET NULL: удаление admin'а не должно каскадить
--  на валидаторы; запись просто «осиротеет».
-- ============================================================

ALTER TABLE validators
    ADD COLUMN created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    ADD COLUMN updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    ADD COLUMN created_by UUID REFERENCES users(id) ON DELETE SET NULL;
