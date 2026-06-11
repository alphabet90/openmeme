# OpenMeme Site — vanilla PHP + SQLite + jQuery

Extreme-minimalist rebuild of the OpenMeme web client. No frameworks, no
build complexity, no runtime dependencies beyond PHP and nginx.

**`/memes/*` is the source of truth.** The SQLite database is a disposable
index rebuilt from MDX frontmatter at any time.

## Stack

| Layer | Tech |
|-------|------|
| Server | nginx + PHP-FPM 8.2+ (pdo_sqlite, gd) |
| Data | SQLite (FTS5 full-text search, LIKE fallback) |
| Frontend | Server-rendered PHP templates + jQuery enhancement |
| Build | webpack 5 (one JS bundle + one CSS bundle) |

## Architecture

```
memes/**/*.mdx ──(bin/build-index.php)──▶ data/memes.db ──▶ public/index.php ──▶ HTML
                                                                    ▲
assets/{js,css} ──(webpack)──▶ public/assets/{app.js,app.css} ──────┘
```

- Every page is fully server-rendered → crawlable by Google with zero JS.
- jQuery only adds enhancement: search suggestions, mobile drawer, copy/share.
- Images carry `width`/`height` from the index → no layout shift (CLS = 0).
- `sitemap.xml`, `robots.txt`, canonical URLs, OG tags and JSON-LD
  (`WebSite` + `SearchAction`, `ImageObject` per meme) are built in.

## Routes

| Route | Purpose |
|-------|---------|
| `/` | Home: hero search, trending, top categories, stats |
| `/meme/{slug}` | Meme detail: download, share, related, JSON-LD |
| `/category/{cat}` | Paginated category listing |
| `/categories` | All categories |
| `/search?q=` | Full-text search (noindex) |
| `/top`, `/nuevos` | Paginated listings by score / recency |
| `/terminos`, `/privacidad`, `/dmca`, `/contacto` | Legal & contact pages (content from apps/web es-AR) |
| `/random` | 302 → random meme |
| `/api/suggest?q=` | JSON suggestions for the search dropdown |
| `/sitemap.xml`, `/robots.txt` | SEO |
| `/memes/{cat}/{img}` | Meme images, served by nginx with 1y immutable cache |

## Setup

```bash
cd site

# 1. Build the frontend bundle
npm install
npm run build                  # → public/assets/app.{js,css}

# 2. Build the SQLite index from /memes/*
php bin/build-index.php        # → data/memes.db

# 3a. Dev server
php -S 0.0.0.0:8090 -t public public/index.php

# 3b. Production: nginx + php-fpm, see nginx.conf
```

Re-run `php bin/build-index.php` whenever `/memes/*` changes (e.g. from the
scraper pipeline or a cron/CI step). The rebuild is atomic — the live DB is
swapped via rename, so there is zero downtime.

Set `OPENMEME_BASE_URL=https://yourdomain.com` in the PHP-FPM environment
for correct sitemap/OG absolute URLs.
