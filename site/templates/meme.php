<?php
$w = (int) $meme['width'];
$h = (int) $meme['height'];
$tags = preg_split('/\s+/', (string) $meme['tags'], -1, PREG_SPLIT_NO_EMPTY);
?>
<article class="meme-page">
  <nav class="breadcrumb" aria-label="Breadcrumb">
    <a href="/">Memes</a> <span>/</span>
    <a href="<?= e(category_url($meme['category'])) ?>"><?= e(cat_label($meme['category'])) ?></a>
  </nav>

  <h1 class="meme-title"><?= e($meme['title']) ?></h1>

  <figure class="meme-figure">
    <img src="<?= e(meme_img($meme)) ?>" alt="<?= e($meme['title']) ?>"
      <?= $w > 0 ? 'width="' . $w . '" height="' . $h . '"' : '' ?> decoding="async" fetchpriority="high">
    <figcaption><?= e($meme['description']) ?></figcaption>
  </figure>

  <div class="meme-actions">
    <a class="btn-primary" href="<?= e(meme_img($meme)) ?>" download="<?= e(basename($meme['image'])) ?>">
      <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><path d="M21 15v4a2 2 0 01-2 2H5a2 2 0 01-2-2v-4"/><polyline points="7 10 12 15 17 10"/><line x1="12" y1="15" x2="12" y2="3"/></svg>
      Download free
    </a>
    <button class="btn-secondary" data-copy="<?= e(BASE_URL . meme_url($meme)) ?>" type="button">Copy link</button>
    <button class="btn-secondary" data-share="<?= e(BASE_URL . meme_url($meme)) ?>" data-title="<?= e($meme['title']) ?>" type="button">Share</button>
  </div>

  <dl class="meme-meta">
    <div><dt>Score</dt><dd><?= e(compact_num((int) $meme['score'])) ?> upvotes</dd></div>
    <div><dt>Author</dt><dd>u/<?= e($meme['author']) ?></dd></div>
    <?php if ($meme['subreddit'] !== ''): ?>
    <div><dt>Source</dt><dd><a href="<?= e($meme['post_url']) ?>" rel="nofollow noopener ugc">r/<?= e($meme['subreddit']) ?></a></dd></div>
    <?php endif ?>
    <div><dt>License</dt><dd>Free to use &amp; share</dd></div>
  </dl>

  <?php if ($tags): ?>
  <div class="meme-tags">
    <?php foreach ($tags as $tag): ?>
    <a class="filter-pill" href="/search?q=<?= e(rawurlencode($tag)) ?>">#<?= e($tag) ?></a>
    <?php endforeach ?>
  </div>
  <?php endif ?>
</article>

<?php if (!empty($related)): ?>
<div class="section-header">
  <h2 class="section-title">More <?= e(cat_label($meme['category'])) ?></h2>
  <a href="<?= e(category_url($meme['category'])) ?>" class="section-link">View all →</a>
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
    'url' => BASE_URL . meme_url($meme),
    'name' => $meme['title'],
    'description' => $meme['description'],
    'width' => $w,
    'height' => $h,
    'datePublished' => $meme['created_at'],
    'author' => ['@type' => 'Person', 'name' => $meme['author']],
    'acquireLicensePage' => BASE_URL . '/',
    'creditText' => 'OpenMeme',
], JSON_UNESCAPED_SLASHES | JSON_UNESCAPED_UNICODE) ?>
</script>
