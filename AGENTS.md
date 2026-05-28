# OpenMeme — Agent Guide

Single source of truth for AI coding agents working on this repository. OpenMeme is a modular monorepo for scraping, classifying, indexing, and serving Reddit memes.

## Subsystems

1. **TypeScript Scraper** (`packages/scraper/`) — Reddit pipeline, Bloom filter, AI classification, git automation
2. **Automation Scripts** (`scripts/`) — guard.ts (validation), sync.ts (auto-sync), optimize.ts (image compression)
3. **CLI Tools** (`tools/cli/`) — Interactive meme management (add, list, search, validate, stats, import)
4. **Dev Tools** (`tools/dev/`) — Lint, benchmark, prompt generation, db-check, git hooks
5. **Design System** (`packages/design-system/`) — Color tokens, typography, components (Refero standard)
6. **Shared UI** (`packages/ui/`) — React presentational components consumed by the web app
7. **Java API** (`apps/api/`) — Spring Boot REST API + PostgreSQL + Redis + Flyway
8. **Next.js Web Client** (`apps/web/`) — React/TypeScript frontend with next-intl i18n
9. **Meme Collection** (`memes/`) — Git-tracked meme images + MDX metadata
10. **Skills** (`skills/`) — Reusable AI capabilities: i18n-localizer, meme-classifier, meme-curator

---

## Tech Stack

| Layer | Tech |
|-------|------|
| Scraper | TypeScript 5.7, Node 20, pnpm, commander, p-queue, sharp, vitest |
| Scripts | TypeScript 5.7, Node 20, sharp |
| CLI | TypeScript 5.7, commander, inquirer, chalk, ora |
| Dev Tools | TypeScript 5.7, commander, chalk, ora |
| Design System | CSS tokens, Anton + Space Grotesk fonts, Refero standard, Storybook |
| Shared UI | React 19, TypeScript 5, CSS Modules |
| API | Java 21, Spring Boot 3.3.4, Maven, PostgreSQL 16, Redis 7, Flyway, Testcontainers |
| Web | Next.js 16.2.4, React 19, TypeScript 5, Tailwind CSS 4, next-intl, PostHog, @opennextjs/cloudflare |
| DevOps | Docker Compose, GitHub Actions, TurboRepo, Cloudflare Workers |

---

## Monorepo Structure

```
├── apps/
│   ├── api/                    # Java Spring Boot REST API (Maven project)
│   │   ├── pom.xml
│   │   ├── src/main/java/      # Controllers, services, repositories, config
│   │   ├── src/main/resources/ # openapi.yaml, application.yml, Flyway migrations
│   │   ├── src/test/java/      # JUnit 5 + Testcontainers tests
│   │   └── Dockerfile          # Multi-stage build
│   └── web/                    # Next.js frontend
│       ├── app/                # App Router with [locale] segment
│       ├── components/         # App-specific components
│       ├── lib/                # API client, data fetchers, SEO utilities
│       ├── i18n/               # next-intl config
│       └── messages/           # 7 JSON translation files
├── packages/
│   ├── scraper/                # Core scraper pipeline (TypeScript)
│   │   ├── src/                # scraper, downloader, classifier, saver, pipeline, bloom, validator
│   │   └── package.json        # @openmeme/scraper
│   ├── design-system/          # Design tokens + components (CSS/Refero)
│   │   ├── tokens/             # colors, typography, spacing, shadows, motion, radius
│   │   ├── src/components.css
│   │   ├── scripts/build-tokens.js
│   │   └── package.json        # @openmeme/design-system
│   ├── ui/                     # Shared React components
│   │   ├── src/                # MemeCard, MasonryGrid, Pagination, icons, etc.
│   │   └── package.json        # @openmeme/ui
│   ├── config/                 # Shared static TS/ESLint configs (stub/minimal)
│   │   └── package.json        # @openmeme/config
│   └── utils/                  # Minimal shared utilities (stub/minimal)
│       └── package.json        # @openmeme/utils
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
├── .github/workflows/          # CI/CD (index-memes.yml)
├── docker-compose.yml          # Local dev stack
├── turbo.json                  # TurboRepo task orchestration
├── pnpm-workspace.yaml         # Workspace definition
└── package.json                # Root scripts (pnpm scrape, guard, sync, etc.)
```

### Workspace Package Status

| Package | Status | Build Tool |
|---------|--------|------------|
| `@openmeme/web` | Active | Next.js |
| `@openmeme/api` | Active (Maven) | Maven — package.json is a placeholder for monorepo compatibility |
| `@openmeme/scraper` | Active | tsc |
| `@openmeme/design-system` | Active | tsc + token compiler |
| `@openmeme/ui` | Active | Consumed as raw TypeScript source |
| `@openmeme/scripts` | Active | tsc |
| `@openmeme/cli` | Active | tsc |
| `@openmeme/dev` | Active | tsc |
| `@openmeme/config` | Stub/minimal | Consumed as raw TypeScript source |
| `@openmeme/utils` | Stub/minimal | Consumed as raw TypeScript source |

### Inter-workspace Dependencies

- `@openmeme/web` → `@openmeme/ui`, `@openmeme/design-system`
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

### Design System
```bash
cd packages/design-system
pnpm build                   # tsc + build-tokens
pnpm storybook               # Start Storybook dev server
pnpm build-storybook         # Build Storybook static site
```

### Java API
```bash
cd apps/api
mvn verify                   # Compile + generate OpenAPI stubs + run all tests
mvn test                     # Run tests only
mvn package                  # Build fat JAR
mvn generate-sources         # Regenerate API stubs from openapi.yaml
```

### Web Client
```bash
cd apps/web
pnpm dev                     # Next.js dev server (local)
pnpm build                   # OpenNext Cloudflare build (production build for CF Workers)
pnpm build:next              # Standard Next.js build (debugging only)
pnpm deploy                  # Deploy to Cloudflare Workers via wrangler
pnpm preview                 # Build + preview worker locally
pnpm lint                    # ESLint
pnpm cf-typegen              # Generate Cloudflare env types
```

### Docker Compose (full stack)
```bash
docker-compose up            # Postgres 16 + Redis 7 + API
```

---

## Code Style

### TypeScript (Scraper, Scripts, CLI, Dev, UI)
- Node 20+ ES2022, `NodeNext` module resolution
- Strict TypeScript, no `any` unless necessary
- `pnpm` for package management
- Workspace packages: `@openmeme/scraper`, `@openmeme/scripts`, `@openmeme/cli`, `@openmeme/dev`

### Java (API)
- Lombok required: `@Data`, `@Value`, `@Builder`, `@Slf4j`
- No ternary operators — use `Optional<T>`
- Controllers delegate only; services own business logic
- OpenAPI spec (`src/main/resources/openapi.yaml`) is source of truth
- Repository layer uses raw `JdbcTemplate` (no ORM)

### TypeScript / Next.js (Web)
- App Router, Server Components by default
- CSS Modules for component-scoped styles
- `next-intl` for i18n (default: `es-AR`, supported: `en`, `es`, `es-AR`, `pt`, `fr`, `de`, `ar`)
- Arabic (`ar`) renders RTL
- ISR with `revalidate` (300s–3600s) and fetch cache tags
- Deployed via `@opennextjs/cloudflare` adapter as a Cloudflare Worker

---

## Testing

| Suite | Command | Details |
|-------|---------|---------|
| Scraper | `cd packages/scraper && pnpm test` | Vitest |
| Design System | `cd packages/design-system && pnpm test` | Vitest |
| API | `cd apps/api && mvn test` | JUnit 5, Mockito, AssertJ, Testcontainers (PostgreSQL 16) |
| Web | `cd apps/web && pnpm lint` | ESLint (linting only — no build test) |
| Web (build) | `cd apps/web && pnpm build` | OpenNext Cloudflare build (requires network and Cloudflare config) |

### API Test Coverage
- `MemesControllerTest` — Mocked service layer; locale param resolution, 404s, pagination
- `AdminControllerTest` — Auth filter (401/200)
- `MemeRepositoryTest` — Real PostgreSQL container; upserts, idempotency, search, pagination
- `SchemaSmokeTest` — Validates extensions, enums, domains, materialized views, SQL functions
- `IndexerServiceTest` — MDX parsing (V2 & flat), locale grouping, normalization, async dispatch

---

## Security

- Admin API (`/admin/*`) protected by Spring Security + `ApiKeyAuthenticationFilter` with DB-backed key hashing
- Request/response headers are logged in full (no redaction); body values are masked for sensitive headers (`X-Api-Key`, `Authorization`)
- AI CLI processes (`claude`, `codex`) require installed + authenticated CLIs
- `.env` is gitignored — never commit it
- **Never scan `memes/` in bulk** — thousands of images will exhaust context

---

## Deployment

| Component | Method |
|-----------|--------|
| API | Docker multi-stage → Railway/Render |
| Web | OpenNext Cloudflare adapter → Cloudflare Workers |
| CDN | Cloudflare Worker for meme images |
| CI | GitHub Actions reindexes memes on push to `main` |
| CD | GitHub Actions deploys web to Cloudflare Workers on push to `main` |

### CI/CD Details

**Index memes workflow** (`.github/workflows/index-memes.yml`) — triggers on pushes to `main` that change `memes/**`:
1. Detects changed `.mdx` files (including locale variants like `slug.es-AR.mdx`)
2. Parses YAML frontmatter
3. Deduplicates by base stem
4. POSTs JSON payload to `/admin/reindex` with `X-Api-Key` header
5. Requires secrets: `API_BASE_URL`, `ADMIN_API_KEY`

**Deploy web workflow** (`.github/workflows/deploy-web.yml`) — triggers on pushes to `main` that change `apps/web/**`, `packages/ui/**`, or `packages/design-system/**`:
1. Installs dependencies
2. Builds web app with `opennext build` (OpenNext Cloudflare adapter)
3. Deploys to Cloudflare Workers via `wrangler deploy`
4. Requires secrets: `CLOUDFLARE_API_TOKEN`, `CLOUDFLARE_ACCOUNT_ID`, `NEXT_PUBLIC_MEMES_API_URL`, `MEMES_API_KEY`, `NEXT_SERVER_ACTIONS_ENCRYPTION_KEY`

---

## Key Conventions

| Convention | Detail |
|------------|--------|
| Deduplication | Bloom filter for URLs/post IDs; SHA1 for content |
| State file | `processed.bloom` (~360 KB fixed) |
| Git branching | `memes/{subreddit}-{YYYYMMDD-HHMMSS}` |
| MDX format | `title`, `description`, `author`, `subreddit`, `category`, `slug`, `score`, `created_at`, `source_url`, `post_url`, `image`, `tags` |
| MDX localization | Base: `{slug}.mdx` (English); translations: `{slug}.{locale}.mdx` (e.g., `slug.es-AR.mdx`) |
| API caching | Redis cache names suffixed with `-v2`: `stats-v2`, `categories-v2`, `meme-list-v2`, `meme-v2`, `search-v2` |
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
| API | `PORT` (default `8080`), `DB_URL`, `DB_USER`, `DB_PASSWORD`, `REDIS_HOST`, `REDIS_PORT`, `CDN_URL` |
| Scripts | `BATCH_SIZE`, `DRY_RUN`, `PER_POST`, `FROM_FILE`, `POST_URL` |
| Sync | `SYNC_SUBREDDITS`, `SYNC_LIMIT`, `SYNC_BATCH_SIZE`, `SYNC_CLASSIFIER`, `SYNC_CLASSIFY_WORKERS`, `SYNC_MIN_COMMENT_UPVOTES`, `SYNC_DRY_RUN`, `SYNC_TIME` |
| Optimize | `OPTIMIZE_DRY_RUN`, `OPTIMIZE_RESIZE` |
| Web | `NEXT_PUBLIC_MEMES_API_URL`, `MEMES_API_KEY`, `NEXT_SERVER_ACTIONS_ENCRYPTION_KEY`, `NEXT_PUBLIC_POSTHOG_KEY`, `NEXT_PUBLIC_POSTHOG_HOST` |
| Cloudflare | `CLOUDFLARE_API_TOKEN`, `CLOUDFLARE_ACCOUNT_ID` |
