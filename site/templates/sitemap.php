<?= '<?xml version="1.0" encoding="UTF-8"?>' . "\n" ?>
<urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">
  <url><loc><?= e(BASE_URL) ?>/</loc><changefreq>daily</changefreq><priority>1.0</priority></url>
  <url><loc><?= e(BASE_URL) ?>/categories</loc><changefreq>weekly</changefreq><priority>0.8</priority></url>
<?php foreach ($categories as $c): ?>
  <url><loc><?= e(BASE_URL . category_url($c['category'])) ?></loc><changefreq>daily</changefreq><priority>0.7</priority></url>
<?php endforeach ?>
<?php foreach ($memes as $m): ?>
  <url><loc><?= e(BASE_URL . '/meme/' . rawurlencode($m['slug'])) ?></loc><?php if ($m['created_at'] !== ''): ?><lastmod><?= e(substr($m['created_at'], 0, 10)) ?></lastmod><?php endif ?></url>
<?php endforeach ?>
</urlset>
