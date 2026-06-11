<?php
/** Expects: $base, $page, $total */
$pages = (int) ceil($total / PAGE_SIZE);
if ($pages <= 1) {
    return;
}
?>
<nav class="pagination" aria-label="<?= e(t('pager.label')) ?>">
  <?php if ($page > 1): ?>
  <a class="page-btn" href="<?= e(lurl(page_link($base, $page - 1))) ?>" rel="prev"><?= e(t('pager.prev')) ?></a>
  <?php endif ?>
  <span class="page-info"><?= (int) $page ?> / <?= $pages ?></span>
  <?php if ($page < $pages): ?>
  <a class="page-btn" href="<?= e(lurl(page_link($base, $page + 1))) ?>" rel="next"><?= e(t('pager.next')) ?></a>
  <?php endif ?>
</nav>
