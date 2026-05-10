# CLAUDE.md

Guidance for Claude Code when working with the OpenMeme monorepo.

## Project Overview

OpenMeme is a modular monorepo for scraping, classifying, curating, and serving Reddit memes. It contains:

- **packages/scraper/** — TypeScript pipeline: scrape Reddit → download → classify (Claude/Codex AI) → save to git-tracked `memes/`
- **scripts/** — TypeScript automation: guard (pre-commit validation), sync (Reddit auto-sync), optimize (image compression)
- **tools/cli/** — Terminal utility for adding, listing, searching, and validating memes
- **tools/dev/** — Developer utilities: lint, benchmark, prompt generation, db-check, git hooks
- **packages/design-system/** — CSS tokens and components following Refero Research-First Design
- **api/** — Java Spring Boot REST API (from reddit.memes)
- **apps/web/** — Next.js frontend (from reddit.memes)
- **memes/** — Git-tracked meme images + MDX metadata

## Running the Application

### Scraper (TypeScript)
```bash
# Quick start
pnpm scrape --subreddit argentina --limit 100

# Or from package
pnpm --filter @openmeme/scraper scrape --subreddit argentina
```

### CLI
```bash
pnpm add-meme <image>       # Add meme interactively
pnpm validate               # Validate all memes
pnpm stats                  # Show repository statistics
pnpm import <url>           # Import from Reddit/URL
```

### Scripts
```bash
pnpm guard --staged         # Validate staged files before commit
pnpm sync --subreddit memes # Auto-sync from Reddit
pnpm optimize --all         # Compress images, enforce budget
```

### Full dev stack
```bash
docker-compose up           # PostgreSQL + Redis + API
cd apps/web && pnpm dev  # Frontend dev server
```

---

## Architecture

### Monorepo: pnpm workspaces + TurboRepo

```
packages/     # @openmeme/* — shared code
scripts/      # @openmeme/scripts — automation
 tools/       # @openmeme/cli, @openmeme/dev — utilities
```

### Scraper Pipeline (`packages/scraper/src/`)

1. **Scrape** (`scraper.ts`) — Fetches from `old.reddit.com` JSON API with pagination, exponential backoff, share-link resolution
2. **Download** (`downloader.ts`) — Parallel downloads via p-queue, SHA1 dedup, 10MB limit
3. **Classify** (`classifier.ts`) — Pluggable: Claude CLI (default) or Codex CLI. Loads locale-aware prompts
4. **Save & Commit** (`saver.ts`) — Copies to `memes/{category}/{slug}{ext}`, creates MDX, git commits per batch

**State**: `bloom.ts` + `post-tracker.ts` — counting Bloom filter (`processed.bloom`), O(1) membership, supports deletion.

### Scripts (`scripts/src/`)

- **guard.ts** — 10-rule validator: checks image existence, extensions, title, description, URLs, category, slug, author, subreddit, tags. Runs on staged or all files.
- **sync.ts** — Multi-subreddit sync via JSON config. Daemon mode with configurable intervals.
- **optimize.ts** — Sharp-based image compression (JPEG/WebP/AVIF), size budget enforcement, dry-run mode.

### CLI (`tools/cli/src/commands/`)

- **add.ts** — Interactive meme addition with form preview
- **import.ts** — Reddit post URL or direct image URL importing
- **list.ts** — Filtered listing by category
- **search.ts** — Search by tag, title, or description
- **stats.ts** — Repository analytics (categories, formats, top authors/tags)
- **validate.ts** — Run validation across all memes

---

## Design System (`packages/design-system/`)

**Refero Research-First Design** standard:
- Dark-first: `#0D0D0D` background
- Lime accent: `#D4FF00` — CTAs, badges, highlights
- Celeste secondary: `#74C6F4`
- Type: Anton (display) + Space Grotesk (UI)
- Motion: 90-350ms, `cubic-bezier(0.16, 1, 0.3, 1)`
- Spacing: 4px base grid

---

## Code Style

### TypeScript
- Node 20, ES2022, `NodeNext` module resolution
- Strict mode, no `any` unless annotated
- Imports: explicit `.js` extensions for ESM
- Environment config: `config.ts` with `process.env` fallbacks

### Key Conventions
- Image hosts whitelisted in `config.ts` (`IMAGE_HOSTS`)
- Slug sanitization: alphanumerics + hyphens, max 80 chars
- Classifier prompts: `prompts/prompt.{locale}.txt`, BCP 47 naming
- Adding classifier: implement `BaseClassifier`, register in `createClassifier()`
- Git: raw `subprocess` calls via `saver.ts` — no `gitpython`
- CLI flag changes → update `docs/CLI.md` in same commit

---

## Navigation Rules

> Mandatory for all agents.

| Change Type | Work In |
|-------------|---------|
| Scraper / pipeline | `packages/scraper/*` |
| Validation / sync / optimize | `scripts/src/*` |
| CLI commands | `tools/cli/src/commands/*` |
| Dev utilities | `tools/dev/src/commands/*` |
| Design tokens / components | `packages/design-system/*` |
| Backend / API | `api/*` |
| Frontend / UI | `apps/web/*` |
| Quality standards | `craft/rules.md` |

**NEVER scan `memes/` in bulk.** Thousands of images will exhaust context.
