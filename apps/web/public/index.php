<?php
/**
 * Front controller. nginx rewrites everything that isn't a real file here.
 *
 * Locales: es-AR (default, at /) and en-US (at /en/...).
 *
 * Dev server: php -S 0.0.0.0:8090 -t site/public site/public/index.php
 */

declare(strict_types=1);

require __DIR__ . '/../config.php';
require __DIR__ . '/../src/helpers.php';
require __DIR__ . '/../src/i18n.php';
require __DIR__ . '/../src/repo.php';
require __DIR__ . '/../src/pages.php';

$path = rawurldecode((string) parse_url($_SERVER['REQUEST_URI'], PHP_URL_PATH));

// PHP built-in dev server: serve static files and /memes/* images directly.
if (PHP_SAPI === 'cli-server') {
    $file = __DIR__ . $path;
    if ($path !== '/' && is_file($file)) {
        return false;
    }
    if (preg_match('#^/memes/([a-z0-9-]+)/([^/]+\.(?:jpe?g|png|gif|webp))$#i', $path, $m)) {
        $img = MEMES_DIR . '/' . $m[1] . '/' . $m[2];
        if (is_file($img)) {
            header('Content-Type: ' . (mime_content_type($img) ?: 'application/octet-stream'));
            header('Cache-Control: public, max-age=31536000, immutable');
            readfile($img);
            exit;
        }
    }
}

// ── Locale detection: /en[/...] → en-US, everything else → es-AR ──
if ($path === '/en' || str_starts_with($path, '/en/')) {
    define('LOCALE', 'en');
    $path = substr($path, 3);
    if ($path === '') {
        $path = '/';
    }
} else {
    define('LOCALE', 'es');
}

// Unprefixed path + query, used by the language switcher and hreflang.
$query = (string) ($_SERVER['QUERY_STRING'] ?? '');
define('REQUEST_PATH', $path . ($query === '' ? '' : '?' . $query));

// ── Routes ──────────────────────────────────────────────────────

if ($path === '/') {
    $stats = repo_stats();
    render('home', [
        'page_title' => t('home.meta_title'),
        'meta_description' => t('home.meta_description', compact_num((int) $stats['memes'])),
        'canonical' => BASE_URL . lurl('/'),
        'alternates' => alternates('/'),
        'trending' => repo_trending(PAGE_SIZE),
        'top_categories' => repo_categories(8),
        'stats' => $stats,
        'is_home' => true,
    ]);
    exit;
}

// HTML fragment of the next batch of cards, appended by the home "show more" button.
if ($path === '/api/memes') {
    $offset = max(0, min(10000, (int) ($_GET['offset'] ?? 0)));
    header('Content-Type: text/html; charset=utf-8');
    header('Cache-Control: public, max-age=300');
    foreach (repo_trending(PAGE_SIZE, $offset) as $meme) {
        partial('partials/card', ['meme' => $meme]);
    }
    exit;
}

if (preg_match('#^/meme/([a-z0-9-]+)$#', $path, $m)) {
    $meme = repo_meme($m[1]);
    if ($meme === null) {
        not_found();
    }
    render('meme', [
        'page_title' => mb_substr($meme['title'], 0, 58) . ' | OpenMeme',
        'meta_description' => mb_substr((string) $meme['description'], 0, 160),
        'canonical' => BASE_URL . lurl(meme_url($meme)),
        'alternates' => alternates(meme_url($meme)),
        'og_image' => BASE_URL . meme_img($meme),
        'meme' => $meme,
        'related' => repo_related($meme),
    ]);
    exit;
}

if (preg_match('#^/category/([a-z0-9-]+)$#', $path, $m)) {
    $category = $m[1];
    $page = max(1, (int) ($_GET['page'] ?? 1));
    $result = repo_category_page($category, $page);
    if ($result['total'] === 0 || ($page > 1 && empty($result['rows']))) {
        not_found();
    }
    render('category', [
        'page_title' => t('category.meta_title', cat_label($category)),
        'meta_description' => t('category.meta_description', $result['total'], cat_label($category)),
        'canonical' => BASE_URL . lurl(page_link(category_url($category), $page)),
        'alternates' => alternates(page_link(category_url($category), $page)),
        'category' => $category,
        'memes' => $result['rows'],
        'total' => $result['total'],
        'page' => $page,
        'base' => category_url($category),
    ]);
    exit;
}

if ($path === '/categories') {
    render('categories', [
        'page_title' => t('categories.meta_title'),
        'meta_description' => t('categories.meta_description'),
        'canonical' => BASE_URL . lurl('/categories'),
        'alternates' => alternates('/categories'),
        'categories' => repo_categories(),
    ]);
    exit;
}

if ($path === '/search') {
    $q = mb_substr(trim((string) ($_GET['q'] ?? '')), 0, MAX_QUERY_LENGTH);
    $page = max(1, (int) ($_GET['page'] ?? 1));
    $search_error = false;
    try {
        $result = repo_search($q, $page);
    } catch (SearchUnavailableException) {
        $search_error = true;
        $result = ['rows' => [], 'total' => 0];
        http_response_code(503);
        header('Retry-After: 60');
        header('Cache-Control: no-store');
    }
    render('search', [
        'search_error' => $search_error,
        'page_title' => $q === '' ? t('search.meta_title_empty') : t('search.meta_title', $q),
        'meta_description' => t('search.meta_description', compact_num((int) repo_stats()['memes'])),
        'canonical' => BASE_URL . lurl('/search' . ($q === '' ? '' : '?q=' . rawurlencode($q))),
        'alternates' => alternates('/search' . ($q === '' ? '' : '?q=' . rawurlencode($q))),
        'robots' => 'noindex, follow',
        'q' => $q,
        'memes' => $result['rows'],
        'total' => $result['total'],
        'page' => $page,
        'base' => '/search?q=' . rawurlencode($q),
    ]);
    exit;
}

if ($path === '/top' || $path === '/nuevos') {
    $order = $path === '/top' ? 'top' : 'new';
    $page = max(1, (int) ($_GET['page'] ?? 1));
    $result = repo_list($order, $page);
    if ($page > 1 && empty($result['rows'])) {
        not_found();
    }
    $ns = $order === 'top' ? 'top' : 'new';
    render('listing', [
        'page_title' => t("$ns.meta_title"),
        'meta_description' => t("$ns.meta_description"),
        'canonical' => BASE_URL . lurl(page_link($path, $page)),
        'alternates' => alternates(page_link($path, $page)),
        'heading' => t("$ns.heading"),
        'subtitle' => $order === 'top' ? t('top.subtitle', $result['total']) : t('new.subtitle'),
        'memes' => $result['rows'],
        'total' => $result['total'],
        'page' => $page,
        'base' => $path,
    ]);
    exit;
}

$staticPages = [
    '/terminos' => ['legal', 'page_terminos'],
    '/privacidad' => ['legal', 'page_privacidad'],
    '/dmca' => ['legal', 'page_dmca'],
    '/contacto' => ['contact', 'page_contacto'],
];
if (isset($staticPages[$path])) {
    [$template, $contentFn] = $staticPages[$path];
    $content = $contentFn();
    render($template, [
        'page_title' => $content['meta_title'],
        'meta_description' => $content['meta_description'],
        'canonical' => BASE_URL . lurl($path),
        'alternates' => alternates($path),
        'content' => $content,
    ]);
    exit;
}

if ($path === '/random') {
    $meme = repo_random();
    if ($meme === null) {
        not_found();
    }
    header('Cache-Control: no-store');
    header('Location: ' . lurl(meme_url($meme)), true, 302);
    exit;
}

if ($path === '/api/suggest') {
    header('Content-Type: application/json; charset=utf-8');
    header('Cache-Control: public, max-age=60');
    header('X-Content-Type-Options: nosniff');
    echo json_encode(repo_suggest((string) ($_GET['q'] ?? '')), JSON_UNESCAPED_UNICODE);
    exit;
}

if ($path === '/sitemap.xml') {
    header('Content-Type: application/xml; charset=utf-8');
    partial('sitemap', [
        'memes' => db()->query('SELECT slug, created_at FROM memes ORDER BY score DESC')->fetchAll(),
        'categories' => repo_categories(),
    ]);
    exit;
}

if ($path === '/robots.txt') {
    header('Content-Type: text/plain; charset=utf-8');
    echo "User-agent: *\nAllow: /\nDisallow: /search\nDisallow: /en/search\nDisallow: /api/\nDisallow: /en/api/\n\nSitemap: " . BASE_URL . "/sitemap.xml\n";
    exit;
}

not_found();
