<?php
/** Expects: $base, $page, $total */
$pages = (int) ceil($total / PAGE_SIZE);
if ($pages <= 1) {
    return;
}
?>
<nav class="pagination" aria-label="Paginación">
  <?php if ($page > 1): ?>
  <a class="page-btn" href="<?= e(page_link($base, $page - 1)) ?>" rel="prev">← Anterior</a>
  <?php endif ?>
  <span class="page-info"><?= (int) $page ?> / <?= $pages ?></span>
  <?php if ($page < $pages): ?>
  <a class="page-btn" href="<?= e(page_link($base, $page + 1)) ?>" rel="next">Siguiente →</a>
  <?php endif ?>
</nav>
