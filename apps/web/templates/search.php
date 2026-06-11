<div class="search-results-header">
  <div>
    <h1 class="search-results-title"><?= $q === '' ? e(t('search.title')) : e(t('search.results_for', $q)) ?></h1>
    <span class="search-results-count"><?= e(t('search.count', (int) $total)) ?></span>
  </div>
</div>

<?php if (empty($memes)): ?>
<div class="empty-state">
  <?php if ($q === ''): ?>
  <p><?= e(t('search.empty_prompt')) ?></p>
  <?php else: ?>
  <p><?= e(t('search.no_results', $q)) ?> <a href="<?= e(lurl('/categories')) ?>"><?= e(t('search.browse_link')) ?></a>.</p>
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
