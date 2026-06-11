<section class="hero">
  <h1 class="hero-title"><?= t('home.title') ?></h1>
  <p class="hero-sub"><?= e(t('home.sub')) ?></p>
  <form class="hero-search" action="<?= e(lurl('/search')) ?>" method="get" role="search">
    <span class="hero-search-icon">
      <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/></svg>
    </span>
    <input name="q" maxlength="100" data-search-input type="text" placeholder="<?= e(t('home.search_placeholder')) ?>" autocomplete="off" enterkeyhint="search" aria-label="<?= e(t('nav.search_label')) ?>">
    <button class="hero-search-btn" type="submit" aria-label="<?= e(t('nav.search_btn')) ?>">
      <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/></svg>
    </button>
    <div class="search-dropdown" data-dropdown></div>
  </form>
</section>

<nav class="filters" aria-label="<?= e(t('home.filters_label')) ?>">
  <a class="filter-pill active" href="<?= e(lurl('/')) ?>"><?= e(t('home.all')) ?></a>
  <?php foreach ($top_categories as $c): ?>
  <a class="filter-pill" href="<?= e(lurl(category_url($c['category']))) ?>"><?= e(cat_label($c['category'])) ?></a>
  <?php endforeach ?>
</nav>

<div class="section-header">
  <h2 class="section-title"><?= e(t('home.trending')) ?></h2>
  <a href="<?= e(lurl('/top')) ?>" class="section-link"><?= e(t('home.view_all')) ?></a>
</div>
<div class="masonry-wrap">
  <div class="masonry" data-home-grid>
    <?php foreach ($trending as $i => $meme): ?>
      <?php partial('partials/card', ['meme' => $meme, 'rank' => $i + 1]) ?>
    <?php endforeach ?>
  </div>
  <?php if ((int) $stats['memes'] > count($trending)): ?>
  <div class="show-more-wrap">
    <a class="btn-show-more" href="<?= e(lurl('/top?page=' . (intdiv(count($trending), PAGE_SIZE) + 1))) ?>"
       data-show-more data-offset="<?= count($trending) ?>"
       data-loading="<?= e(t('home.loading')) ?>">
      <?= e(t('home.show_more')) ?>
      <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="6 9 12 15 18 9"/></svg>
    </a>
  </div>
  <?php endif ?>
</div>

<div class="section-header">
  <h2 class="section-title"><?= e(t('home.browse_categories')) ?></h2>
  <a href="<?= e(lurl('/categories')) ?>" class="section-link"><?= e(t('home.all_categories')) ?></a>
</div>
<div class="categories-row">
  <div class="cat-grid">
    <?php foreach ($top_categories as $c): ?>
    <a class="cat-card" href="<?= e(lurl(category_url($c['category']))) ?>">
      <div class="cat-card-left">
        <div class="cat-card-icon">🎭</div>
        <div class="cat-card-name"><?= e(cat_label($c['category'])) ?></div>
      </div>
      <div class="cat-card-count"><?= e(compact_num((int) $c['n'])) ?></div>
    </a>
    <?php endforeach ?>
  </div>
</div>

<div class="stats-bar">
  <div class="stat">
    <div class="stat-num"><?= e(compact_num((int) $stats['memes'])) ?></div>
    <div class="stat-label"><?= e(t('home.stat_memes')) ?></div>
  </div>
  <div class="stat">
    <div class="stat-num"><?= e(compact_num((int) $stats['categories'])) ?></div>
    <div class="stat-label"><?= e(t('home.stat_categories')) ?></div>
  </div>
  <div class="stat">
    <div class="stat-num"><?= e(compact_num((int) $stats['upvotes'])) ?></div>
    <div class="stat-label"><?= e(t('home.stat_upvotes')) ?></div>
  </div>
  <div class="stat">
    <div class="stat-num">100%</div>
    <div class="stat-label"><?= e(t('home.stat_open')) ?></div>
  </div>
</div>
