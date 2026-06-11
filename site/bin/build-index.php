<?php
/**
 * Build the SQLite index from /memes/*.
 *
 * The meme collection (MDX frontmatter + images) is the source of truth;
 * this index is disposable and rebuilt atomically (write temp DB, rename).
 *
 * Usage: php site/bin/build-index.php
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
');

$hasFts = true;
try {
    $pdo->exec("
        CREATE VIRTUAL TABLE memes_fts USING fts5(
            title, description, tags, category,
            content='memes', content_rowid='id'
        )
    ");
} catch (PDOException $e) {
    $hasFts = false;
    fwrite(STDERR, "warn: FTS5 unavailable, search will use LIKE fallback\n");
}

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

$stats = ['indexed' => 0, 'skipped' => 0, 'dupes' => 0, 'no_image' => 0];
$pdo->beginTransaction();

foreach (glob(MEMES_DIR . '/*', GLOB_ONLYDIR) as $dir) {
    $dirName = basename($dir);
    if ($dirName[0] === '_' || $dirName[0] === '.') {
        continue;
    }
    foreach (glob($dir . '/*.mdx') as $file) {
        if (is_locale_variant($file)) {
            continue;
        }
        $fm = parse_frontmatter((string) file_get_contents($file));
        if ($fm === null || empty($fm['title']) || empty($fm['slug']) || empty($fm['image'])) {
            $stats['skipped']++;
            continue;
        }

        $imageFile = $dir . '/' . basename((string) $fm['image']);
        if (!is_file($imageFile)) {
            $stats['no_image']++;
            continue;
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
        } catch (PDOException $e) {
            if (str_contains($e->getMessage(), 'UNIQUE')) {
                $stats['dupes']++;
            } else {
                throw $e;
            }
        }
    }
}

if ($hasFts) {
    $pdo->exec("
        INSERT INTO memes_fts (rowid, title, description, tags, category)
        SELECT id, title, description, tags, category FROM memes
    ");
}
$pdo->commit();
$pdo->exec('PRAGMA optimize');
$pdo = null;

rename($tmpPath, DB_PATH);

printf(
    "Indexed %d memes (%d skipped, %d duplicate slugs, %d missing images) in %.1fs → %s\n",
    $stats['indexed'],
    $stats['skipped'],
    $stats['dupes'],
    $stats['no_image'],
    microtime(true) - $start,
    DB_PATH
);
