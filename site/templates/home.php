<section class="hero">
  <h1 class="hero-title">THE WORLD'S LARGEST<br>FREE <span class="lime">MEME</span> STOCK</h1>
  <p class="hero-sub">Open source. Totally free. Easy to share and download. Supporting Argentina &amp; the US.</p>
  <form class="hero-search" action="/search" method="get" role="search">
    <span class="hero-search-icon">
      <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/></svg>
    </span>
    <input name="q" data-search-input type="text" placeholder="Probá «doge», «Messi», «gato gracioso»…" autocomplete="off" enterkeyhint="search" aria-label="Buscar memes">
    <button class="hero-search-btn" type="submit" aria-label="Buscar">
      <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/></svg>
    </button>
    <div class="search-dropdown" data-dropdown></div>
  </form>
</section>

<nav class="filters" aria-label="Categorías populares">
  <a class="filter-pill active" href="/">All</a>
  <?php foreach ($top_categories as $c): ?>
  <a class="filter-pill" href="<?= e(category_url($c['category'])) ?>"><?= e(cat_label($c['category'])) ?></a>
  <?php endforeach ?>
</nav>

<div class="section-header">
  <h2 class="section-title">Trending Now</h2>
  <a href="/categories" class="section-link">View all →</a>
</div>
<div class="masonry-wrap">
  <div class="masonry">
    <?php foreach ($trending as $i => $meme): ?>
      <?php partial('partials/card', ['meme' => $meme, 'rank' => $i + 1]) ?>
    <?php endforeach ?>
  </div>
</div>

<div class="section-header">
  <h2 class="section-title">Browse Categories</h2>
  <a href="/categories" class="section-link">All categories →</a>
</div>
<div class="categories-row">
  <div class="cat-grid">
    <?php foreach ($top_categories as $c): ?>
    <a class="cat-card" href="<?= e(category_url($c['category'])) ?>">
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
    <div class="stat-label">Free memes</div>
  </div>
  <div class="stat">
    <div class="stat-num"><?= e(compact_num((int) $stats['categories'])) ?></div>
    <div class="stat-label">Categories</div>
  </div>
  <div class="stat">
    <div class="stat-num"><?= e(compact_num((int) $stats['upvotes'])) ?></div>
    <div class="stat-label">Upvotes</div>
  </div>
  <div class="stat">
    <div class="stat-num">100%</div>
    <div class="stat-label">Open source</div>
  </div>
</div>
