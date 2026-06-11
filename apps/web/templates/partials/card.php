<?php
/**
 * Meme card. Expects: $meme (row), optional $rank (1-3).
 * width/height come from the index so the masonry never reflows (zero CLS).
 */
$w = (int) $meme['width'];
$h = (int) $meme['height'];
?>
<article class="card">
  <?php if (!empty($rank) && $rank <= 3): ?>
  <span class="card-rank rank-<?= (int) $rank ?>"><?= (int) $rank ?></span>
  <?php elseif (is_new($meme)): ?>
  <span class="card-badge badge-nuevo"><?= e(t('card.new')) ?></span>
  <?php endif ?>
  <a class="card-link" href="<?= e(lurl(meme_url($meme))) ?>" aria-label="<?= e($meme['title']) ?>">
    <img class="card-img" src="<?= e(meme_img($meme)) ?>" alt="<?= e($meme['title']) ?>"
      <?= $w > 0 ? 'width="' . $w . '" height="' . $h . '"' : '' ?> loading="lazy" decoding="async">
  </a>
  <div class="card-overlay"></div>
  <span class="card-views">
    <svg width="10" height="10" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><path d="M12 2l2.4 7.2H22l-6 4.6 2.3 7.2-6.3-4.5-6.3 4.5L8 13.8 2 9.2h7.6z"/></svg>
    <?= e(compact_num((int) $meme['score'])) ?>
  </span>
  <div class="card-actions">
    <button class="card-action" title="<?= e(t('meme.copy_link')) ?>" aria-label="<?= e(t('meme.copy_link')) ?>" data-copy-img="<?= e(meme_img($meme)) ?>" data-copy="<?= e(BASE_URL . lurl(meme_url($meme))) ?>" type="button">
      <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><rect x="9" y="9" width="13" height="13" rx="2"/><path d="M5 15H4a2 2 0 01-2-2V4a2 2 0 012-2h9a2 2 0 012 2v1"/></svg>
    </button>
    <a class="card-action" title="<?= e(t('meme.download')) ?>" aria-label="<?= e(t('meme.download_label', $meme['title'])) ?>" href="<?= e(meme_img($meme)) ?>" download="<?= e(basename($meme['image'])) ?>">
      <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><path d="M21 15v4a2 2 0 01-2 2H5a2 2 0 01-2-2v-4"/><polyline points="7 10 12 15 17 10"/><line x1="12" y1="15" x2="12" y2="3"/></svg>
    </a>
  </div>
</article>
