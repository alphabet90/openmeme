<?php
/**
 * OpenMeme site configuration.
 * Single source of truth for paths. No framework, no magic.
 */

declare(strict_types=1);

define('SITE_ROOT', __DIR__);
define('REPO_ROOT', dirname(__DIR__, 2));

// Composer autoload (meilisearch-php SDK).
if (!is_file(SITE_ROOT . '/vendor/autoload.php')) {
    http_response_code(503);
    exit("Dependencies not installed. Run: composer install (in apps/web)\n");
}
require_once SITE_ROOT . '/vendor/autoload.php';

// /memes/* is the source of truth. The SQLite DB is a disposable index of it.
define('MEMES_DIR', REPO_ROOT . '/memes');
define('DB_PATH', SITE_ROOT . '/data/memes.db');

define('TEMPLATES_DIR', SITE_ROOT . '/templates');

// Default page size everywhere: home grid, listings, search,
// and the "show more" batch size.
define('PAGE_SIZE', 100);

// Memes newer than this many days get the NEW badge
define('NEW_DAYS', 7);

// Search abuse limits: queries are truncated server-side, and the suggest
// endpoint ignores queries shorter than this (mirrored client-side in app.js).
define('MAX_QUERY_LENGTH', 100);
define('MIN_SUGGEST_LENGTH', 2);

// Load apps/web/.env (fallback: repo-root .env). Real environment
// variables always win, so PHP-FPM env[...] / fastcgi_param overrides work.
load_env();

// Canonical base URL for sitemap/OG tags. Override in production via env.
define('BASE_URL', rtrim(env('OPENMEME_BASE_URL') ?: detect_base_url(), '/'));

// Optional CDN host for meme images (e.g. https://cdn.openmeme.io).
// Empty = serve images same-origin from /memes/*.
define('CDN_URL', rtrim(env('OPENMEME_CDN_URL'), '/'));

// Meilisearch (required for /search and /api/suggest).
// Scoped keys are a production hardening step; dev can run on the master key.
define('MEILI_URL', rtrim(env('MEILI_URL', 'http://127.0.0.1:7700'), '/'));
define('MEILI_SEARCH_KEY', env('MEILI_SEARCH_KEY') ?: env('MEILI_MASTER_KEY'));
define('MEILI_ADMIN_KEY', env('MEILI_ADMIN_KEY') ?: env('MEILI_MASTER_KEY'));
define('MEILI_INDEX', 'memes');

function load_env(?array $files = null): void
{
    foreach ($files ?? [SITE_ROOT . '/.env', REPO_ROOT . '/.env'] as $file) {
        if (!is_file($file)) {
            continue;
        }
        foreach (file($file, FILE_IGNORE_NEW_LINES | FILE_SKIP_EMPTY_LINES) as $line) {
            $line = trim($line);
            if ($line === '' || $line[0] === '#' || !str_contains($line, '=')) {
                continue;
            }
            [$key, $value] = explode('=', $line, 2);
            $key = trim($key);
            $value = trim($value);
            if ($value !== '' && ($value[0] === '"' || $value[0] === "'")) {
                $value = trim($value, $value[0]);
            }
            if ($key !== '' && getenv($key) === false) {
                putenv("$key=$value");
            }
        }
        break; // first file found wins
    }
}

function env(string $key, string $default = ''): string
{
    $v = getenv($key);
    if ($v === false || $v === '') {
        $v = (string) ($_SERVER[$key] ?? '');
    }
    return $v !== '' ? $v : $default;
}

function detect_base_url(): string
{
    $host = $_SERVER['HTTP_HOST'] ?? 'localhost:8090';
    $isHttps = (!empty($_SERVER['HTTPS']) && $_SERVER['HTTPS'] !== 'off')
        || ($_SERVER['HTTP_X_FORWARDED_PROTO'] ?? '') === 'https';
    $scheme = $isHttps ? 'https' : 'http';
    return $scheme . '://' . $host;
}
