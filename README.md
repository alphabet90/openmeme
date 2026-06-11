# OpenMeme

> The open-source meme curation platform. Scraped from Reddit, classified by AI, curated by the community.

---

## What is OpenMeme?

OpenMeme is a modular monorepo for scraping, classifying, indexing, and serving Reddit memes. It combines:

- **TypeScript scraper** with pluggable AI classifiers (Claude / Codex)
- **Bloom filter deduplication** — tracks 200K items in ~360 KB
- **Git-tracked meme collection** — images + MDX metadata, browsable on GitHub
- **Format/franchise category folders** — `memes/simpsons/`, `memes/reaction/`, `memes/argentina-politics/`, etc.
- **Pre-commit validation** — guard script checks quality before every commit
- **Auto-sync** — scheduled Reddit synchronization with configurable intervals
- **Vanilla PHP web site** — server-rendered, SQLite-indexed, zero framework overhead

---

## Monorepo Structure

```
├── apps/
│   └── web/                # Vanilla PHP + SQLite + jQuery site
│       ├── public/         # Front controller + static assets
│       ├── src/            # PHP helpers, i18n, repo queries
│       ├── templates/      # Server-rendered PHP templates
│       ├── bin/            # build-index.php (SQLite index from memes/)
│       ├── assets/         # Source JS/CSS for webpack
│       └── package.json    # @openmeme/web
├── packages/
│   └── scraper/            # Core scraper pipeline (TypeScript)
│       ├── src/            # cli, scraper, downloader, classifier, saver, pipeline, bloom, validator
│       ├── package.json    # @openmeme/scraper
│       └── tsconfig.json
├── scripts/
│   ├── src/guard.ts        # Pre-commit validation gate
│   ├── src/sync.ts         # Reddit auto-sync
│   ├── src/optimize.ts     # Image optimization & budget
│   └── package.json        # @openmeme/scripts
├── tools/
│   ├── cli/                # Terminal utility (add, list, search, validate, stats, import)
│   │   ├── src/commands/
│   │   └── package.json    # @openmeme/cli
│   └── dev/                # Developer utilities (lint, benchmark, db-check, setup-hooks)
│       ├── src/commands/
│       └── package.json    # @openmeme/dev
├── skills/                 # Reusable AI capabilities (classifier, curator, localizer)
├── craft/
│   └── rules.md            # Brand manifesto + "good meme" criteria + anti-AI-slop
├── memes/                  # Git-tracked meme collection (format/franchise folders)
├── site/                   # STALE leftover (not a workspace; ignore)
├── docs/
│   └── CLI.md              # Complete CLI reference
├── turbo.json              # TurboRepo orchestration
├── pnpm-workspace.yaml     # Workspace definition
└── package.json            # Root scripts (pnpm scrape, guard, sync, etc.)
```

---

## Quick Start

### Prerequisites

- Node.js 22 (see `.nvmrc`) and pnpm
- PHP 8.2+ with `pdo_sqlite` and `gd`
- Reddit app credentials (for scraper)

### Install

```bash
# Clone the monorepo
git clone https://github.com/alphabet90/openmeme.git
cd openmeme

# Install dependencies
pnpm install

# Build all packages
pnpm build
```

> Note: `pnpm lint` currently fails because `eslint` is not installed, and `pnpm test` fails because no test files exist yet.

### Scrape Memes

```bash
# Basic scrape (requires claude CLI)
pnpm scrape --subreddit argentina --limit 50

# Or with environment variables
export SUBREDDIT=argentina
export REDDIT_USER_AGENT="OpenMemeBot/1.0"
pnpm scrape --limit 100
```

The classifier looks for prompts at `packages/scraper/prompts/prompt.{locale}.txt`. If the directory does not exist, it falls back to a hard-coded prompt. Generate a locale prompt with:

```bash
pnpm generate-prompt es-AR
```

### Validate & Commit

```bash
# Check quality before commit
pnpm guard --staged

# Auto-sync from Reddit
pnpm sync --subreddit argentina --limit 50

# Optimize images
pnpm optimize --all
```

### Run the Web Site

```bash
# 1. Build the front-end bundle
cd apps/web && pnpm build

# 2. Build the SQLite index from /memes/*
php bin/build-index.php

# 3. Start the dev server
php -S 0.0.0.0:8090 -t public public/index.php
```

For production, use nginx + PHP-FPM. See `apps/web/nginx.conf`. Ignore the stale `site/` directory.

---

## CLI Reference

| Command | Purpose |
|---------|---------|
| `pnpm scrape --subreddit arg --limit 100` | Scrape Reddit |
| `pnpm guard --staged` | Validate staged memes |
| `pnpm sync --config sync.config.json` | Auto-sync subreddits |
| `pnpm optimize --all` | Compress images |
| `pnpm add-meme ./img.jpg` | Add meme interactively |
| `pnpm validate` | Validate all memes |
| `pnpm stats` | Repository statistics |
| `pnpm import <url>` | Import from Reddit/URL |
| `pnpm generate-prompt es-AR` | Generate classifier prompt |
| `pnpm db-check` | Check repository consistency |

Full CLI docs: [`docs/CLI.md`](docs/CLI.md)

---

## Tech Stack

| Layer | Technology |
|-------|------------|
| Scraper | TypeScript 5.7, Node 22, commander, p-queue, sharp |
| Scripts | TypeScript 5.7, sharp |
| CLI | TypeScript 5.7, chalk, ora, commander |
| Web | PHP 8.2, SQLite (FTS5), nginx, webpack 5, jQuery |
| DevOps | GitHub Actions, TurboRepo |

---

## Environment Variables

```bash
# Scraper
SUBREDDIT=argentina
REPO_PATH=/path/to/repo
TMP_DIR=/tmp/memes
BLOOM_FILTER_FILE=data/processed.bloom
CLASSIFIER=claude          # claude or codex
CLASSIFY_WORKERS=4

# Reddit API
REDDIT_USER_AGENT="OpenMemeBot/1.0 (by u/your_username)"
REDDIT_CLIENT_ID=your_id
REDDIT_CLIENT_SECRET=your_secret

# Web
OPENMEME_BASE_URL=https://openmeme.example.com

# Scripts
BATCH_SIZE=10
DRY_RUN=false
PER_POST=false
```

Copy `.env.example` to `.env` and configure your values.

---

## Contributing

1. **Scraper**: `packages/scraper/src/` — TypeScript pipeline
2. **Scripts**: `scripts/src/` — automation (guard, sync, optimize)
3. **CLI**: `tools/cli/src/commands/` — interactive commands
4. **Web**: `apps/web/` — PHP site

**Before committing**: Run `pnpm guard --staged` to validate your memes.

**Quality standards**: See [`craft/rules.md`](craft/rules.md) for the "good meme" manifesto.

---

## Security

- `.env` is gitignored — never commit credentials
- AI CLI processes require installed + authenticated CLIs

---

## License

[Apache-2.0](LICENSE)
