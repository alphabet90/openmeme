<section class="hero hero-compact">
  <h1 class="hero-title hero-title-sm"><?= t('categories.title') ?></h1>
  <p class="hero-sub"><?= e(t('categories.sub')) ?></p>
</section>

<div class="categories-row">
  <div class="cat-grid">
    <?php foreach ($categories as $c): ?>
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
