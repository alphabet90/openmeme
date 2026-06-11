<?php
$w = (int) $meme['width'];
$h = (int) $meme['height'];
$tags = preg_split('/\s+/', (string) $meme['tags'], -1, PREG_SPLIT_NO_EMPTY);
$fecha = format_date((string) $meme['created_at']);
?>
<div class="meme-main">
<nav class="breadcrumbs" aria-label="Breadcrumb">
  <ol>
    <li><a href="<?= e(lurl('/')) ?>"><?= e(t('meme.home')) ?></a></li>
    <li><a href="<?= e(lurl('/categories')) ?>"><?= e(t('meme.categories')) ?></a></li>
    <li><a href="<?= e(lurl(category_url($meme['category']))) ?>"><?= e(cat_label($meme['category'])) ?></a></li>
    <li><span aria-current="page"><?= e($meme['title']) ?></span></li>
  </ol>
</nav>

<article class="meme-hero">
  <figure class="meme-figure">
    <img src="<?= e(meme_img($meme)) ?>" alt="<?= e($meme['title']) ?>"
      <?= $w > 0 ? 'width="' . $w . '" height="' . $h . '"' : '' ?> decoding="async" fetchpriority="high">
  </figure>

  <div class="meme-info">
    <h1 class="meme-title"><?= e($meme['title']) ?></h1>

    <div class="meme-byline">
      <?php if ($meme['author'] !== ''): ?>
      <span class="byline-avatar" aria-hidden="true"><?= e(mb_strtoupper(mb_substr($meme['author'], 0, 1))) ?></span>
      <span class="byline-author">u/<?= e($meme['author']) ?></span>
      <?php endif ?>
      <?php if ($meme['subreddit'] !== ''): ?>
      <span class="byline-sep" aria-hidden="true">·</span>
      <a class="byline-sub" href="<?= e($meme['post_url']) ?>" rel="nofollow noopener ugc">#<?= e($meme['subreddit']) ?></a>
      <?php endif ?>
      <?php if ($fecha !== ''): ?>
      <span class="byline-sep" aria-hidden="true">·</span>
      <span class="byline-date">
        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="4" width="18" height="18" rx="2"/><line x1="16" y1="2" x2="16" y2="6"/><line x1="8" y1="2" x2="8" y2="6"/><line x1="3" y1="10" x2="21" y2="10"/></svg>
        <?= e($fecha) ?>
      </span>
      <?php endif ?>
    </div>

<?php if ($meme['description'] !== ''): ?>
    <p class="meme-desc"><?= e($meme['description']) ?></p>
<?php endif ?>
<?php if ($tags): ?>
    <div class="meme-tags">
      <?php foreach ($tags as $tag): ?>
      <a class="tag-pill" href="<?= e(lurl('/search?q=' . rawurlencode($tag))) ?>">#<?= e($tag) ?></a>
      <?php endforeach ?>
    </div>
<?php endif ?>

    <div class="meme-actions">
      <a class="btn-primary" href="<?= e(meme_img($meme)) ?>" download="<?= e(basename($meme['image'])) ?>">
        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><path d="M21 15v4a2 2 0 01-2 2H5a2 2 0 01-2-2v-4"/><polyline points="7 10 12 15 17 10"/><line x1="12" y1="15" x2="12" y2="3"/></svg>
        <?= e(t('meme.download')) ?>
      </a>
      <button class="btn-secondary" data-copy="<?= e(BASE_URL . lurl(meme_url($meme))) ?>" type="button">
        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="9" y="9" width="13" height="13" rx="2"/><path d="M5 15H4a2 2 0 01-2-2V4a2 2 0 012-2h9a2 2 0 012 2v1"/></svg>
        <?= e(t('meme.copy')) ?>
      </button>
    </div>
  </div>
</article>
</div>

<?php if (!empty($related)): ?>
<div class="section-header">
  <h2 class="section-title"><?= e(t('meme.more', cat_label($meme['category']))) ?></h2>
  <a href="<?= e(lurl(category_url($meme['category']))) ?>" class="section-link"><?= e(t('meme.view_all')) ?></a>
</div>
<div class="masonry-wrap">
  <div class="masonry">
    <?php foreach ($related as $rel): ?>
      <?php partial('partials/card', ['meme' => $rel]) ?>
    <?php endforeach ?>
  </div>
</div>
<?php endif ?>

<script type="application/ld+json">
<?= json_encode([
    '@context' => 'https://schema.org',
    '@type' => 'ImageObject',
    'contentUrl' => BASE_URL . meme_img($meme),
    'url' => BASE_URL . lurl(meme_url($meme)),
    'name' => $meme['title'],
    'description' => $meme['description'],
    'inLanguage' => locale_tag(),
    'width' => $w,
    'height' => $h,
    'datePublished' => $meme['created_at'],
    'author' => ['@type' => 'Person', 'name' => $meme['author']],
    'acquireLicensePage' => BASE_URL . lurl('/'),
    'creditText' => 'OpenMeme',
], JSON_UNESCAPED_SLASHES | JSON_UNESCAPED_UNICODE) ?>
</script>
