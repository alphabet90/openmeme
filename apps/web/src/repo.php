<?php
/**
 * Read-side queries against the SQLite index.
 *
 * Every meme row is localized for the active LOCALE: es-AR text comes from
 * meme_locales (indexed from the .es-AR.mdx files) and falls back to the
 * canonical English columns. Search and suggestions run on Meilisearch
 * (src/meili.php, indexed by bin/build-search.php); rows are rehydrated
 * from SQLite so rendering has a single source of truth.
 */

declare(strict_types=1);

require_once __DIR__ . '/db.php';
require_once __DIR__ . '/meili.php';

/**
 * Shared locale-aware SELECT with title/description/tags localized.
 * Columns are listed explicitly (no m.*) so there are no duplicate names —
 * nested SELECTs would otherwise shadow the localized aliases.
 */
function meme_select(): string
{
    $tag = db()->quote(locale_tag());
    return "SELECT m.id, m.slug, m.author, m.subreddit, m.category, m.score,
                   m.created_at, m.source_url, m.post_url, m.image, m.width, m.height,
                   COALESCE(l.title, m.title) AS title,
                   COALESCE(l.description, m.description) AS description,
                   COALESCE(l.tags, m.tags) AS tags
            FROM memes m
            LEFT JOIN meme_locales l ON l.meme_id = m.id AND l.locale = $tag";
}

function repo_stats(): array
{
    static $stats = null;
    if ($stats === null) {
        $stats = db()->query(
            'SELECT COUNT(*) AS memes,
                    COUNT(DISTINCT category) AS categories,
                    COALESCE(SUM(score), 0) AS upvotes
             FROM memes'
        )->fetch();
    }
    return $stats;
}

function repo_trending(int $limit = 20, int $offset = 0): array
{
    $st = db()->prepare(meme_select() . ' ORDER BY m.score DESC LIMIT ? OFFSET ?');
    $st->execute([$limit, $offset]);
    return $st->fetchAll();
}

function repo_newest(int $limit = 20): array
{
    $st = db()->prepare(meme_select() . ' ORDER BY m.created_at DESC LIMIT ?');
    $st->execute([$limit]);
    return $st->fetchAll();
}

/** Paginated full listing, $order 'top' (score) or 'new' (created_at). */
function repo_list(string $order, int $page): array
{
    $orderBy = $order === 'new' ? 'm.created_at DESC' : 'm.score DESC';
    $offset = ($page - 1) * PAGE_SIZE;
    $st = db()->prepare(meme_select() . " ORDER BY $orderBy LIMIT ? OFFSET ?");
    $st->execute([PAGE_SIZE, $offset]);
    return ['rows' => $st->fetchAll(), 'total' => (int) repo_stats()['memes']];
}

function repo_random(): ?array
{
    $row = db()->query(meme_select() . ' ORDER BY RANDOM() LIMIT 1')->fetch();
    return $row === false ? null : $row;
}

function repo_meme(string $slug): ?array
{
    $st = db()->prepare(meme_select() . ' WHERE m.slug = ?');
    $st->execute([$slug]);
    $row = $st->fetch();
    return $row === false ? null : $row;
}

function repo_related(array $meme, int $limit = 8): array
{
    $st = db()->prepare(
        meme_select() . ' WHERE m.category = ? AND m.id != ? ORDER BY m.score DESC LIMIT ?'
    );
    $st->execute([$meme['category'], $meme['id'], $limit]);
    return $st->fetchAll();
}

function repo_categories(int $limit = 0): array
{
    $sql = 'SELECT category, COUNT(*) AS n FROM memes GROUP BY category ORDER BY n DESC';
    if ($limit > 0) {
        $sql .= ' LIMIT ' . $limit;
    }
    return db()->query($sql)->fetchAll();
}

function repo_category_page(string $category, int $page): array
{
    $offset = ($page - 1) * PAGE_SIZE;
    $st = db()->prepare(
        meme_select() . ' WHERE m.category = ? ORDER BY m.score DESC LIMIT ? OFFSET ?'
    );
    $st->execute([$category, PAGE_SIZE, $offset]);
    $rows = $st->fetchAll();

    $ct = db()->prepare('SELECT COUNT(*) FROM memes WHERE category = ?');
    $ct->execute([$category]);
    return ['rows' => $rows, 'total' => (int) $ct->fetchColumn()];
}

/** Localized rows for the given slugs, preserving the input order. */
function repo_by_slugs(array $slugs): array
{
    if ($slugs === []) {
        return [];
    }
    $ph = implode(',', array_fill(0, count($slugs), '?'));
    $st = db()->prepare('SELECT x.* FROM (' . meme_select() . ") x WHERE x.slug IN ($ph)");
    $st->execute(array_values($slugs));
    $bySlug = array_column($st->fetchAll(), null, 'slug');
    $rows = [];
    foreach ($slugs as $slug) {
        if (isset($bySlug[$slug])) {
            $rows[] = $bySlug[$slug];
        }
    }
    return $rows;
}

/**
 * Search is multilingual in both locales: one Meilisearch query matches the
 * English (*_en) AND es-AR (*_es) fields, so "noose" works on the Spanish
 * site and "soga" on the English site. Results render in the active locale.
 *
 * @throws SearchUnavailableException when Meilisearch is unreachable/errored
 */
function repo_search(string $q, int $page = 1): array
{
    $q = mb_substr(trim($q), 0, MAX_QUERY_LENGTH);
    if ($q === '') {
        return ['rows' => [], 'total' => 0];
    }

    try {
        $res = meili_search_client()->index(MEILI_INDEX)->search($q, [
            // page + hitsPerPage → exhaustive totalHits (pagination shows real totals)
            'page' => $page,
            'hitsPerPage' => PAGE_SIZE,
            'attributesToRetrieve' => ['id'],
            // constrain query-language detection to our two languages
            'locales' => ['spa', 'eng'],
        ]);
    } catch (Throwable $e) {
        throw new SearchUnavailableException($e->getMessage(), 0, $e);
    }

    $slugs = array_column($res->getHits(), 'id');
    return ['rows' => repo_by_slugs($slugs), 'total' => (int) $res->getTotalHits()];
}

/** Dropdown suggestions. Degrades to [] on any Meilisearch failure. */
function repo_suggest(string $q, int $limit = 6): array
{
    $q = mb_substr(trim($q), 0, MAX_QUERY_LENGTH);
    if (mb_strlen($q) < MIN_SUGGEST_LENGTH) {
        return [];
    }

    try {
        $res = meili_search_client()->index(MEILI_INDEX)->search($q, [
            'limit' => $limit,
            'attributesToRetrieve' => ['slug', 'title_en', 'title_es', 'category'],
            'locales' => ['spa', 'eng'],
        ]);
    } catch (Throwable) {
        return [];
    }

    $out = [];
    foreach ($res->getHits() as $hit) {
        $out[] = [
            'term' => meili_suggest_term($hit, LOCALE),
            'slug' => $hit['slug'],
            'cat' => $hit['category'],
        ];
    }
    return $out;
}
