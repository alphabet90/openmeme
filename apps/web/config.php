<?php
/**
 * OpenMeme site configuration.
 * Single source of truth for paths. No framework, no magic.
 */

declare(strict_types=1);

define('SITE_ROOT', __DIR__);
define('REPO_ROOT', dirname(__DIR__, 2));

// /memes/* is the source of truth. The SQLite DB is a disposable index of it.
define('MEMES_DIR', REPO_ROOT . '/memes');
define('DB_PATH', SITE_ROOT . '/data/memes.db');

define('TEMPLATES_DIR', SITE_ROOT . '/templates');

// Default page size everywhere: home grid, listings, search,
// and the "show more" batch size.
define('PAGE_SIZE', 100);

// Memes newer than this many days get the NEW badge
define('NEW_DAYS', 7);

// Canonical base URL for sitemap/OG tags. Override in production via env.
define('BASE_URL', rtrim(getenv('OPENMEME_BASE_URL') ?: detect_base_url(), '/'));

function detect_base_url(): string
{
    $host = $_SERVER['HTTP_HOST'] ?? 'localhost:8090';
    $isHttps = (!empty($_SERVER['HTTPS']) && $_SERVER['HTTPS'] !== 'off')
        || ($_SERVER['HTTP_X_FORWARDED_PROTO'] ?? '') === 'https';
    $scheme = $isHttps ? 'https' : 'http';
    return $scheme . '://' . $host;
}
