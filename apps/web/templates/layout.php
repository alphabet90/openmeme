<?php
/** @var string $templateFile */
$stats = repo_stats();
$navCategories = repo_categories(6);
?><!DOCTYPE html>
<html lang="<?= e(locale_tag()) ?>">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<title><?= e($page_title ?? 'OpenMeme') ?></title>
<meta name="description" content="<?= e($meta_description ?? '') ?>">
<?php if (!empty($robots)): ?>
<meta name="robots" content="<?= e($robots) ?>">
<?php endif ?>
<?php if (!empty($canonical)): ?>
<link rel="canonical" href="<?= e($canonical) ?>">
<?php endif ?>
<?php foreach ($alternates ?? [] as $hreflang => $href): ?>
<link rel="alternate" hreflang="<?= e($hreflang) ?>" href="<?= e($href) ?>">
<?php endforeach ?>
<meta property="og:title" content="<?= e($page_title ?? 'OpenMeme') ?>">
<meta property="og:description" content="<?= e($meta_description ?? '') ?>">
<meta property="og:type" content="website">
<meta property="og:locale" content="<?= e(str_replace('-', '_', locale_tag())) ?>">
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
    'url' => BASE_URL . lurl('/'),
    'inLanguage' => locale_tag(),
    'potentialAction' => [
        '@type' => 'SearchAction',
        'target' => ['@type' => 'EntryPoint', 'urlTemplate' => BASE_URL . lurl('/search') . '?q={search_term_string}'],
        'query-input' => 'required name=search_term_string',
    ],
], JSON_UNESCAPED_SLASHES) ?>
</script>
<?php endif ?>
</head>
<body>

<nav class="nav" aria-label="Principal">
  <a href="<?= e(lurl('/')) ?>" class="nav-logo">OPEN<span class="lime">MEME</span></a>

  <form class="nav-search" action="<?= e(lurl('/search')) ?>" method="get" role="search">
    <button class="nav-search-submit" type="submit" aria-label="<?= e(t('nav.search_btn')) ?>">
      <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="#6b6b6b" stroke-width="2.5"><circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/></svg>
    </button>
    <input type="search" name="q" maxlength="100" data-search-input placeholder="<?= e(t('nav.search_placeholder')) ?>" autocomplete="off" aria-label="<?= e(t('nav.search_label')) ?>" value="<?= e($q ?? '') ?>">
    <div class="search-dropdown" data-dropdown></div>
  </form>

  <div class="nav-links">
    <a class="nav-link<?= !empty($is_home) ? ' active' : '' ?>" href="<?= e(lurl('/')) ?>"><?= e(t('nav.memes')) ?></a>
    <a class="nav-link" href="<?= e(lurl('/categories')) ?>"><?= e(t('nav.categories')) ?></a>
    <a class="nav-link" href="<?= e(lurl('/top')) ?>"><?= e(t('nav.top')) ?></a>
    <a class="nav-link" href="<?= e(lurl('/nuevos')) ?>"><?= e(t('nav.new')) ?></a>
    <a class="nav-link" href="<?= e(lurl('/random')) ?>"><?= e(t('nav.random')) ?></a>
  </div>

  <nav class="lang-switch" aria-label="<?= e(t('lang.switch_label')) ?>">
    <a class="lang-link<?= LOCALE === 'es' ? ' active' : '' ?>" href="<?= e(REQUEST_PATH) ?>" hreflang="es-AR" lang="es-AR">AR</a>
    <span class="lang-sep" aria-hidden="true">/</span>
    <a class="lang-link<?= LOCALE === 'en' ? ' active' : '' ?>" href="<?= e(REQUEST_PATH === '/' ? '/en' : '/en' . REQUEST_PATH) ?>" hreflang="en-US" lang="en-US">US</a>
  </nav>

  <button class="nav-icon-btn nav-mobile-only" data-open-msearch aria-label="<?= e(t('nav.search_label')) ?>" type="button">
    <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/></svg>
  </button>
  <button class="nav-mobile-toggle" data-toggle-menu aria-label="<?= e(t('nav.menu')) ?>" aria-expanded="false" type="button">
    <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="3" y1="6" x2="21" y2="6"/><line x1="3" y1="12" x2="21" y2="12"/><line x1="3" y1="18" x2="21" y2="18"/></svg>
  </button>
</nav>

<div class="nav-backdrop" data-backdrop></div>
<aside class="mobile-drawer" data-drawer role="dialog" aria-label="<?= e(t('nav.menu')) ?>">
  <button class="nav-drawer-close" data-close-menu aria-label="<?= e(t('nav.close_menu')) ?>" type="button">
    <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
  </button>
  <span class="drawer-logo">OPEN<span class="lime">MEME</span></span>
  <nav class="drawer-links" aria-label="<?= e(t('nav.drawer_label')) ?>">
    <a class="nav-link" href="<?= e(lurl('/')) ?>"><?= e(t('nav.memes')) ?></a>
    <a class="nav-link" href="<?= e(lurl('/categories')) ?>"><?= e(t('nav.categories')) ?></a>
    <a class="nav-link" href="<?= e(lurl('/top')) ?>"><?= e(t('nav.top')) ?></a>
    <a class="nav-link" href="<?= e(lurl('/nuevos')) ?>"><?= e(t('nav.new')) ?></a>
    <a class="nav-link" href="<?= e(lurl('/random')) ?>"><?= e(t('nav.random')) ?></a>
    <?php foreach ($navCategories as $c): ?>
    <a class="nav-link" href="<?= e(lurl(category_url($c['category']))) ?>"><?= e(cat_label($c['category'])) ?></a>
    <?php endforeach ?>
  </nav>
  <nav class="lang-switch drawer-lang" aria-label="<?= e(t('lang.switch_label_drawer')) ?>">
    <a class="lang-link<?= LOCALE === 'es' ? ' active' : '' ?>" href="<?= e(REQUEST_PATH) ?>" hreflang="es-AR" lang="es-AR">🇦🇷 Argentina</a>
    <a class="lang-link<?= LOCALE === 'en' ? ' active' : '' ?>" href="<?= e(REQUEST_PATH === '/' ? '/en' : '/en' . REQUEST_PATH) ?>" hreflang="en-US" lang="en-US">🇺🇸 United States</a>
  </nav>
</aside>

<div class="mobile-search-overlay" data-msearch role="dialog" aria-label="<?= e(t('nav.search_label')) ?>">
  <form class="msearch-head" action="<?= e(lurl('/search')) ?>" method="get">
    <button class="msearch-back" data-close-msearch aria-label="<?= e(t('nav.back')) ?>" type="button">
      <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><line x1="19" y1="12" x2="5" y2="12"/><polyline points="12 19 5 12 12 5"/></svg>
    </button>
    <div class="msearch-field">
      <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="#6b6b6b" stroke-width="2.5"><circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/></svg>
      <input name="q" maxlength="100" data-msearch-input type="text" placeholder="<?= e(t('msearch.placeholder')) ?>" autocomplete="off" autocorrect="off" spellcheck="false" enterkeyhint="search">
      <button class="msearch-clear" data-msearch-clear aria-label="<?= e(t('nav.clear')) ?>" type="button">
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
      </button>
    </div>
    <button class="msearch-go" type="submit"><?= e(t('nav.search_btn')) ?></button>
  </form>
  <div class="msearch-body" data-msearch-body></div>
</div>

<main>
<?php require $templateFile; ?>
</main>

<footer class="footer">
  <div class="footer-grid">
    <div>
      <p class="footer-brand">OPEN<span class="lime">MEME</span></p>
      <p class="footer-tagline"><?= e(t('footer.tagline')) ?></p>
    </div>

    <nav aria-label="<?= e(t('footer.nav_label')) ?>">
      <h3 class="footer-col-title"><?= e(t('footer.explore')) ?></h3>
      <ul>
        <li><a href="<?= e(lurl('/')) ?>"><?= e(t('footer.home')) ?></a></li>
        <li><a href="<?= e(lurl('/top')) ?>"><?= e(t('nav.top')) ?></a></li>
        <li><a href="<?= e(lurl('/nuevos')) ?>"><?= e(t('nav.new')) ?></a></li>
        <li><a href="<?= e(lurl('/random')) ?>"><?= e(t('nav.random')) ?></a></li>
        <li><a href="<?= e(lurl('/categories')) ?>"><?= e(t('nav.categories')) ?></a></li>
      </ul>
    </nav>

    <nav aria-label="<?= e(t('footer.categories_label')) ?>">
      <h3 class="footer-col-title"><?= e(t('footer.categories')) ?></h3>
      <ul>
        <?php foreach (array_slice($navCategories, 0, 5) as $c): ?>
        <li><a href="<?= e(lurl(category_url($c['category']))) ?>"><?= e(cat_label($c['category'])) ?></a></li>
        <?php endforeach ?>
      </ul>
    </nav>

    <nav aria-label="<?= e(t('footer.legal_label')) ?>">
      <h3 class="footer-col-title"><?= e(t('footer.legal')) ?></h3>
      <ul>
        <li><a href="<?= e(lurl('/terminos')) ?>"><?= e(t('footer.terms')) ?></a></li>
        <li><a href="<?= e(lurl('/privacidad')) ?>"><?= e(t('footer.privacy')) ?></a></li>
        <li><a href="<?= e(lurl('/dmca')) ?>">DMCA</a></li>
        <li><a href="<?= e(lurl('/contacto')) ?>"><?= e(t('footer.contact')) ?></a></li>
      </ul>
    </nav>
  </div>

  <div class="footer-bottom">
    <small>© <?= date('Y') ?> OpenMeme. <?= e(t('footer.copyright')) ?></small>
  </div>
</footer>

<script>
window.OM = <?= json_encode([
    'prefix' => LOCALES[LOCALE]['prefix'],
    'categories' => array_map(
        fn ($c) => ['name' => cat_label($c['category']), 'slug' => $c['category']],
        $navCategories
    ),
    'i18n' => [
        'recents' => t('js.recents'),
        'clear' => t('js.clear'),
        'explore' => t('js.explore'),
        'no_suggestions' => t('js.no_suggestions', '%s'),
        'press_enter' => t('js.press_enter'),
        'remove' => t('js.remove'),
    ],
], JSON_UNESCAPED_UNICODE | JSON_UNESCAPED_SLASHES) ?>;
</script>
<script src="<?= e(asset('/assets/app.js')) ?>" defer></script>
</body>
</html>
