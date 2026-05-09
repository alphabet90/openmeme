-- ============================================================================
-- V6__seed_es_ar_translations.sql
-- Back-fills es-ar rows by copying every es translation that does not yet
-- have an es-ar counterpart. ON CONFLICT DO NOTHING makes the migration
-- idempotent and safe to apply on environments that already have some
-- es-ar rows.
--
-- meme_translations.search_vector is GENERATED ALWAYS and is therefore
-- omitted; PostgreSQL recomputes it automatically on insert.
-- ============================================================================

INSERT INTO category_translations (category_id, locale, name, description)
SELECT category_id, 'es-ar'::locale_code, name, description
FROM   category_translations
WHERE  locale = 'es'
ON CONFLICT (category_id, locale) DO NOTHING;

INSERT INTO tag_translations (tag_id, locale, name)
SELECT tag_id, 'es-ar'::locale_code, name
FROM   tag_translations
WHERE  locale = 'es'
ON CONFLICT (tag_id, locale) DO NOTHING;

INSERT INTO meme_translations (meme_id, locale, title, description)
SELECT meme_id, 'es-ar'::locale_code, title, description
FROM   meme_translations
WHERE  locale = 'es'
ON CONFLICT (meme_id, locale) DO NOTHING;
