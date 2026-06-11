<?php

declare(strict_types=1);

function e(?string $s): string
{
    return htmlspecialchars((string) $s, ENT_QUOTES, 'UTF-8');
}

/** Render a template into the shared layout. */
function render(string $template, array $vars = []): void
{
    extract($vars, EXTR_SKIP);
    $templateFile = TEMPLATES_DIR . '/' . $template . '.php';
    require TEMPLATES_DIR . '/layout.php';
}

/** Render a template without layout (partials, XML). */
function partial(string $template, array $vars = []): void
{
    extract($vars, EXTR_SKIP);
    require TEMPLATES_DIR . '/' . $template . '.php';
}

function not_found(): never
{
    http_response_code(404);
    render('404', [
        'page_title' => t('notfound.meta_title'),
        'meta_description' => t('notfound.meta_description'),
    ]);
    exit;
}

/** 54231 → "54.2K" */
function compact_num(int $n): string
{
    if ($n >= 1000000) {
        return rtrim(rtrim(number_format($n / 1000000, 1), '0'), '.') . 'M';
    }
    if ($n >= 1000) {
        return rtrim(rtrim(number_format($n / 1000, 1), '0'), '.') . 'K';
    }
    return (string) $n;
}

/** "first-time" → "First Time" */
function cat_label(string $category): string
{
    return ucwords(str_replace('-', ' ', $category));
}

function meme_url(array $meme): string
{
    return '/meme/' . rawurlencode($meme['slug']);
}

function category_url(string $category): string
{
    return '/category/' . rawurlencode($category);
}

/** Public URL of the meme image, served straight out of /memes/* by nginx. */
function meme_img(array $meme): string
{
    return '/memes/' . implode('/', array_map('rawurlencode', explode('/', $meme['image'])));
}

function is_new(array $meme): bool
{
    $ts = strtotime($meme['created_at'] ?? '');
    return $ts !== false && $ts > time() - NEW_DAYS * 86400;
}

/** "2026-01-17T12:22:20Z" → "17 de enero de 2026" */
function fecha_es(string $iso): string
{
    $ts = strtotime($iso);
    if ($ts === false) {
        return '';
    }
    $meses = [
        1 => 'enero', 'febrero', 'marzo', 'abril', 'mayo', 'junio',
        'julio', 'agosto', 'septiembre', 'octubre', 'noviembre', 'diciembre',
    ];
    return sprintf('%02d de %s de %d', (int) date('j', $ts), $meses[(int) date('n', $ts)], (int) date('Y', $ts));
}

/** Cache-busted asset URL based on file mtime. */
function asset(string $path): string
{
    $file = SITE_ROOT . '/public' . $path;
    $v = is_file($file) ? (string) filemtime($file) : '0';
    return $path . '?v=' . $v;
}

function page_link(string $base, int $page): string
{
    $sep = str_contains($base, '?') ? '&' : '?';
    return $page <= 1 ? $base : $base . $sep . 'page=' . $page;
}
