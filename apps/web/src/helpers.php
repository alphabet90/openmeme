<?php

declare(strict_types=1);

function e(?string $s): string
{
    return htmlspecialchars((string) $s, ENT_QUOTES, 'UTF-8');
}

/** Per-request CSP nonce for the layout's inline <script>/<style> blocks. */
function csp_nonce(): string
{
    static $nonce = null;
    return $nonce ??= base64_encode(random_bytes(16));
}

/**
 * Content-Security-Policy for dynamic responses. Inline scripts/styles must
 * carry csp_nonce(); the only third parties are Google Fonts and CDN_URL.
 */
function csp_policy(): string
{
    $nonce = "'nonce-" . csp_nonce() . "'";
    $img = "'self'" . (CDN_URL !== '' ? ' ' . CDN_URL : '');
    $directives = [
        "default-src 'self'",
        "script-src 'self' $nonce",
        "style-src 'self' $nonce https://fonts.googleapis.com",
        "font-src 'self' https://fonts.gstatic.com",
        "img-src $img",
        "connect-src 'self'",
        "object-src 'none'",
        "base-uri 'self'",
        "form-action 'self'",
        "frame-ancestors 'self'",
    ];
    if (is_https()) {
        $directives[] = 'upgrade-insecure-requests';
    }
    return implode('; ', $directives);
}

/**
 * OWASP-recommended security headers for every dynamic response.
 * nginx adds its own minimal set on static-asset locations (see nginx.conf);
 * keep the two in sync rather than duplicating headers across layers.
 */
function send_security_headers(): void
{
    header_remove('X-Powered-By');
    header('Content-Security-Policy: ' . csp_policy());
    header('X-Content-Type-Options: nosniff');
    header('X-Frame-Options: SAMEORIGIN');
    header('Referrer-Policy: strict-origin-when-cross-origin');
    header('X-XSS-Protection: 0');
    header('Permissions-Policy: camera=(), geolocation=(), microphone=(), payment=(), usb=()');
    header('Cross-Origin-Opener-Policy: same-origin');
    header('Cross-Origin-Resource-Policy: same-origin');
    // Browsers ignore HSTS over plain HTTP, so only send it when the request
    // arrived via TLS (directly or at the proxy in front of us).
    if (is_https()) {
        header('Strict-Transport-Security: max-age=31536000; includeSubDomains');
    }
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

/**
 * Image URL for <img> src: CDN host when configured, else same-origin.
 * Download/copy links must keep using meme_img() — the download attribute
 * is ignored on cross-origin anchors.
 */
function meme_img_src(array $meme): string
{
    return CDN_URL !== '' ? CDN_URL . meme_img($meme) : meme_img($meme);
}

/** Absolute image URL for OG tags / JSON-LD. */
function meme_img_abs(array $meme): string
{
    return CDN_URL !== '' ? CDN_URL . meme_img($meme) : BASE_URL . meme_img($meme);
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
