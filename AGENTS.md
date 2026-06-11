# OpenMeme — Agent Guide

Single source of truth for AI coding agents working on this repository. OpenMeme is a modular monorepo for scraping, classifying, indexing, and serving Reddit memes.

## Subsystems

1. **TypeScript Scraper** (`packages/scraper/`) — Reddit pipeline, Bloom filter, AI classification, git automation
2. **Automation Scripts** (`scripts/`) — guard.ts (validation), sync.ts (auto-sync), optimize.ts (image compression)
3. **CLI Tools** (`tools/cli/`) — Interactive meme management (add, list, search, validate, stats, import)
4. **Dev Tools** (`tools/dev/`) — Lint, benchmark, prompt generation, db-check, git hooks
5. **Web Site** (`apps/web/`) — Vanilla PHP + SQLite + jQuery site
6. **Meme Collection** (`memes/`) — Git-tracked meme images + MDX metadata
7. **Skills** (`skills/`) — Reusable AI capabilities: i18n-localizer, meme-classifier, meme-curator

---

## Tech Stack

| Layer | Tech |
|-------|------|
| Scraper | TypeScript 5.7, Node 20, pnpm, commander, p-queue, sharp, vitest |
| Scripts | TypeScript 5.7, Node 20, sharp |
| CLI | TypeScript 5.7, commander, inquirer, chalk, ora |
| Dev Tools | TypeScript 5.7, commander, chalk, ora |
| Web | PHP 8.2+, SQLite (FTS5), nginx, webpack 5, jQuery |
| DevOps | GitHub Actions, TurboRepo |

---

## Monorepo Structure

```
├── apps/
│   └── web/                    # Vanilla PHP + SQLite + jQuery site
│       ├── public/             # Front controller + static assets
│       ├── src/                # PHP helpers, i18n, repo queries
│       ├── templates/          # Server-rendered PHP templates
│       ├── bin/                # build-index.php (SQLite index from memes/)
│       ├── assets/             # Source JS/CSS for webpack
│       ├── nginx.conf          # Production nginx config
│       └── package.json        # @openmeme/web
├── packages/
│   └── scraper/                # Core scraper pipeline (TypeScript)
│       ├── src/                # scraper, downloader, classifier, saver, pipeline, bloom, validator
│       └── package.json        # @openmeme/scraper
├── scripts/
│   ├── src/guard.ts            # Pre-commit validation gate
│   ├── src/sync.ts             # Reddit auto-sync
│   ├── src/optimize.ts         # Image optimization
│   └── package.json            # @openmeme/scripts
├── tools/
│   ├── cli/                    # openmeme CLI utility
│   │   ├── src/commands/       # add, list, search, validate, stats, import
│   │   └── package.json        # @openmeme/cli
│   └── dev/                    # Developer utilities
│       ├── src/commands/       # lint, benchmark, generate-prompt, db-check, setup-hooks
│       └── package.json        # @openmeme/dev
├── skills/
│   ├── i18n-localizer/         # Translate memes into locale MDX files
│   ├── meme-classifier/        # Standardize categories and enforce taxonomy
│   └── meme-curator/           # SEO-optimize metadata
├── craft/
│   └── rules.md                # Brand manifesto + quality standards
├── memes/                      # Git-tracked meme collection
├── turbo.json                  # TurboRepo task orchestration
├── pnpm-workspace.yaml         # Workspace definition
└── package.json                # Root scripts (pnpm scrape, guard, sync, etc.)
```

### Workspace Package Status

| Package | Status | Build Tool |
|---------|--------|------------|
| `@openmeme/web` | Active | webpack |
| `@openmeme/scraper` | Active | tsc |
| `@openmeme/scripts` | Active | tsc |
| `@openmeme/cli` | Active | tsc |
| `@openmeme/dev` | Active | tsc |

### Inter-workspace Dependencies

- `@openmeme/cli` → `@openmeme/scraper`
- `@openmeme/dev` → `@openmeme/scraper`
- `@openmeme/scripts` → `@openmeme/scraper`

---

## Build & Run Commands

### Root (TurboRepo)
```bash
pnpm build                   # Build all workspace packages
pnpm dev                     # Start all dev servers/watchers
pnpm lint                    # Lint all packages
pnpm test                    # Run all test suites
pnpm clean                   # Clean build artifacts
```

### Scraper
```bash
pnpm scrape --subreddit argentina --limit 100
cd packages/scraper && pnpm build && node dist/cli.js scrape --subreddit argentina
```

### Scripts
```bash
pnpm guard --staged          # Validate staged memes
pnpm sync --subreddit memes  # Sync from Reddit
pnpm optimize --all          # Optimize all images
```

### CLI
```bash
pnpm add-meme <image>        # Add meme interactively
pnpm validate                # Validate all memes
pnpm stats                   # Repository statistics
```

### Dev Tools
```bash
pnpm --filter @openmeme/dev lint --fix              # Lint MDX frontmatter
pnpm --filter @openmeme/dev generate-prompt es-AR   # Generate classifier prompt
pnpm --filter @openmeme/dev benchmark               # Benchmark pipeline
pnpm --filter @openmeme/dev db-check                # Check repo consistency
pnpm --filter @openmeme/dev setup-hooks             # Install git hooks
```

### Web Site
```bash
cd apps/web
pnpm build                   # webpack → public/assets/app.{js,css}
php bin/build-index.php      # Rebuild SQLite index from memes/*
php -S 0.0.0.0:8090 -t public public/index.php   # Dev server
```

For production, use nginx + PHP-FPM with `apps/web/nginx.conf`.

---

## Code Style

### TypeScript (Scraper, Scripts, CLI, Dev)
- Node 20+ ES2022, `NodeNext` module resolution
- Strict TypeScript, no `any` unless necessary
- `pnpm` for package management
- Workspace packages: `@openmeme/scraper`, `@openmeme/scripts`, `@openmeme/cli`, `@openmeme/dev`

### PHP (Web)
- PHP 8.2+ with `declare(strict_types=1);`
- Server-rendered templates; no framework
- `/memes/*` is the source of truth; `data/memes.db` is a disposable index
- Two locales: `es-AR` (default) and `en-US` (at `/en/...`)

---

## Testing

| Suite | Command | Details |
|-------|---------|---------|
| Scraper | `cd packages/scraper && pnpm test` | Vitest |
| Web | `cd apps/web && pnpm build` | webpack build validation |

---

## Security

- AI CLI processes (`claude`, `codex`) require installed + authenticated CLIs
- `.env` is gitignored — never commit it
- **Never scan `memes/` in bulk** — thousands of images will exhaust context

---

## Deployment

| Component | Method |
|-----------|--------|
| Web | PHP-FPM + nginx (see `apps/web/nginx.conf`) |
| CI | GitHub Actions (to be redefined for the PHP site) |

---

## Key Conventions

| Convention | Detail |
|------------|--------|
| Deduplication | Bloom filter for URLs/post IDs; SHA1 for content |
| State file | `processed.bloom` (~360 KB fixed) |
| Git branching | `memes/{subreddit}-{YYYYMMDD-HHMMSS}` |
| MDX format | `title`, `description`, `author`, `subreddit`, `category`, `slug`, `score`, `created_at`, `source_url`, `post_url`, `image`, `tags` |
| MDX localization | Base: `{slug}.mdx` (English); translations: `{slug}.{locale}.mdx` (e.g., `slug.es-AR.mdx`) |
| Design tokens | Refero standard: lime `#D4FF00`, dark `#0D0D0D`, Anton display, Space Grotesk UI, 4px grid |
| Commit messages | `Add {N} memes from r/{subreddit} batch {N} [{category1}({count1}), {category2}({count2})]` |
| Category system | `funny`, `wholesome`, `politics`, `gaming`, `tech`, `culture`, `relatable`, `absurd`, `argentina`, `other` |
| Quality criteria | A good meme satisfies ≥3 of: cultural relevance, originality, emotional connection, technical quality (min 400×400px), clear context, appropriate format |
| Health targets | >90% complete metadata, <5% duplicates, <2% uncategorized, 100% author attribution |

---

## Skills

Skills in `skills/` are reusable AI capabilities with a consistent structure:

```
skills/<skill-name>/
├── SKILL.md           # Primary instruction document
├── references/        # Supporting docs (templates, rubrics, taxonomies)
└── scripts/           # Python helper scripts
```

| Skill | Purpose |
|-------|---------|
| `i18n-localizer` | Translate & culturally adapt memes into `{slug}.{locale}.mdx` files |
| `meme-classifier` | Standardize category directories, merge duplicates, enforce taxonomy |
| `meme-curator` | SEO-optimize metadata (titles, descriptions, tags, slugs) |

---

## Environment Variables

Copy `.env.example` to `.env` and configure per subsystem:

| Section | Key Vars |
|---------|----------|
| Scraper | `SUBREDDIT`, `REPO_PATH`, `TMP_DIR`, `BLOOM_FILTER_FILE`, `CLASSIFIER` (`claude`/`codex`), `CLASSIFY_WORKERS`, `REDDIT_CLIENT_ID/SECRET`, `REDDIT_USERNAME/PASSWORD` |
| Scripts | `BATCH_SIZE`, `DRY_RUN`, `PER_POST`, `FROM_FILE`, `POST_URL` |
| Sync | `SYNC_SUBREDDITS`, `SYNC_LIMIT`, `SYNC_BATCH_SIZE`, `SYNC_CLASSIFIER`, `SYNC_CLASSIFY_WORKERS`, `SYNC_MIN_COMMENT_UPVOTES`, `SYNC_DRY_RUN`, `SYNC_TIME` |
| Optimize | `OPTIMIZE_DRY_RUN`, `OPTIMIZE_RESIZE` |
| Web | `OPENMEME_BASE_URL` |
