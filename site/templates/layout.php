<?php
/** @var string $templateFile */
$stats = repo_stats();
$navCategories = repo_categories(6);
?><!DOCTYPE html>
<html lang="es">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<title><?= e($page_title ?? 'OpenMeme — Free Meme Stock') ?></title>
<meta name="description" content="<?= e($meta_description ?? '') ?>">
<?php if (!empty($robots)): ?>
<meta name="robots" content="<?= e($robots) ?>">
<?php endif ?>
<?php if (!empty($canonical)): ?>
<link rel="canonical" href="<?= e($canonical) ?>">
<?php endif ?>
<meta property="og:title" content="<?= e($page_title ?? 'OpenMeme — Free Meme Stock') ?>">
<meta property="og:description" content="<?= e($meta_description ?? '') ?>">
<meta property="og:type" content="website">
<?php if (!empty($canonical)): ?>
<meta property="og:url" content="<?= e($canonical) ?>">
<?php endif ?>
<?php if (!empty($og_image)): ?>
<meta property="og:image" content="<?= e($og_image) ?>">
<meta name="twitter:card" content="summary_large_image">
<?php endif ?>
<link rel="preconnect" href="https://fonts.googleapis.com">
<link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
<link href="https://fonts.googleapis.com/css2?family=Anton&family=Space+Grotesk:wght@300;400;500;600;700&display=swap" rel="stylesheet">
<link rel="stylesheet" href="<?= e(asset('/assets/app.css')) ?>">
<?php if (!empty($is_home)): ?>
<script type="application/ld+json">
<?= json_encode([
    '@context' => 'https://schema.org',
    '@type' => 'WebSite',
    'name' => 'OpenMeme',
    'url' => BASE_URL . '/',
    'potentialAction' => [
        '@type' => 'SearchAction',
        'target' => ['@type' => 'EntryPoint', 'urlTemplate' => BASE_URL . '/search?q={search_term_string}'],
        'query-input' => 'required name=search_term_string',
    ],
], JSON_UNESCAPED_SLASHES) ?>
</script>
<?php endif ?>
</head>
<body>

<nav class="nav" aria-label="Principal">
  <a href="/" class="nav-logo">OPEN<span class="lime">MEME</span></a>

  <form class="nav-search" action="/search" method="get" role="search">
    <button class="nav-search-submit" type="submit" aria-label="Buscar">
      <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="#6b6b6b" stroke-width="2.5"><circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/></svg>
    </button>
    <input type="search" name="q" data-search-input placeholder="Buscar memes, plantillas…" autocomplete="off" aria-label="Buscar memes" value="<?= e($q ?? '') ?>">
    <div class="search-dropdown" data-dropdown></div>
  </form>

  <div class="nav-links">
    <a class="nav-link<?= !empty($is_home) ? ' active' : '' ?>" href="/">Memes</a>
    <a class="nav-link" href="/categories">Categorías</a>
    <a class="nav-link" href="/random">Aleatorio</a>
    <a class="nav-link" href="https://github.com/alphabet90/openmeme" rel="noopener">GitHub</a>
  </div>

  <button class="nav-icon-btn nav-mobile-only" data-open-msearch aria-label="Buscar memes" type="button">
    <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/></svg>
  </button>
  <button class="nav-mobile-toggle" data-toggle-menu aria-label="Menú" aria-expanded="false" type="button">
    <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="3" y1="6" x2="21" y2="6"/><line x1="3" y1="12" x2="21" y2="12"/><line x1="3" y1="18" x2="21" y2="18"/></svg>
  </button>
</nav>

<div class="nav-backdrop" data-backdrop></div>
<aside class="mobile-drawer" data-drawer role="dialog" aria-label="Menú">
  <button class="nav-drawer-close" data-close-menu aria-label="Cerrar menú" type="button">
    <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
  </button>
  <span class="drawer-logo">OPEN<span class="lime">MEME</span></span>
  <nav class="drawer-links" aria-label="Menú móvil">
    <a class="nav-link" href="/">Memes</a>
    <a class="nav-link" href="/categories">Categorías</a>
    <a class="nav-link" href="/random">Aleatorio</a>
    <?php foreach ($navCategories as $c): ?>
    <a class="nav-link" href="<?= e(category_url($c['category'])) ?>"><?= e(cat_label($c['category'])) ?></a>
    <?php endforeach ?>
  </nav>
</aside>

<div class="mobile-search-overlay" data-msearch role="dialog" aria-label="Buscar memes">
  <form class="msearch-head" action="/search" method="get">
    <button class="msearch-back" data-close-msearch aria-label="Volver" type="button">
      <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><line x1="19" y1="12" x2="5" y2="12"/><polyline points="12 19 5 12 12 5"/></svg>
    </button>
    <div class="msearch-field">
      <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="#6b6b6b" stroke-width="2.5"><circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/></svg>
      <input name="q" data-msearch-input type="text" placeholder="Buscar memes, plantillas, categorías…" autocomplete="off" autocorrect="off" spellcheck="false" enterkeyhint="search">
      <button class="msearch-clear" data-msearch-clear aria-label="Borrar" type="button">
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
      </button>
    </div>
    <button class="msearch-go" type="submit">Buscar</button>
  </form>
  <div class="msearch-body" data-msearch-body></div>
</div>

<main>
<?php require $templateFile; ?>
</main>

<footer class="footer">
  <div class="footer-logo">OPEN<span class="lime">MEME</span></div>
  <div class="footer-links">
    <a class="footer-link" href="/categories">Categories</a>
    <a class="footer-link" href="/random">Random</a>
    <a class="footer-link" href="/sitemap.xml">Sitemap</a>
    <a class="footer-link" href="https://github.com/alphabet90/openmeme" rel="noopener">GitHub</a>
  </div>
  <div class="footer-flags">🇦🇷 🇺🇸</div>
  <div class="footer-copy">© <?= date('Y') ?> OpenMeme. Open source, free forever. <?= e(compact_num((int) $stats['memes'])) ?> memes and counting.</div>
</footer>

<script>
window.OM = <?= json_encode([
    'trending' => array_map(
        fn ($m) => $m['title'],
        array_slice(repo_trending(6), 0, 6)
    ),
    'categories' => array_map(
        fn ($c) => ['name' => cat_label($c['category']), 'slug' => $c['category']],
        $navCategories
    ),
], JSON_UNESCAPED_UNICODE | JSON_UNESCAPED_SLASHES) ?>;
</script>
<script src="<?= e(asset('/assets/app.js')) ?>" defer></script>
</body>
</html>
