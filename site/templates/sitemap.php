<?php
/**
 * Bilingual sitemap: every URL is listed once per locale, each entry
 * carrying hreflang alternates (Google's recommended multilingual format).
 */
$emit = function (string $path, string $extra = '') {
    $variants = [
        'es-AR' => BASE_URL . $path,
        'en-US' => BASE_URL . '/en' . ($path === '/' ? '' : $path),
    ];
    $links = '';
    foreach ($variants as $tag => $href) {
        $links .= '<xhtml:link rel="alternate" hreflang="' . $tag . '" href="' . e($href) . '"/>';
    }
    $links .= '<xhtml:link rel="alternate" hreflang="x-default" href="' . e($variants['es-AR']) . '"/>';
    foreach ($variants as $href) {
        echo '  <url><loc>' . e($href) . '</loc>' . $links . $extra . "</url>\n";
    }
};
echo '<?xml version="1.0" encoding="UTF-8"?>' . "\n";
?>
<urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9" xmlns:xhtml="http://www.w3.org/1999/xhtml">
<?php
$emit('/', '<changefreq>daily</changefreq><priority>1.0</priority>');
$emit('/categories', '<changefreq>weekly</changefreq><priority>0.8</priority>');
$emit('/top', '<changefreq>daily</changefreq><priority>0.8</priority>');
$emit('/nuevos', '<changefreq>daily</changefreq><priority>0.8</priority>');
foreach (['terminos', 'privacidad', 'dmca', 'contacto'] as $p) {
    $emit('/' . $p, '<changefreq>yearly</changefreq><priority>0.3</priority>');
}
foreach ($categories as $c) {
    $emit(category_url($c['category']), '<changefreq>daily</changefreq><priority>0.7</priority>');
}
foreach ($memes as $m) {
    $lastmod = $m['created_at'] !== '' ? '<lastmod>' . e(substr($m['created_at'], 0, 10)) . '</lastmod>' : '';
    $emit('/meme/' . rawurlencode($m['slug']), $lastmod);
}
?>
</urlset>
