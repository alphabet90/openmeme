# OpenMeme — Agent Guide

Single source of truth for AI coding agents working on this repository. OpenMeme is a modular monorepo for scraping, classifying, indexing, and serving Reddit memes.

## Subsystems

1. **TypeScript Scraper** (`packages/scraper/`) — Reddit pipeline, Bloom filter, AI classification, git automation
2. **Automation Scripts** (`scripts/`) — guard.ts (validation), sync.ts (auto-sync), optimize.ts (image compression)
3. **CLI Tools** (`tools/cli/`) — Interactive meme management (add, list, search, validate, stats, import)
4. **Dev Tools** (`tools/dev/`) — Lint, benchmark, prompt generation, db-check, git hooks
5. **Design System** (`packages/design-system/`) — Color tokens, typography, components (Refero standard)
6. **Java API** (`apps/api/`) — Spring Boot REST API + PostgreSQL + Redis (from reddit.memes)
7. **Next.js Web Client** (`apps/web/`) — React/TypeScript frontend (from reddit.memes)
8. **Meme Collection** (`memes/`) — Git-tracked meme images + MDX metadata (from reddit.memes)

---

## Tech Stack

| Layer | Tech |
|-------|------|
| Scraper | TypeScript 5.7, Node 20, pnpm, commander, p-queue, sharp |
| Scripts | TypeScript 5.7, Node 20, sharp |
| CLI | TypeScript 5.7, chalk, ora |
| Design System | CSS tokens, Anton + Space Grotesk fonts, Refero standard |
| API | Java 21, Spring Boot 3.3.4, Maven, PostgreSQL 16, Redis 7, Flyway |
| Web | Next.js 16.2.4, React 19, TypeScript 5, Tailwind CSS 4, next-intl |
| DevOps | Docker Compose, GitHub Actions, TurboRepo |

---

## Monorepo Structure

```
├── packages/
│   ├── scraper/           # Core scraper pipeline (TypeScript)
│   │   ├── src/           # scraper, downloader, classifier, saver, pipeline, bloom, validator
│   │   └── package.json   # @openmeme/scraper
│   └── design-system/     # Design tokens + components (CSS/Refero)
│       ├── tokens/        # colors, typography, spacing, shadows, motion, radius
│       ├── src/components.css
│       └── references/design-refero.md
├── scripts/
│   ├── src/guard.ts       # Pre-commit validation gate
│   ├── src/sync.ts        # Reddit auto-sync
│   ├── src/optimize.ts    # Image optimization
│   └── package.json       # @openmeme/scripts
├── tools/
│   ├── cli/               # openmeme CLI utility
│   │   ├── src/commands/  # add, list, search, validate, stats, import
│   │   └── package.json   # @openmeme/cli
│   └── dev/               # Developer utilities
│       ├── src/commands/  # lint, benchmark, generate-prompt, db-check, setup-hooks
│       └── package.json   # @openmeme/dev
├── craft/
│   └── rules.md           # Brand manifesto + quality standards
├── apps/api/                   # Java Spring Boot REST API
├── apps/web/           # Next.js frontend
├── memes/                 # Git-tracked meme collection
├── .github/workflows/     # CI/CD (index-memes.yml)
├── docker-compose.yml     # Local dev stack
├── turbo.json             # TurboRepo task orchestration
├── pnpm-workspace.yaml    # Workspace definition
└── package.json           # Root scripts (pnpm scrape, guard, sync, etc.)
```

---

## Build & Run Commands

### Scraper
```bash
pnpm scrape --subreddit argentina --limit 100
cd packages/scraper && pnpm build && node dist/cli.js scrape --subreddit argentina
```

### Scripts
```bash
pnpm guard --staged         # Validate staged memes
pnpm sync --subreddit memes # Sync from Reddit
pnpm optimize --all         # Optimize all images
```

### CLI
```bash
pnpm add-meme <image>       # Add meme interactively
pnpm validate               # Validate all memes
pnpm stats                  # Repository statistics
```

### Design System
```bash
cd packages/design-system    # CSS tokens — no build needed
```

### Java API
```bash
cd api && mvn verify         # Compile + test
```

### Web Client
```bash
cd apps/web && pnpm dev   # Dev server
```

### Docker Compose (full stack)
```bash
docker-compose up            # Postgres + Redis + API
```

---

## Code Style

### TypeScript (Scraper, Scripts, CLI, Dev)
- Node 20+ ES2022, `NodeNext` module resolution
- Strict TypeScript, no `any` unless necessary
- `pnpm` for package management
- Workspace packages: `@openmeme/scraper`, `@openmeme/scripts`, `@openmeme/cli`, `@openmeme/dev`

### Java (API)
- Lombok required: `@Data`, `@Value`, `@Builder`, `@Slf4j`
- No ternary operators — use `Optional<T>`
- Controllers delegate only; services own business logic
- OpenAPI spec is source of truth

### TypeScript / Next.js (Web)
- App Router, Server Components by default
- CSS Modules for component-scoped styles
- next-intl for i18n (default: `es-AR`)
- `next/image` with whitelisted CDN

---

## Testing

| Suite | Command |
|-------|---------|
| Scraper | `cd packages/scraper && pnpm test` |
| Bloom filter | `cd packages/scraper && pnpm test` |
| API | `cd api && mvn test` |
| Web | `cd apps/web && pnpm lint` |

---

## Security

- Admin API (`/admin/*`) protected by `ApiKeyAuthFilter`
- `ADMIN_API_KEY` via env var; default in Docker: `testing`
- Sensitive headers (`X-Api-Key`, `Authorization`) masked in logs
- AI CLI processes (`claude`, `codex`) require installed + authenticated CLIs
- `.env` is gitignored — never commit it
- **Never scan `memes/` in bulk** — thousands of images will exhaust context

---

## Deployment

| Component | Method |
|-----------|--------|
| API | Docker multi-stage → Railway/Render |
| Web | Static export or Node.js → Vercel |
| CDN | Cloudflare Worker for meme images |
| CI | GitHub Actions reindexes memes on push to `main` |

---

## Key Conventions

| Convention | Detail |
|------------|--------|
| Deduplication | Bloom filter for URLs/post IDs; SHA1 for content |
| State file | `processed.bloom` (~360 KB fixed) |
| Git branching | `memes/{subreddit}-{YYYYMMDD-HHMMSS}` |
| MDX format | `title`, `description`, `author`, `subreddit`, `category`, `slug`, `score`, `created_at`, `source_url`, `post_url`, `image`, `tags` |
| API caching | Redis: `stats`, `categories`, `memeList`, `meme`, `search` |
| Design tokens | Refero standard: lime `#D4FF00`, dark `#0D0D0D`, Anton display, 4px grid |
