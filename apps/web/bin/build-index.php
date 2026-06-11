<?php
/**
 * Build the SQLite index from /memes/*.
 *
 * The meme collection (MDX frontmatter + images) is the source of truth;
 * this index is disposable and rebuilt atomically (write temp DB, rename).
 * Full-text search lives in Meilisearch — after this script, run
 * bin/build-search.php to push the search index.
 *
 * Usage: php bin/build-index.php && php bin/build-search.php
 */

declare(strict_types=1);

require __DIR__ . '/../config.php';

$start = microtime(true);
$tmpPath = DB_PATH . '.tmp';

@mkdir(dirname(DB_PATH), 0775, true);
@unlink($tmpPath);

$pdo = new PDO('sqlite:' . $tmpPath, null, null, [
    PDO::ATTR_ERRMODE => PDO::ERRMODE_EXCEPTION,
]);
$pdo->exec('PRAGMA journal_mode = MEMORY');
$pdo->exec('PRAGMA synchronous = OFF');

$pdo->exec('
    CREATE TABLE memes (
        id INTEGER PRIMARY KEY,
        slug TEXT NOT NULL UNIQUE,
        title TEXT NOT NULL,
        description TEXT NOT NULL DEFAULT "",
        author TEXT NOT NULL DEFAULT "",
        subreddit TEXT NOT NULL DEFAULT "",
        category TEXT NOT NULL,
        score INTEGER NOT NULL DEFAULT 0,
        created_at TEXT NOT NULL DEFAULT "",
        source_url TEXT NOT NULL DEFAULT "",
        post_url TEXT NOT NULL DEFAULT "",
        image TEXT NOT NULL,
        width INTEGER NOT NULL DEFAULT 0,
        height INTEGER NOT NULL DEFAULT 0,
        tags TEXT NOT NULL DEFAULT ""
    );
    CREATE INDEX idx_memes_category ON memes(category, score DESC);
    CREATE INDEX idx_memes_score ON memes(score DESC);
    CREATE INDEX idx_memes_created ON memes(created_at DESC);

    CREATE TABLE meme_locales (
        meme_id INTEGER NOT NULL REFERENCES memes(id),
        locale TEXT NOT NULL,
        title TEXT NOT NULL,
        description TEXT NOT NULL DEFAULT "",
        tags TEXT NOT NULL DEFAULT "",
        PRIMARY KEY (meme_id, locale)
    );
');

/**
 * Minimal YAML-frontmatter parser for the flat key/value format used in
 * meme MDX files (quoted strings, ints, inline JSON-style arrays).
 */
function parse_frontmatter(string $raw): ?array
{
    if (!preg_match('/^---\R(.*?)\R---/s', $raw, $m)) {
        return null;
    }
    $out = [];
    foreach (preg_split('/\R/', $m[1]) as $line) {
        if (!preg_match('/^([A-Za-z_][\w]*):\s*(.*)$/', $line, $kv)) {
            continue;
        }
        [, $key, $val] = $kv;
        $val = trim($val);
        if ($val !== '' && $val[0] === '[') {
            $arr = json_decode($val, true);
            if (!is_array($arr)) {
                $arr = json_decode(str_replace("'", '"', $val), true);
            }
            $out[$key] = is_array($arr) ? $arr : [];
        } elseif (preg_match('/^"(.*)"$/s', $val, $q)) {
            $out[$key] = stripcslashes($q[1]);
        } elseif (preg_match("/^'(.*)'$/s", $val, $q)) {
            $out[$key] = $q[1];
        } else {
            $out[$key] = $val;
        }
    }
    return $out;
}

/** Base meme files only — locale variants like slug.es-AR.mdx are translations. */
function is_locale_variant(string $file): bool
{
    return (bool) preg_match('/\.[a-z]{2}(-[A-Za-z]{2})?\.mdx$/', $file);
}

$insert = $pdo->prepare('
    INSERT INTO memes (slug, title, description, author, subreddit, category,
                       score, created_at, source_url, post_url, image, width, height, tags)
    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
');

$insertLocale = $pdo->prepare('
    INSERT OR REPLACE INTO meme_locales (meme_id, locale, title, description, tags)
    VALUES (?, ?, ?, ?, ?)
');
$findBySlug = $pdo->prepare('SELECT id FROM memes WHERE slug = ?');

$stats = ['indexed' => 0, 'skipped' => 0, 'dupes' => 0, 'no_image' => 0, 'locales' => 0, 'es_only' => 0];
$pdo->beginTransaction();

/** Validate frontmatter, resolve the image, and insert a canonical meme row. Returns id or null. */
function insert_meme(array $fm, string $dir, string $dirName, PDOStatement $insert, PDO $pdo, array &$stats): ?int
{
    if (empty($fm['title']) || empty($fm['slug']) || empty($fm['image'])) {
        $stats['skipped']++;
        return null;
    }
    $imageFile = $dir . '/' . basename((string) $fm['image']);
    if (!is_file($imageFile)) {
        $stats['no_image']++;
        return null;
    }
    $size = @getimagesize($imageFile);
    [$w, $h] = $size === false ? [0, 0] : [$size[0], $size[1]];
    $tags = $fm['tags'] ?? [];
    try {
        $insert->execute([
            $fm['slug'],
            $fm['title'],
            $fm['description'] ?? '',
            $fm['author'] ?? '',
            $fm['subreddit'] ?? '',
            $fm['category'] ?? $dirName,
            (int) ($fm['score'] ?? 0),
            $fm['created_at'] ?? '',
            $fm['source_url'] ?? '',
            $fm['post_url'] ?? '',
            $dirName . '/' . basename($imageFile),
            $w,
            $h,
            is_array($tags) ? implode(' ', $tags) : (string) $tags,
        ]);
        $stats['indexed']++;
        return (int) $pdo->lastInsertId();
    } catch (PDOException $e) {
        if (str_contains($e->getMessage(), 'UNIQUE')) {
            $stats['dupes']++;
            return null;
        }
        throw $e;
    }
}

// Pass 1 — base .mdx files become the canonical (English) rows.
$dirs = [];
foreach (glob(MEMES_DIR . '/*', GLOB_ONLYDIR) as $dir) {
    $dirName = basename($dir);
    if ($dirName[0] === '_' || $dirName[0] === '.') {
        continue;
    }
    $dirs[] = [$dir, $dirName];
    foreach (glob($dir . '/*.mdx') as $file) {
        if (is_locale_variant($file)) {
            continue;
        }
        $fm = parse_frontmatter((string) file_get_contents($file));
        if ($fm === null) {
            $stats['skipped']++;
            continue;
        }
        insert_meme($fm, $dir, $dirName, $insert, $pdo, $stats);
    }
}

// Pass 2 — .es-AR.mdx files are the Argentina memes. They localize the
// canonical row, or become canonical themselves when no base file exists.
foreach ($dirs as [$dir, $dirName]) {
    foreach (glob($dir . '/*.es-AR.mdx') as $file) {
        $fm = parse_frontmatter((string) file_get_contents($file));
        if ($fm === null || empty($fm['title']) || empty($fm['slug'])) {
            $stats['skipped']++;
            continue;
        }
        $findBySlug->execute([$fm['slug']]);
        $id = $findBySlug->fetchColumn();
        if ($id === false) {
            $id = insert_meme($fm, $dir, $dirName, $insert, $pdo, $stats);
            if ($id === null) {
                continue;
            }
            $stats['es_only']++;
        }
        $tags = $fm['tags'] ?? [];
        $insertLocale->execute([
            (int) $id,
            'es-AR',
            $fm['title'],
            $fm['description'] ?? '',
            is_array($tags) ? implode(' ', $tags) : (string) $tags,
        ]);
        $stats['locales']++;
    }
}

$pdo->commit();
$pdo->exec('PRAGMA optimize');
$pdo = null;

rename($tmpPath, DB_PATH);

printf(
    "Indexed %d memes, %d es-AR translations (%d es-AR-only, %d skipped, %d duplicate slugs, %d missing images) in %.1fs → %s\n",
    $stats['indexed'],
    $stats['locales'],
    $stats['es_only'],
    $stats['skipped'],
    $stats['dupes'],
    $stats['no_image'],
    microtime(true) - $start,
    DB_PATH
);
