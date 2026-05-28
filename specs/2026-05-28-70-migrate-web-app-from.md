# Spec: Migrate web app from Vercel to Cloudflare Workers via \@opennextjs/cloudflare

| Field | Value |
|-------|-------|
| **Issue** | [alphabet90/openmeme#70](https://github.com/alphabet90/openmeme/issues/70) |
| **Branch** | `looper/planner/70-migrate-web-app-from` |
| **Base** | `main` |
| **Date** | 2026-05-28 |
| **Estimate** | M (2--3 days, including testing and documentation) |

---

## Problem

The OpenMeme web app (`apps/web/`) is a **Next.js 16.2.4** application currently deployed on **Vercel**. This incurs unnecessary hosting costs and does not leverage our existing Cloudflare edge infrastructure (memes are already served via Cloudflare). The packages `@opennextjs/cloudflare` (^1.19.11) and `wrangler` (^4.95.0) already exist in our devDependencies but are unused, indicating the migration was planned but never completed.

## Goals

1. Build the Next.js app with the OpenNext Cloudflare adapter instead of the standard `output: "standalone"` build.
2. Deploy to Cloudflare Workers via `wrangler deploy`.
3. Preserve all existing functionality: meme listing, search, locale switching, ISR/SSG behavior, and i18n routing.
4. Map all environment variables to Cloudflare-compatible secrets/vars.
5. Evaluate and either replace or confirm compatibility of `@trieb.work/nextjs-turbo-redis-cache`.
6. Audit API routes, middleware, and server components for Node.js-only APIs.
7. Update CI/CD pipeline and documentation.

## Approach

### 1. Audit current next.config.ts for incompatible settings

**Current config** (`apps/web/next.config.ts`):
- `output: "standalone"` -- incompatible with OpenNext Cloudflare adapter; must be removed.
- `images: { unoptimized: true }` -- compatible, keep as-is.
- `async headers()` with `X-Accel-Buffering` -- evaluate if needed in Workers environment; probably safe to remove or keep (no-op if upstream ignores it).
- `next-intl` plugin wrapping -- must verify compatibility with the worker build.

**Action:** Remove `output: "standalone"`, add `@opennextjs/cloudflare` config wrapper per the playground16 example.

### 2. Create wrangler.jsonc

Based on the [playground16 example](https://github.com/opennextjs/opennextjs-cloudflare/tree/main/examples/playground16), create `apps/web/wrangler.jsonc`:

```jsonc
{
  "name": "openmeme-web",
  "main": ".open-next/worker.js",
  "compatibility_date": "2026-05-28",
  "compatibility_flags": ["nodejs_compat"],
  "assets": {
    "directory": ".open-next/assets"
  },
  "routes": [
    { "pattern": "openmeme.io", "custom_domain": true }
  ],
  "observability": {
    "enabled": true
  }
}
```

Key considerations:
- `compatibility_flags: ["nodejs_compat"]` enables Node.js API polyfills (needed for `crypto`, `fs`, etc. in server components).
- `main` entry points to the OpenNext-generated worker bundle at `.open-next/worker.js`.
- Assets directory serves static files (images, fonts, etc.) from the build output.

### 3. Update package.json scripts

**Current scripts** (`apps/web/package.json`):
```json
{
  "dev": "next dev",
  "build": "next build",
  "start": "next start",
  "lint": "eslint"
}
```

**Target scripts:**
```json
{
  "dev": "next dev",
  "build": "opennextjs-cloudflare build",
  "start": "wrangler dev",
  "deploy": "wrangler deploy",
  "lint": "eslint",
  "cf-typegen": "wrangler types"
}
```

- `build` now uses the OpenNext Cloudflare adapter instead of raw `next build`.
- `start` runs the local Workers simulator via `wrangler dev`.
- `deploy` pushes to Cloudflare Workers.
- Add `@opennextjs/cloudflare` and `wrangler` as devDependencies (or confirm they exist in the workspace root).

### 4. Environment variables mapping

| Variable | Scope | Current usage | Cloudflare mapping |
|----------|-------|---------------|--------------------|
| `NEXT_PUBLIC_MEMES_API_URL` | Public | `lib/site.ts` -- API base URL | `wrangler secret put NEXT_PUBLIC_MEMES_API_URL` |
| `NEXT_PUBLIC_POSTHOG_KEY` | Public | `app/providers.tsx` -- PostHog analytics | `wrangler secret put NEXT_PUBLIC_POSTHOG_KEY` |
| `MEMES_API_KEY` | Server | `lib/api.ts` -- API auth header | `wrangler secret put MEMES_API_KEY` |
| `NEXT_SERVER_ACTIONS_ENCRYPTION_KEY` | Server | Server Actions encryption | `wrangler secret put NEXT_SERVER_ACTIONS_ENCRYPTION_KEY` |

For public variables prefixed with `NEXT_PUBLIC_*`, they can be inlined at build time via `wrangler.jsonc` `[vars]` block or passed as build args. Server-side secrets must use `wrangler secret put`.

### 5. Cache strategy: replace @trieb.work/nextjs-turbo-redis-cache

**Current state:**
- `cache-handler.js` wraps `RedisStringsHandler` from `@trieb.work/nextjs-turbo-redis-cache`.
- `next.config.ts` does **not** currently configure `cacheHandler` (the handler file exists but may not be wired in).

**Assessment:**
- `@trieb.work/nextjs-turbo-redis-cache` expects a Redis connection via `REDISHOST`/`REDISPORT` env vars. It depends on Node.js `net`/`tls` modules and is **not compatible** with Cloudflare Workers out of the box.
- The OpenNext Cloudflare adapter handles ISR cache via Cloudflare KV automatically (using the Workflows + KV integration). Manual Redis caching is unnecessary.

**Action:**
- Remove `@trieb.work/nextjs-turbo-redis-cache` from `apps/web/package.json`.
- Delete `apps/web/cache-handler.js`.
- Ensure `next.config.ts` does not reference a `cacheHandler` path.

### 6. Audit API routes, middleware, and server components

**API routes:** No `app/api/` route handlers exist. This is a purely static + ISR app consuming the external Java API. No migration concern.

**Middleware** (`apps/web/middleware.ts`):
- Uses `next-intl/middleware` (based on `@formatjs/intl-localematcher`). The OpenNext Cloudflare adapter supports Next.js middleware in Workers. No changes expected, but must be verified in staging.

**Server components:**
- `lib/api.ts` uses `fetch()` with `next: { revalidate }` for ISR -- compatible with Workers (Cloudflare's `fetch` and `cache:` options are supported).
- `lib/site.ts` reads `process.env.NEXT_PUBLIC_MEMES_API_URL` -- compatible with `nodejs_compat` flag.
- `app/providers.tsx` uses `posthog-js` (client-side only) -- no server impact.
- Check for any `fs`, `crypto`, or `path` usage in server components. If found, `nodejs_compat` flag should polyfill these.

**Action:** Enable `nodejs_compat` compatibility flag in `wrangler.jsonc`. Run a build and check for any missing polyfill errors.

### 7. ISR / SSG behavior under @opennextjs/cloudflare

The OpenNext Cloudflare adapter supports:
- **Static Generation (SSG):** Fully supported. Static pages are pre-rendered at build time and served from Cloudflare's static assets.
- **Incremental Static Regeneration (ISR):** Handled via Cloudflare KV. Pages with `revalidate` are rendered on first miss, stored in KV, and served on subsequent requests. This replaces the previous Redis-based caching.
- **Server-Side Rendering (SSR):** Supported via the Workers runtime.

**Trade-offs:**
- ISR cache is now KV-backed instead of Redis-backed. KV has eventual consistency (up to 60s propagation). This is acceptable for meme content.
- KV has a 25 MB limit per value; individual cached pages will be well under this.
- Free tier KV includes 1 GB storage and 10 million reads/day -- sufficient for current scale.

**Action:** No code changes needed for ISR/SSG. The adapter handles it automatically. Document the KV dependency and account limits.

### 8. i18n (next-intl) compatibility

**Current setup:**
- `i18n/routing.ts` defines 7 locales with `localePrefix: "always"`.
- `i18n/request.ts` async-loads JSON message files.
- `middleware.ts` uses `createMiddleware(routing)` for locale detection.
- All locale segments in App Router: `app/[locale]/`.

**Verification needed:**
1. Build with OpenNext adapter and confirm locale-based routes (`/en/memes`, `/es-AR/memes`, etc.) work.
2. Confirm `createMiddleware` runs correctly in Workers runtime (no edge-runtime incompatibilities).
3. Confirm async dynamic imports (`import(../messages/${locale}.json)`) resolve correctly in the worker bundle.

**Expected outcome:** `next-intl` is edge-runtime compatible and should work without changes. The playground16 example does not include i18n, but the middleware API is supported.

### 9. Deployment pipeline

**Current:** Vercel deploy (likely via GitHub integration). The existing `.github/workflows/index-memes.yml` only handles meme reindexing.

**New workflow** (`.github/workflows/deploy-web.yml`):

```yaml
name: Deploy Web

on:
  push:
    branches: [main]
    paths:
      - "apps/web/**"
      - "packages/ui/**"
      - "packages/design-system/**"
  workflow_dispatch:

jobs:
  deploy:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: pnpm/action-setup@v4
      - uses: actions/setup-node@v4
        with:
          node-version: 20
          cache: pnpm
      - run: pnpm install --frozen-lockfile
      - run: pnpm --filter @openmeme/web build
      - name: Deploy to Cloudflare Workers
        uses: cloudflare/wrangler-action@v3
        with:
          apiToken: ${{ secrets.CF_API_TOKEN }}
          workingDirectory: apps/web
          command: deploy
      - name: Invalidate CDN cache
        run: npx wrangler deploy --dry-run  # CDN purge handled by wrangler deploy
```

**Secrets required in GitHub:**
- `CF_API_TOKEN` -- Cloudflare API token with Workers Deploy permissions.
- `CF_ACCOUNT_ID` -- Cloudflare account ID (or read from `wrangler.jsonc`).

### 10. Remove Vercel-specific files

- `apps/web/Dockerfile` -- no longer needed for Vercel but may be retained for alternative deployments (document that Cloudflare is primary).
- `apps/web/k8s/` -- Kubernetes manifests, keep if still relevant for other deployment paths.
- `apps/web/.dockerignore` -- keep or remove after Dockerfile cleanup decision.

**Decision:** Keep Dockerfile and k8s/ for now (opt-in alternative deployments). Remove Vercel-specific configs if any exist (e.g., `vercel.json`).

## Risks & Mitigations

| Risk | Mitigation |
|------|------------|
| `@opennextjs/cloudflare` may not support Next.js 16.2.4 features (e.g., PPR, partial rendering) | Pin to the version used in the playground16 example; test with staging deploy before production cutover. |
| `next-intl` async locale resolution may not bundle correctly | Test locale switching in staging; pre-build messages into the worker bundle if dynamic imports fail. |
| ISR cache misses degrade performance under load | KV has low latency (~200ms reads); acceptable for meme content. Monitor cache hit rates in Cloudflare dashboard. |
| Node.js built-in usage in server components (e.g., `crypto`, `fs`) | Enable `nodejs_compat` flag. Run full build and test to catch missing polyfills. |
| Middleware matcher includes `_next` and `_vercel` paths | Update matcher to remove `_vercel` references (not needed on Cloudflare). |
| Wrangler deploy overwrites production on first deploy | Deploy to a staging worker (`openmeme-web-staging`) first; use `routes` or `preview` environments. |
| KV eventual consistency causes stale ISR pages | Set `revalidate` timeouts conservatively (300s+). Manual purge via Cloudflare dashboard if needed. |
| PostHog analytics breaks in Workers environment | PostHog client-side JS runs in the browser, not in the worker -- no impact. |

## Validation

1. **Build verification:**
   - `cd apps/web && pnpm build` produces a `.open-next/` directory with `worker.js` and `assets/`.
   - No errors or warnings about missing polyfills.
   - `pnpm lint` passes.

2. **Local worker simulation:**
   - `cd apps/web && pnpm start` (wrangler dev) serves the app at `http://localhost:8787`.
   - Home page loads with correct locale detection.
   - Meme listing page loads with correct pagination.
   - Search endpoint returns results.
   - Locale switching works for all 7 locales.
   - Arabic locale renders RTL correctly.

3. **Staging deployment:**
   - `wrangler deploy --env=staging` deploys to `openmeme-web-staging.workers.dev`.
   - All above checks pass on the staging URL.
   - ISR pages revalidate after `revalidate` timeout.

4. **Production deployment:**
   - `wrangler deploy` deploys to the production worker.
   - Custom domain (`openmeme.io`) routes correctly.
   - Cloudflare CDN cache is warm after first request wave.

5. **Env var verification:**
   - `wrangler secret list` shows all 4 secrets.
   - Public vars from `[vars]` in `wrangler.jsonc` are accessible.

## Affected Files

- `apps/web/next.config.ts` -- Remove `output: "standalone"`, add OpenNext adapter wrapper
- `apps/web/wrangler.jsonc` -- New file (Cloudflare Worker config)
- `apps/web/package.json` -- Updated scripts, removed `@trieb.work/nextjs-turbo-redis-cache`
- `apps/web/cache-handler.js` -- Delete (Redis cache handler, incompatible)
- `apps/web/middleware.ts` -- Update matcher regex (remove `_vercel`)
- `apps/web/Dockerfile` -- Keep or note as secondary deployment path
- `.github/workflows/deploy-web.yml` -- New file (Cloudflare deployment workflow)
- `apps/web/.env.example` -- Updated with notes on Cloudflare var mapping
- `AGENTS.md` -- Updated build/deploy commands

## Definition of Done

- [ ] Build target changed from `standalone` to OpenNext Cloudflare adapter.
- [ ] `wrangler.jsonc` exists and correctly configured.
- [ ] All 4 env vars mapped and working as Cloudflare secrets/vars.
- [ ] `@trieb.work/nextjs-turbo-redis-cache` removed, `cache-handler.js` deleted.
- [ ] Middleware matcher updated (no `_vercel` references).
- [ ] No Node.js built-in compatibility errors in build output.
- [ ] Locale switching works for all 7 locales in worker environment.
- [ ] CD pipeline deploys to Cloudflare Workers on push.
- [ ] Staging smoke tests pass (home, meme list, search, locale switch).
- [ ] Documentation updated (AGENTS.md, package.json scripts).
- [ ] No regressions in lint or existing functionality.
