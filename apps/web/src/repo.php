<?php
/**
 * Read-side queries against the SQLite index.
 *
 * Every meme row is localized for the active LOCALE: es-AR text comes from
 * meme_locales (indexed from the .es-AR.mdx files) and falls back to the
 * canonical English columns. Search uses FTS5 with locale column filters
 * and degrades to LIKE otherwise.
 */

declare(strict_types=1);

require_once __DIR__ . '/db.php';

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

function repo_has_fts(): bool
{
    static $has = null;
    if ($has === null) {
        $has = (bool) db()->query(
            "SELECT 1 FROM sqlite_master WHERE type = 'table' AND name = 'memes_fts'"
        )->fetch();
    }
    return $has;
}

/** Escape user input into a prefix-matching FTS5 query: each token quoted + starred. */
function fts_query(string $q): string
{
    $tokens = preg_split('/\s+/', trim($q), -1, PREG_SPLIT_NO_EMPTY);
    $parts = [];
    foreach (array_slice($tokens, 0, 8) as $t) {
        $parts[] = '"' . str_replace('"', '""', $t) . '"*';
    }
    return implode(' ', $parts);
}

/**
 * Search is multilingual in both locales: the query matches the English
 * base AND the es-AR columns, so "noose" works on the Spanish site and
 * "soga" works on the English site. Results render in the active locale.
 */
function repo_search(string $q, int $page = 1): array
{
    $q = trim($q);
    if ($q === '') {
        return ['rows' => [], 'total' => 0];
    }
    $offset = ($page - 1) * PAGE_SIZE;

    if (repo_has_fts()) {
        $match = fts_query($q);
        $st = db()->prepare(
            'SELECT x.* FROM memes_fts f JOIN (' . meme_select() . ') x ON x.id = f.rowid
             WHERE memes_fts MATCH ? ORDER BY rank, x.score DESC LIMIT ? OFFSET ?'
        );
        $st->execute([$match, PAGE_SIZE, $offset]);
        $rows = $st->fetchAll();

        $ct = db()->prepare('SELECT COUNT(*) FROM memes_fts WHERE memes_fts MATCH ?');
        $ct->execute([$match]);
        return ['rows' => $rows, 'total' => (int) $ct->fetchColumn()];
    }

    $like = '%' . str_replace(['%', '_'], ['\\%', '\\_'], $q) . '%';
    $where = "m2.title LIKE :q ESCAPE '\\' OR m2.description LIKE :q ESCAPE '\\'
              OR m2.tags LIKE :q ESCAPE '\\' OR m2.category LIKE :q ESCAPE '\\'
              OR l2.title LIKE :q ESCAPE '\\' OR l2.description LIKE :q ESCAPE '\\'
              OR l2.tags LIKE :q ESCAPE '\\'";
    $ids = "SELECT DISTINCT m2.id FROM memes m2
            LEFT JOIN meme_locales l2 ON l2.meme_id = m2.id
            WHERE $where";
    $st = db()->prepare(
        'SELECT x.* FROM (' . meme_select() . ") x WHERE x.id IN ($ids)
         ORDER BY x.score DESC LIMIT :lim OFFSET :off"
    );
    $st->bindValue(':q', $like);
    $st->bindValue(':lim', PAGE_SIZE, PDO::PARAM_INT);
    $st->bindValue(':off', $offset, PDO::PARAM_INT);
    $st->execute();
    $rows = $st->fetchAll();

    $ct = db()->prepare("SELECT COUNT(*) FROM ($ids)");
    $ct->execute([':q' => $like]);
    return ['rows' => $rows, 'total' => (int) $ct->fetchColumn()];
}

function repo_suggest(string $q, int $limit = 8): array
{
    $result = repo_search($q, 1);
    $out = [];
    foreach (array_slice($result['rows'], 0, $limit) as $m) {
        $out[] = [
            'term' => $m['title'],
            'slug' => $m['slug'],
            'cat' => $m['category'],
        ];
    }
    return $out;
}
