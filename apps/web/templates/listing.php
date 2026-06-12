<?php /** Expects: $heading, $subtitle, $memes, $total, $page, $base */ ?>
<div class="search-results-header">
  <div>
    <h1 class="search-results-title"><?= e($heading) ?></h1>
    <span class="search-results-count"><?= e($subtitle) ?></span>
  </div>
</div>

<div class="masonry-wrap">
  <div class="masonry">
    <?php foreach ($memes as $i => $meme): ?>
      <?php partial('partials/card', ['meme' => $meme, 'index' => $i]) ?>
    <?php endforeach ?>
  </div>
</div>
<?php partial('partials/pagination', ['base' => $base, 'page' => $page, 'total' => $total]) ?>
