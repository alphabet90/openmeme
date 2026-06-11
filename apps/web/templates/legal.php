<?php /** Expects: $content (from src/pages.php) */ ?>
<article class="legal-article">
  <p class="legal-eyebrow">Legal</p>
  <h1 class="legal-title"><?= e($content['title']) ?></h1>
  <p class="legal-updated"><?= e($content['last_updated']) ?></p>
  <p class="legal-intro"><?= e($content['intro']) ?></p>
<?php foreach ($content['sections'] as [$sectionTitle, $sectionBody]): ?>
  <section class="legal-section">
    <h2 class="legal-section-title"><?= e($sectionTitle) ?></h2>
    <p class="legal-section-body"><?= e($sectionBody) ?></p>
  </section>
<?php endforeach ?>
</article>
