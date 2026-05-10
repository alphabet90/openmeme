# OpenMeme

> The open-source meme curation platform. Scraped from Reddit, classified by AI, curated by the community.

---

## What is OpenMeme?

OpenMeme is a modular monorepo for scraping, classifying, indexing, and serving Reddit memes. It combines:

- **TypeScript scraper** with pluggable AI classifiers (Claude / Codex)
- **Bloom filter deduplication** — tracks 200K items in ~360 KB
- **Git-tracked meme collection** — images + MDX metadata, browsable on GitHub
- **Pre-commit validation** — guard script checks quality before every commit
- **Auto-sync** — scheduled Reddit synchronization with configurable intervals
- **Design system** — Refero-standard tokens and components
- **Java REST API** — Spring Boot + PostgreSQL + Redis
- **Next.js web client** — React/TypeScript/Tailwind

---

## Monorepo Structure

```
├── packages/
│   ├── scraper/            # Core scraper pipeline (TypeScript)
│   │   ├── src/            # scraper, downloader, classifier, saver, pipeline, bloom, validator
│   │   ├── package.json    # @openmeme/scraper
│   │   └── tsconfig.json
│   └── design-system/      # Design tokens + components (CSS/Refero)
│       ├── tokens/         # colors, typography, spacing, shadows, motion, radius
│       ├── src/components.css
│       ├── SKILL.md        # AI agent design skill reference
│       └── references/design-refero.md
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
├── craft/
│   └── rules.md            # Brand manifesto + "good meme" criteria + anti-AI-slop
├── apps/api/                    # Java Spring Boot REST API
├── apps/web/            # Next.js frontend
├── memes/                  # Git-tracked meme collection (2,086 files, 134 MB, 388 categories)
├── docs/
│   └── CLI.md              # Complete CLI reference
├── .github/workflows/
│   └── index-memes.yml     # Auto-reindex memes on push
├── docker-compose.yml      # PostgreSQL + Redis + API
├── turbo.json              # TurboRepo orchestration
├── pnpm-workspace.yaml     # Workspace definition
└── package.json            # Root scripts (pnpm scrape, guard, sync, etc.)
```

---

## Quick Start

### Prerequisites

- Node.js 20+ and pnpm
- Java 21 and Maven (for API)
- Docker + Docker Compose (optional, for full stack)
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

### Scrape Memes

```bash
# Basic scrape (requires claude CLI)
pnpm scrape --subreddit argentina --limit 50

# Or with environment variables
export SUBREDDIT=argentina
export REDDIT_USER_AGENT="OpenMemeBot/1.0"
pnpm scrape --limit 100
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

### Run Full Stack (Docker)

```bash
# Start PostgreSQL, Redis, and API
docker-compose up

# Frontend (in another terminal)
cd apps/web && pnpm dev
```

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

Full CLI docs: [`docs/CLI.md`](docs/CLI.md)

---

## Tech Stack

| Layer | Technology |
|-------|------------|
| Scraper | TypeScript 5, Node 20, commander, p-queue, sharp |
| Scripts | TypeScript 5, sharp |
| CLI | TypeScript 5, chalk, ora, commander |
| Design System | CSS tokens, Anton + Space Grotesk, Refero standard |
| API | Java 21, Spring Boot 3, PostgreSQL 16, Redis 7, Flyway |
| Web | Next.js 16, React 19, TypeScript 5, Tailwind CSS 4 |
| DevOps | Docker Compose, GitHub Actions, TurboRepo |

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

# API
PORT=8080
DB_PASSWORD=memes
ADMIN_API_KEY=testing
API_BASE_URL=http://localhost:8080

# Scripts
BLOOM_FILTER_FILE=data/processed.bloom
REPO_PATH=/path/to/repo
SUBREDDIT=argentina
BATCH_SIZE=10
CLASSIFY_WORKERS=4
CLASSIFIER=claude
DRY_RUN=false
PER_POST=false
```

Copy `.env.example` to `.env` and configure your values.

---

## Design System

OpenMeme's design system follows **Refero Research-First Design** principles:

- **Dark-first**: `#0D0D0D` background — memes are the star
- **Lime accent**: `#D4FF00` — CTAs, badges, highlights
- **Celeste secondary**: `#74C6F4` — brand heritage
- **Type contrast**: Anton (bold uppercase) vs Space Grotesk (clean UI)
- **4px grid**: all spacing based on 4px unit system
- **Motion**: 90-350ms durations, no unnecessary animation

```css
@import '@openmeme/design-system/tokens/index.css';
@import '@openmeme/design-system/src/components.css';
```

See `packages/design-system/references/design-refero.md` for full documentation.

---

## Contributing

1. **Scraper**: `packages/scraper/src/` — TypeScript pipeline
2. **Scripts**: `scripts/src/` — automation (guard, sync, optimize)
3. **CLI**: `tools/cli/src/commands/` — interactive commands
4. **Design**: `packages/design-system/` — tokens, components
5. **API**: `api/` — Java Spring Boot
6. **Web**: `apps/web/` — Next.js frontend

**Before committing**: Run `pnpm guard --staged` to validate your memes.

**Quality standards**: See [`craft/rules.md`](craft/rules.md) for the "good meme" manifesto.

---

## Security

- Admin API endpoints protected by `ApiKeyAuthFilter`
- `ADMIN_API_KEY` via environment variable
- Sensitive headers masked in logs
- `.env` is gitignored — never commit credentials
- AI CLI processes require installed + authenticated CLIs

---

## Migrated From

This project was originally [`reddit.memes`](https://github.com/alphabet90/reddit.memes) — a Python-based Reddit meme scraper. It was migrated to a modular TypeScript monorepo with:

- Full TypeScript port of the scraper pipeline
- New scripts (guard, sync, optimize)
- CLI tool for interactive meme management
- Developer utilities (lint, benchmark, db-check)
- Design system with Refero standards
- Docker Compose orchestration
- CI/CD with GitHub Actions

---

## License

[Apache-2.0](LICENSE)
