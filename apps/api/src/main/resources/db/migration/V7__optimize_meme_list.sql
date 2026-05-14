CREATE OR REPLACE FUNCTION list_memes_flat(p_locale locale_code)
RETURNS TABLE (
    id            bigint,
    slug          slug,
    score         integer,
    created_at    timestamptz,
    indexed_at    timestamptz,
    category_slug slug,
    author_username citext,
    title         text,
    description   text,
    image_path    text,
    tags          text[]
)
LANGUAGE sql STABLE
AS $$
    SELECT
        m.id,
        m.slug,
        m.score,
        m.created_at,
        m.indexed_at,
        c.slug        AS category_slug,
        a.username    AS author_username,
        mt.title,
        mt.description,
        mi.path       AS image_path,
        array_agg(DISTINCT t.slug::text ORDER BY t.slug::text) AS tags
    FROM memes m
    JOIN categories c ON c.id = m.category_id
    LEFT JOIN authors a ON a.id = m.author_id
    JOIN meme_translations mt
        ON mt.meme_id = m.id
        AND mt.locale = p_locale
    LEFT JOIN meme_images mi
        ON mi.meme_id = m.id
        AND mi.is_primary = true
    LEFT JOIN meme_tags mtg ON mtg.meme_id = m.id
    LEFT JOIN tags t ON t.id = mtg.tag_id
    WHERE m.deleted_at IS NULL
    GROUP BY
        m.id,
        c.slug,
        a.username,
        mt.title,
        mt.description,
        mi.path;
$$;
