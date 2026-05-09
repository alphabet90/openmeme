-- ============================================================================
-- V5__fts_for_es_ar.sql
-- Updates FTS helper functions to route 'es-ar' to the Spanish stemmer.
-- Runs after V4 commits so 'es-ar' is a visible enum value.
-- ============================================================================

CREATE OR REPLACE FUNCTION fts_config_for(p_locale locale_code)
RETURNS regconfig LANGUAGE sql IMMUTABLE PARALLEL SAFE AS $$
  SELECT CASE p_locale
    WHEN 'en'    THEN 'pg_catalog.english'::regconfig
    WHEN 'es'    THEN 'pg_catalog.spanish'::regconfig
    WHEN 'es-ar' THEN 'pg_catalog.spanish'::regconfig
    WHEN 'pt'    THEN 'pg_catalog.portuguese'::regconfig
    WHEN 'fr'    THEN 'pg_catalog.french'::regconfig
    WHEN 'de'    THEN 'pg_catalog.german'::regconfig
    ELSE              'pg_catalog.simple'::regconfig
  END;
$$;

CREATE OR REPLACE FUNCTION build_search_vector(
    p_locale       locale_code,
    p_title        TEXT,
    p_description  TEXT
) RETURNS tsvector LANGUAGE sql IMMUTABLE PARALLEL SAFE AS $$
  SELECT
    setweight(to_tsvector(
        CASE p_locale
            WHEN 'en'    THEN 'pg_catalog.english'::regconfig
            WHEN 'es'    THEN 'pg_catalog.spanish'::regconfig
            WHEN 'es-ar' THEN 'pg_catalog.spanish'::regconfig
            WHEN 'pt'    THEN 'pg_catalog.portuguese'::regconfig
            WHEN 'fr'    THEN 'pg_catalog.french'::regconfig
            WHEN 'de'    THEN 'pg_catalog.german'::regconfig
            ELSE              'pg_catalog.simple'::regconfig
        END,
        immutable_unaccent(coalesce(p_title, ''))
    ), 'A')
  ||setweight(to_tsvector(
        CASE p_locale
            WHEN 'en'    THEN 'pg_catalog.english'::regconfig
            WHEN 'es'    THEN 'pg_catalog.spanish'::regconfig
            WHEN 'es-ar' THEN 'pg_catalog.spanish'::regconfig
            WHEN 'pt'    THEN 'pg_catalog.portuguese'::regconfig
            WHEN 'fr'    THEN 'pg_catalog.french'::regconfig
            WHEN 'de'    THEN 'pg_catalog.german'::regconfig
            ELSE              'pg_catalog.simple'::regconfig
        END,
        immutable_unaccent(coalesce(p_description, ''))
    ), 'B');
$$;
