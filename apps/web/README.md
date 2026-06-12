# OpenMeme Site — vanilla PHP + SQLite + Meilisearch + jQuery

Extreme-minimalist rebuild of the OpenMeme web client. No frameworks, no
build complexity. Runtime dependencies: PHP, nginx, and a self-hosted
Meilisearch instance for search.

**`/memes/*` is the source of truth.** The SQLite database and the
Meilisearch index are disposable, rebuilt from MDX frontmatter at any time.

## Stack

| Layer | Tech |
|-------|------|
| Server | nginx + PHP-FPM 8.2+ (pdo_sqlite, gd) |
| Data | SQLite (pages, listings) + Meilisearch (search, required) |
| PHP deps | Composer: meilisearch-php SDK + Guzzle |
| Frontend | Server-rendered PHP templates + jQuery enhancement |
| Build | webpack 5 (one JS bundle + one CSS bundle) |

## Architecture

```
memes/**/*.mdx ──(bin/build-index.php)──▶ data/memes.db ──▶ public/index.php ──▶ HTML
                                               │                    ▲
                                (bin/build-search.php)              │ /search, /api/suggest
                                               ▼                    ▼
                                     Meilisearch index 'memes' ◀── src/meili.php
assets/{js,css} ──(webpack)──▶ public/assets/{app.js,app.css} ──▶ browser
```

- Every page is fully server-rendered → crawlable by Google with zero JS.
- jQuery only adds enhancement: search suggestions, mobile drawer, copy/share.
- Images carry `width`/`height` from the index → no layout shift (CLS = 0).
- `sitemap.xml`, `robots.txt`, canonical URLs, OG tags and JSON-LD
  (`WebSite` + `SearchAction`, `ImageObject` per meme) are built in.

## Internationalization

Two locales only (Argentina-first):

| Locale | URL | Content |
|--------|-----|---------|
| `es-AR` (default) | `/...` | Localized from `{slug}.es-AR.mdx` (`meme_locales` table) |
| `en-US` | `/en/...` | Canonical English from base `{slug}.mdx` |

- `.es-AR.mdx` files are the Argentina memes: they localize the canonical
  row, or **become canonical themselves** when no base file exists.
- Every page emits `hreflang` alternates (`es-AR`, `en-US`, `x-default`);
  the sitemap lists both URLs per page with `xhtml:link` alternates.
- Search is multilingual in both locales: queries match English and
  Spanish text, results render in the active locale (see Search below).
- UI strings live in `src/i18n.php` (`t()`); static page content per
  locale in `src/pages.php`. Language switcher (AR / US) in the nav.

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

## Search (Meilisearch)

`/search` and `/api/suggest` run on a **required** self-hosted Meilisearch
instance (the rest of the site is pure SQLite and works without it).

- **One bilingual index** (`memes`): each document carries `*_en` fields
  (canonical MDX) and `*_es` fields (es-AR translations). The
  `localizedAttributes` setting gives each language its own tokenizer, and
  a single query matches both — "soga" works on the English site and
  "noose" on the Spanish one.
- **Relevance**: titles > tags > category > descriptions; default ranking
  rules plus a final `score:desc` tie-break. Typo tolerance is on
  (Meilisearch defaults). `synonyms` is an empty hook for Argentine slang.
- **Zero-downtime reindex**: `bin/build-search.php` builds a staging index
  and atomically swaps it with the live one.
- **Degradation**: if Meilisearch is down, `/search` returns a localized
  HTTP 503 message and `/api/suggest` returns `[]`; everything else keeps
  working. Search recovers without a restart.
- **Abuse limits**: queries capped at 100 chars and suggestions require
  ≥ 2 chars (`config.php`, mirrored in `app.js` + input `maxlength`);
  nginx rate-limits `/api/*` (10 r/s per IP, burst 20 → 429).

Keys: dev falls back to `MEILI_MASTER_KEY` for everything. In production,
print the auto-generated scoped keys with `php bin/meili-keys.php` and set
`MEILI_SEARCH_KEY` (runtime) and `MEILI_ADMIN_KEY` (indexer) in the env.

## Setup

```bash
# 0. Start Meilisearch (repo root; set MEILI_MASTER_KEY in .env first)
cp .env.example .env           # then: MEILI_MASTER_KEY=$(openssl rand -base64 24)
docker compose up -d

cd apps/web

# 1. Install PHP dependencies + build the frontend bundle
composer install
pnpm install
pnpm build                     # → public/assets/app.{js,css}

# 2. Build the SQLite index from /memes/*, then push to Meilisearch
php bin/build-index.php        # → data/memes.db
php bin/build-search.php       # → Meilisearch index 'memes'
# Or both, from the repo root: pnpm index

# 3a. Dev server
php -S 0.0.0.0:8090 -t public public/index.php

# 3b. Production: nginx + php-fpm, see nginx.conf
```

Re-run both index scripts (or `pnpm index` from the repo root) whenever
`/memes/*` changes (e.g. from the scraper pipeline or a cron/CI step). Both
rebuilds are atomic — SQLite swaps via rename, Meilisearch via index swap —
so there is zero downtime.

Tests: `php vendor/bin/phpunit` (unit) and
`php vendor/bin/phpunit --testsuite integration` (needs Meilisearch running
and both indexes built).

The app reads env from `apps/web/.env`, falling back to the repo-root
`.env` (real environment variables always win). Set
`OPENMEME_BASE_URL=https://yourdomain.com` for correct sitemap/OG absolute
URLs, and `MEILI_URL` / `MEILI_SEARCH_KEY` for search (see nginx.conf for
the PHP-FPM alternative). Set `OPENMEME_CDN_URL=https://cdn.yourdomain.com`
to serve meme images from a CDN; when empty, images are served same-origin
from `/memes/*`. Download and copy-image links always stay same-origin
(the `download` attribute and clipboard fetch don't work cross-origin).
