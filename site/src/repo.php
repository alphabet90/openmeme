<?php
/**
 * Read-side queries against the SQLite index.
 * Search uses FTS5 when available and degrades to LIKE otherwise.
 */

declare(strict_types=1);

require_once __DIR__ . '/db.php';

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

function repo_trending(int $limit = 20): array
{
    $st = db()->prepare('SELECT * FROM memes ORDER BY score DESC LIMIT ?');
    $st->execute([$limit]);
    return $st->fetchAll();
}

function repo_newest(int $limit = 20): array
{
    $st = db()->prepare('SELECT * FROM memes ORDER BY created_at DESC LIMIT ?');
    $st->execute([$limit]);
    return $st->fetchAll();
}

/** Paginated full listing, $order 'top' (score) or 'new' (created_at). */
function repo_list(string $order, int $page): array
{
    $orderBy = $order === 'new' ? 'created_at DESC' : 'score DESC';
    $offset = ($page - 1) * PAGE_SIZE;
    $st = db()->prepare("SELECT * FROM memes ORDER BY $orderBy LIMIT ? OFFSET ?");
    $st->execute([PAGE_SIZE, $offset]);
    return ['rows' => $st->fetchAll(), 'total' => (int) repo_stats()['memes']];
}

function repo_random(): ?array
{
    $row = db()->query('SELECT * FROM memes ORDER BY RANDOM() LIMIT 1')->fetch();
    return $row === false ? null : $row;
}

function repo_meme(string $slug): ?array
{
    $st = db()->prepare('SELECT * FROM memes WHERE slug = ?');
    $st->execute([$slug]);
    $row = $st->fetch();
    return $row === false ? null : $row;
}

function repo_related(array $meme, int $limit = 8): array
{
    $st = db()->prepare(
        'SELECT * FROM memes WHERE category = ? AND id != ? ORDER BY score DESC LIMIT ?'
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
        'SELECT * FROM memes WHERE category = ? ORDER BY score DESC LIMIT ? OFFSET ?'
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
            'SELECT m.* FROM memes_fts f JOIN memes m ON m.id = f.rowid
             WHERE memes_fts MATCH ? ORDER BY rank, m.score DESC LIMIT ? OFFSET ?'
        );
        $st->execute([$match, PAGE_SIZE, $offset]);
        $rows = $st->fetchAll();

        $ct = db()->prepare('SELECT COUNT(*) FROM memes_fts WHERE memes_fts MATCH ?');
        $ct->execute([$match]);
        return ['rows' => $rows, 'total' => (int) $ct->fetchColumn()];
    }

    $like = '%' . str_replace(['%', '_'], ['\\%', '\\_'], $q) . '%';
    $where = "title LIKE ? ESCAPE '\\' OR description LIKE ? ESCAPE '\\'
              OR tags LIKE ? ESCAPE '\\' OR category LIKE ? ESCAPE '\\'";
    $st = db()->prepare(
        "SELECT * FROM memes WHERE $where ORDER BY score DESC LIMIT ? OFFSET ?"
    );
    $st->execute([$like, $like, $like, $like, PAGE_SIZE, $offset]);
    $rows = $st->fetchAll();

    $ct = db()->prepare("SELECT COUNT(*) FROM memes WHERE $where");
    $ct->execute([$like, $like, $like, $like]);
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
