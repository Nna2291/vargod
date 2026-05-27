-- ============================================================
--  V7 — Подсказки решателя на уровне модели.
--
--  P4: новые жёсткие модели (Hodgkin-Huxley) требуют гораздо меньшего
--  максимального шага и более узких толерантностей, чем разумно ставить
--  глобально. Храним рекомендуемые значения прямо в шаблоне модели —
--  OdeSimulationEngine использует их вместо инстанс-defaults, если заданы.
-- ============================================================

ALTER TABLE model_templates
    ADD COLUMN recommended_max_step DOUBLE PRECISION,
    ADD COLUMN recommended_rel_tol  DOUBLE PRECISION,
    ADD COLUMN recommended_abs_tol  DOUBLE PRECISION;
