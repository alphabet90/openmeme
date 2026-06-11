<?php
/**
 * Front controller. nginx rewrites everything that isn't a real file here.
 *
 * Dev server: php -S 0.0.0.0:8090 -t site/public site/public/index.php
 */

declare(strict_types=1);

require __DIR__ . '/../config.php';
require __DIR__ . '/../src/helpers.php';
require __DIR__ . '/../src/repo.php';

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

// ── Routes ──────────────────────────────────────────────────────

if ($path === '/') {
    $stats = repo_stats();
    render('home', [
        'page_title' => "OpenMeme — The World's Largest Free Meme Stock",
        'meta_description' => 'OpenMeme is the world\'s largest open-source meme image stock. '
            . compact_num((int) $stats['memes']) . ' free memes to download and share. Supporting Argentina & the US.',
        'canonical' => BASE_URL . '/',
        'trending' => repo_trending(20),
        'top_categories' => repo_categories(8),
        'stats' => $stats,
        'is_home' => true,
    ]);
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
        'canonical' => BASE_URL . meme_url($meme),
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
        'page_title' => cat_label($category) . ' Memes — Free Download | OpenMeme',
        'meta_description' => $result['total'] . ' free ' . cat_label($category)
            . ' memes. Open source, free to download and share.',
        'canonical' => BASE_URL . page_link(category_url($category), $page),
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
        'page_title' => 'Browse All Meme Categories | OpenMeme',
        'meta_description' => 'Explore every meme category on OpenMeme. All open source, all free.',
        'canonical' => BASE_URL . '/categories',
        'categories' => repo_categories(),
    ]);
    exit;
}

if ($path === '/search') {
    $q = trim((string) ($_GET['q'] ?? ''));
    $page = max(1, (int) ($_GET['page'] ?? 1));
    $result = repo_search($q, $page);
    render('search', [
        'page_title' => ($q === '' ? 'Search Memes' : 'Memes for "' . $q . '"') . ' | OpenMeme',
        'meta_description' => 'Search ' . compact_num((int) repo_stats()['memes'])
            . ' free open-source memes.',
        'canonical' => BASE_URL . '/search' . ($q === '' ? '' : '?q=' . rawurlencode($q)),
        'robots' => 'noindex, follow',
        'q' => $q,
        'memes' => $result['rows'],
        'total' => $result['total'],
        'page' => $page,
        'base' => '/search?q=' . rawurlencode($q),
    ]);
    exit;
}

if ($path === '/random') {
    $meme = repo_random();
    if ($meme === null) {
        not_found();
    }
    header('Cache-Control: no-store');
    header('Location: ' . meme_url($meme), true, 302);
    exit;
}

if ($path === '/api/suggest') {
    header('Content-Type: application/json; charset=utf-8');
    header('Cache-Control: public, max-age=60');
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
    echo "User-agent: *\nAllow: /\nDisallow: /search\nDisallow: /api/\n\nSitemap: " . BASE_URL . "/sitemap.xml\n";
    exit;
}

not_found();
