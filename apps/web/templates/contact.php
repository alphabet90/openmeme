<?php /** Expects: $content (from src/pages.php) */ ?>
<article class="legal-article">
  <p class="legal-eyebrow"><?= e($content['eyebrow']) ?></p>
  <h1 class="legal-title"><?= e($content['title']) ?></h1>
  <p class="legal-intro"><?= e($content['description']) ?></p>
  <div class="contact-card">
    <h2 class="contact-heading"><?= e($content['email_heading']) ?></h2>
    <a class="contact-email" href="mailto:<?= e($content['email_address']) ?>"><?= e($content['email_address']) ?></a>
    <p class="contact-response"><?= e($content['response_time']) ?></p>
  </div>
</article>
