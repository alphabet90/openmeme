-- ============================================================================
-- V4__add_es_ar_locale.sql
-- Adds 'es-ar' (Argentina Spanish) to the locale_code enum.
--
-- ALTER TYPE ... ADD VALUE cannot use the new value within the same
-- transaction (PG 12+: runs in a transaction but the value is invisible
-- until commit). Function updates that reference 'es-ar' live in V5.
-- ============================================================================

ALTER TYPE locale_code ADD VALUE IF NOT EXISTS 'es-ar';
