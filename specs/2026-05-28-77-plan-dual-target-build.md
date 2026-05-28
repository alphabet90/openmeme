# Spec: Dual-Target Build Strategy — Vercel (Previews/Staging) + Cloudflare Workers (Production)

**Issue:** [alphabet90/openmeme#77](https://github.com/alphabet90/openmeme/issues/77)  
**Date:** 2026-05-28  
**Status:** Planning  
**Branch:** `looper/planner/77-plan-dual-target-build`  
**Base Branch:** `main`

---

## 1. Problem

The web frontend (`apps/web`) needs to deploy to **two platforms** with different build requirements:

- **Vercel** — branch previews and staging environments. Vercel expects the standard `next build` output and runs `turbo run build` (or the package's `build` script) automatically.
- **Cloudflare Workers** — production deployment via GitHub Actions. Requires `@opennextjs/cloudflare` adapter, which produces a Cloudflare Workers-compatible bundle.

If the `build` script in `apps/web/package.json` is set to `opennextjs-cloudflare build`, the adapter internally executes `pnpm build` (via `child_process.execSync`) to compile the Next.js app. Because `build` is aliased to `opennextjs-cloudflare build`, the process loops indefinitely:

1. `opennextjs-cloudflare build` spawns `pnpm build`.
2. `pnpm build` runs `opennextjs-cloudflare build` again.
3. Repeat until OOM (`SIGKILL` / exit code 137).

This blocks **both** the Vercel preview pipeline and the Cloudflare production pipeline simultaneously. The recursive build fails on Vercel after ~43 min and on GitHub Actions after ~3.5 min — both due to OOM.

Additionally, runtime configuration differences between the two platforms (edge runtime, image optimization, caching layer) could cause subtle behavioral differences if not explicitly managed.

---

## 2. Goals

1. Restore Vercel preview/staging builds to work with the standard `next build` script.
2. Enable Cloudflare Workers production builds via a separate, explicit adapter invocation (`build:cf` or workflow-level step).
3. Eliminate the recursive build trap entirely — no script in the monorepo should invoke itself transitively.
4. Keep the default `build` script platform-agnostic (`next build`) so that local dev, CI linting, and Vercel all use the same command.
5. Ensure developers do not need to remember manual steps or comment/uncomment scripts when switching targets.
6. Handle platform-specific runtime configuration (environment variables, image optimization, caching) explicitly.

---

## 3. Non-Goals

- Migrating away from Vercel entirely (Vercel remains the preview/staging platform).
- Migrating away from Cloudflare Workers (Cloudflare remains the production platform).
- Re-architecting the Next.js app to avoid adapter differences — the app should use standard Next.js APIs that both platforms support.
- Adding a third deployment target (no AWS Lambda, no self-hosted, etc.).
- Production traffic migration or cut-over planning — this spec covers only the build and deploy pipeline.

---

## 4. Approach

### 4.1 Restore Standard Next.js Build as Default

Update `apps/web/package.json` scripts:

```json
{
  "scripts": {
    "dev": "next dev",
    "build": "next build",
    "build:cf": "opennextjs-cloudflare build",
    "start": "next start",
    "lint": "eslint"
  }
}
```

- `build` — standard Next.js output used by Vercel, local dev, and CI.
- `build:cf` — Cloudflare Workers adapter build, invoked only in the production deploy workflow.

Add `@opennextjs/cloudflare` (currently `1.19.11`) to `devDependencies`.

### 4.2 Add Cloudflare Deploy Workflow

Create `.github/workflows/deploy-web.yml` with a **two-phase build**:

1. **If `@opennextjs/cloudflare` wraps `next build` internally** (i.e., it expects `next build` to have already run, or runs it itself):
   - Phase 1: `pnpm --filter @openmeme/web build` → produces `.next/`.
   - Phase 2: `pnpm --filter @openmeme/web build:cf` → wraps/converts `.next/` into Cloudflare bundle (`.open-next/`).

2. **If `@opennextjs/cloudflare` replaces `next build` entirely** (produces the final artifact in one step):
   - Single step: `pnpm --filter @openmeme/web build:cf`.

The exact invocation order must be confirmed by testing (see Risks section). The workflow should also include:
- `wrangler deploy` or `wrangler publish` step after the build.
- Appropriate caching for `node_modules`, `.next`, and `.open-next`.

### 4.3 Platform-Specific Configuration

Next.js configuration may diverge between platforms. Extract platform-specific overrides:

```js
// next.config.ts
const baseConfig = { /* shared config */ };

if (process.env.VERCEL) {
  // Vercel-specific: standard ISR, default image optimization
} else if (process.env.CF_PAGES || process.env.CLOUDFLARE_WORKERS) {
  // Cloudflare-specific: edge runtime constraints, no Node.js image optimization
}

export default baseConfig;
```

Alternatively, use separate config files that extend a base:

```
next.config.ts            # base (shared)
next.config.vercel.ts     # Vercel overrides
next.config.cloudflare.ts # Cloudflare overrides
```

The workflow selects the right config via `NEXT_CONFIG_FILE` or by passing `--config` to the build command (if the adapter supports it). For V1, a single config with `process.env` branching is preferred for simplicity.

### 4.4 Exclude Platform-Specific Dependencies

- **Vercel deps**: `@vercel/analytics`, `@vercel/speed-insights` — keep in `dependencies`; no issue if absent on Cloudflare.
- **Cloudflare deps**: `wrangler`, `@opennextjs/cloudflare` — add to `devDependencies`; won't be installed on Vercel because Vercel only runs `pnpm install --prod` or uses its own build container with selective installs.

If Vercel's build container installs all `devDependencies` by default, add an `.npmrc` or `pnpm-workspace.yaml` exclusion:

```
# apps/web/.npmrc
onlyBuiltDependencies=[]  # or exclude specific packages
```

Alternatively, move Cloudflare-only deps to the deploy workflow's `package.json` or install them in the CI step directly (`pnpm add -D @opennextjs/cloudflare wrangler` in the workflow).

### 4.5 Output Directory Hygiene

- `next build` outputs to `.next/`.
- `opennextjs-cloudflare build` outputs to `.open-next/`.
- These directories should not conflict. Ensure `.gitignore` ignores both:

```
.next/
.open-next/
```

Add `.open-next/` to `.gitignore` if not already present.

### 4.6 Vercel Project Configuration

In `vercel.json` (or Vercel dashboard):

```json
{
  "buildCommand": "pnpm --filter @openmeme/web build",
  "installCommand": "pnpm install",
  "framework": "nextjs"
}
```

No custom `buildCommand` is needed if the monorepo root's `turbo.json` correctly maps the `build` pipeline. Vercel's default monorepo detection runs `turbo run build` which invokes each workspace's `build` script. Since `@openmeme/web`'s `build` is now `next build`, it will work natively.

---

## 5. Affected Components

| Component | Change |
|-----------|--------|
| `apps/web/package.json` | Rename `build` script; add `build:cf` script; add `@opennextjs/cloudflare` devDependency |
| `.github/workflows/deploy-web.yml` | New workflow: Cloudflare Workers deploy with two-phase or single-phase build |
| `apps/web/next.config.ts` | Add platform-aware branching for Vercel vs Cloudflare |
| `.gitignore` | Add `.open-next/` directory |
| `AGENTS.md` | Document dual-target build strategy and runbook |

---

## 6. Risks & Mitigations

| Risk | Likelihood | Impact | Mitigation |
|------|------------|--------|------------|
| `@opennextjs/cloudflare` calls `pnpm build` internally, re-triggering recursion if not renamed to `build:cf` | Medium | High | Rename `build` to `next build`; keep `build:cf` as the adapter script; verify adapter does not invoke `pnpm build:cf`. |
| Output directory conflict (`.next` vs `.open-next`) pollutes build cache | Low | Medium | Add `.open-next/` to `.gitignore`; configure CI cache keys to separate `.next` and `.open-next`. |
| Vercel installs `@opennextjs/cloudflare` unnecessarily, increasing build time | Medium | Low | Move to `devDependencies`; Vercel skips devDeps in production installs. If still installed, use `.npmrc` exclusions or conditional install in workflow. |
| Runtime behavior differs between Vercel (Node.js) and Cloudflare (Workers/Edge) | Medium | High | Add smoke tests that run against the deployed Cloudflare Worker; use feature flags to disable incompatible features on Cloudflare. |
| `turbo.json` pipeline order causes unexpected `build:cf` execution | Low | Medium | Do not add `build:cf` to the `turbo.json` pipeline — only invoke it explicitly in the deploy workflow. |

### Open Question: Build Order

The critical unknown is whether `@opennextjs/cloudflare build`:

**Option A** — wraps `next build` internally:
- You run `opennextjs-cloudflare build` → it runs `next build` inside, then wraps the output.
- In this case, the deploy workflow needs only `build:cf` (single step).
- Risk: if the adapter spawns `pnpm build` (not `next build` directly), we must ensure the `build` script is `next build`, not the adapter itself.

**Option B** — requires pre-built `.next/`:
- You run `next build` first, then `opennextjs-cloudflare build` wraps the existing `.next/` output.
- In this case, the deploy workflow needs both `build` and `build:cf` (two steps).

**Resolution**: Test both options in a CI branch or local environment. Update this spec with the confirmed order before implementation.

---

## 7. Validation

### 7.1 Vercel Preview

- Push a branch to GitHub; Vercel creates a preview deployment automatically.
- Verify the build log shows a single `next build` invocation (no recursion).
- Verify the preview URL loads without errors.

### 7.2 Cloudflare Production (GitHub Actions)

- Push to `main` or trigger the `deploy-web.yml` workflow manually.
- Verify the build log shows the expected sequence (either single `build:cf` or `build` + `build:cf`).
- Verify `wrangler deploy` succeeds.
- Verify the production URL loads without errors.

### 7.3 Local Smoke Test

```bash
# Standard build
pnpm --filter @openmeme/web build
ls .next/  # should exist

# Cloudflare adapter build
pnpm --filter @openmeme/web build:cf
ls .open-next/  # should exist (if adapter produces this directory)
```

### 7.4 Platform-Specific Feature Parity

- Deploy the same branch to both Vercel preview and Cloudflare staging/production.
- Compare: homepage loads, dynamic routes work (e.g., `/meme/[slug]`), i18n locale switching, image rendering, API route proxying.
- Verify that any platform-gated features (e.g., Vercel Analytics vs no-op on Cloudflare) behave correctly.

---

## 8. Execution Plan

### Phase 1: Package.json & Config (1 PR)

1. Rename `build` → `build:cf` in `apps/web/package.json`; set `build` → `next build`.
2. Add `@opennextjs/cloudflare` to `devDependencies`.
3. Add `.open-next/` to `.gitignore`.
4. Add platform-aware branching to `next.config.ts`.
5. Verify `pnpm --filter @openmeme/web build` completes locally.

**Deliverable**: Vercel preview/staging builds are restored.

### Phase 2: Deploy Workflow (same PR, separate commit)

1. Create `.github/workflows/deploy-web.yml` with the confirmed build order.
2. Add `wrangler` install and `wrangler deploy` steps.
3. Configure caching for `node_modules`, `.next`, `.open-next`.
4. Trigger workflow and verify end-to-end.

**Deliverable**: Cloudflare production deploys work.

### Phase 3: Documentation & Rollout (same PR, separate commit)

1. Update `AGENTS.md` with dual-target runbook.
2. Merge the PR to `main`.
3. Monitor first Vercel preview and first Cloudflare production deploy after merge.

**Deliverable**: Developers and CI can target both platforms without manual intervention.

---

## 9. User Stories

1. As a **developer opening a PR**, I want Vercel to automatically build a preview of my branch using the standard Next.js build, so that I can verify my changes in a live environment.
2. As a **devops engineer** maintaining the deploy pipeline, I want the Cloudflare Workers production build to use a dedicated adapter script (`build:cf`), so that it does not interfere with Vercel or local builds.
3. As a **developer running `pnpm build` locally**, I want the command to produce a standard Next.js output (`.next/`), so that I can test and debug without Cloudflare-specific tooling.
4. As a **reviewer** of deploy workflow changes, I want the build order to be explicit and documented, so that I can understand the adapter's requirements without running it myself.
