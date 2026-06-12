<div class="search-results-header">
  <div>
    <h1 class="search-results-title"><?= e(cat_label($category)) ?></h1>
    <span class="search-results-count"><?= e(t('category.count', (int) $total)) ?></span>
  </div>
  <a href="<?= e(lurl('/categories')) ?>" class="section-link"><?= e(t('category.all')) ?></a>
</div>

<div class="masonry-wrap">
  <div class="masonry">
    <?php foreach ($memes as $i => $meme): ?>
      <?php partial('partials/card', ['meme' => $meme, 'index' => $i]) ?>
    <?php endforeach ?>
  </div>
</div>
<?php partial('partials/pagination', ['base' => $base, 'page' => $page, 'total' => $total]) ?>
