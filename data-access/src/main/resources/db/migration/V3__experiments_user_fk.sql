-- ============================================================
--  V3 — Бэкфилл "legacy" пользователей и FK на experiments/comparisons.
--
--  До P1 поле user_id заполнялось произвольными UUID, передаваемыми клиентом.
--  Чтобы не потерять историю — для каждого уникального user_id из
--  experiments/comparisons создаём заглушечного пользователя с ролью
--  RESEARCHER и невалидным password_hash (войти под ним нельзя).
--  Заодно вводим внешний ключ с ON DELETE SET NULL: удаление пользователя
--  больше не каскадит на эксперименты.
-- ============================================================

-- Берём все ранее использованные user_id (DISTINCT) из обеих таблиц.
INSERT INTO users (id, email, password_hash, role, created_at)
SELECT t.user_id,
       'legacy-' || t.user_id::text || '@vargod.local',
       '!INVALID',  -- BCrypt-хеши начинаются с $2; '!' гарантирует, что verify никогда не пройдёт
       'RESEARCHER',
       NOW()
FROM (
    SELECT DISTINCT user_id FROM experiments
    UNION
    SELECT DISTINCT user_id FROM comparisons
) t
WHERE NOT EXISTS (SELECT 1 FROM users u WHERE u.id = t.user_id);

ALTER TABLE experiments
    ALTER COLUMN user_id DROP NOT NULL,
    ADD CONSTRAINT experiments_user_id_fk
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL;

ALTER TABLE comparisons
    ALTER COLUMN user_id DROP NOT NULL,
    ADD CONSTRAINT comparisons_user_id_fk
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL;
