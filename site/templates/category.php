<div class="search-results-header">
  <div>
    <h1 class="search-results-title"><?= e(cat_label($category)) ?></h1>
    <span class="search-results-count"><?= (int) $total ?> memes gratis en esta categoría</span>
  </div>
  <a href="/categories" class="section-link">All categories →</a>
</div>

<div class="masonry-wrap">
  <div class="masonry">
    <?php foreach ($memes as $meme): ?>
      <?php partial('partials/card', ['meme' => $meme]) ?>
    <?php endforeach ?>
  </div>
</div>
<?php partial('partials/pagination', ['base' => $base, 'page' => $page, 'total' => $total]) ?>
