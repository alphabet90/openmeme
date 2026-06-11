<div class="search-results-header">
  <div>
    <h1 class="search-results-title"><?= $q === '' ? 'Buscar memes' : 'Resultados: «' . e($q) . '»' ?></h1>
    <span class="search-results-count"><?= (int) $total ?> memes encontrados</span>
  </div>
</div>

<?php if (empty($memes)): ?>
<div class="empty-state">
  <?php if ($q === ''): ?>
  <p>Escribí algo para buscar entre miles de memes gratis.</p>
  <?php else: ?>
  <p>Sin resultados para «<?= e($q) ?>». Probá con otra palabra o <a href="/categories">explorá las categorías</a>.</p>
  <?php endif ?>
</div>
<?php else: ?>
<div class="masonry-wrap">
  <div class="masonry">
    <?php foreach ($memes as $meme): ?>
      <?php partial('partials/card', ['meme' => $meme]) ?>
    <?php endforeach ?>
  </div>
</div>
<?php partial('partials/pagination', ['base' => $base, 'page' => $page, 'total' => $total]) ?>
<?php endif ?>
