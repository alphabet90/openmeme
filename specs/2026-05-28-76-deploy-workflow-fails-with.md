# Spec: Fix Deploy Workflow Infinite Build Recursion (OOM)

**Issue:** [alphabet90/openmeme#76](https://github.com/alphabet90/openmeme/issues/76)  
**Date:** 2026-05-28  
**Status:** Planning  
**Branch:** `looper/planner/76-deploy-workflow-fails-with`  
**Base Branch:** `main`

---

## 1. Problem

The proposed **Deploy Web** GitHub Actions workflow (`deploy-web.yml`), introduced during the Cloudflare migration on a feature branch, fails due to an infinite build recursion that exhausts the runner's memory and triggers the OOM killer (SIGTERM / exit code 137). This blocks all production deployments for the web app.

### Root Cause

The `@opennextjs/cloudflare` adapter's CLI (`opennextjs-cloudflare build`) internally calls `pnpm build` via `child_process.execSync`. When `apps/web/package.json` has `"build": "opennextjs-cloudflare build"`, this creates a circular dependency:

1. `pnpm --filter @openmeme/web build` → runs `opennextjs-cloudflare build`
2. `opennextjs-cloudflare build` → internally runs `pnpm build`
3. Step 2 re-triggers Step 1 → infinite loop

Hundreds of parallel Node processes are spawned until the runner hits its memory limit, at which point the kernel sends SIGTERM (exit code 137) and GitHub Actions cancels the job.

### Impact

- **Deployments completely blocked** — every push to `main` that triggers the web app deploy fails.
- **Wasted CI minutes** — the job runs for ~3.5 minutes before being killed, consuming GitHub Actions resources.
- **No clear error feedback** — logs show a cascade of identical recursive builds, making diagnosis difficult without inspecting process signals.

### Key Log Excerpt

```
> @openmeme/web@0.1.0 build /home/runner/work/openmeme/openmeme/apps/web
> opennextjs-cloudflare build

... (repeats 100+ times) ...

Killed
Error: Command failed: pnpm build
  signal: 'SIGTERM',
  pid: 3180,

##[error]The runner has received a shutdown signal.
```

---

## 2. Goals

1. Resolve the infinite build recursion so the Deploy Web workflow completes successfully.
2. Ensure the Cloudflare Workers deploy step receives the correct build output.
3. Keep the developer experience intact — local builds and other CI workflows must not break.

---

## 3. Fast-Follow / Future Work

- **Recursion safeguard**: Add a timeout or recursive-call detection to prevent recurrence if the team keeps using the `opennextjs-cloudflare` CLI. This is tracked in Approach 5.3 as an optional fast-follow item.

---

## 4. Non-Goals

- Migrating away from Cloudflare Workers as a deployment target.
- Adding a full staging/preview environment (can be addressed separately).
- Changing the build tooling for other workspace packages (scraper, design-system, etc.).
- Performance optimization of the Next.js build itself (only addressing the recursion).

---

## 5. Approach

### 5.1 Fix `apps/web/package.json` — Break the Circular Dependency

The `build` script must be changed to invoke `next build` directly, not the adapter CLI. The adapter wrapper is moved to a separate `build:cf` script for local testing:

| Before | After |
|--------|-------|
| `"build": "opennextjs-cloudflare build"` | `"build": "next build"` |
| — | `"build:cf": "opennextjs-cloudflare build"` |

This breaks the cycle because `next build` produces the standard Next.js output without re-invoking `pnpm build`.

### 5.2 Create/Update `.github/workflows/deploy-web.yml`

The deploy workflow must not call `pnpm build` (which would now be `next build`) and then call the adapter separately. Instead:

1. Run `pnpm build` (standard `next build`) to produce the `.next` output.
2. Run `opennextjs-cloudflare build` as a **post-build step** to transform `.next` into the Cloudflare Workers bundle.
3. Deploy via `cloudflare/wrangler-action`.

```yaml
name: Deploy Web

on:
  workflow_dispatch:
  push:
    branches: [main]
    paths:
      - "apps/web/**"
      - "packages/ui/**"
      - "packages/design-system/**"

jobs:
  deploy:
    runs-on: ubuntu-24.04
    timeout-minutes: 15
    defaults:
      run:
        working-directory: apps/web

    steps:
      - uses: actions/checkout@v4

      - uses: pnpm/action-setup@v4
        with:
          version: 10.33.0

      - uses: actions/setup-node@v4
        with:
          node-version: 20.20.2
          cache: pnpm

      - run: pnpm install --frozen-lockfile

      - name: Build Next.js app
        run: pnpm build
        # Runs "next build" — no circular dependency.

      - name: Build Cloudflare adapter bundle
        run: pnpm exec opennextjs-cloudflare build
        # Transforms .next output into the Workers-compatible bundle.

      - name: Deploy to Cloudflare Workers
        uses: cloudflare/wrangler-action@v3
        with:
          apiToken: ${{ secrets.CF_API_TOKEN }}
          workingDirectory: apps/web
```

**Key design decisions:**

- The adapter is invoked via `pnpm exec` (which uses the locally installed devDependency `@opennextjs/cloudflare`) rather than as a `build` script, ensuring it runs exactly once with the pinned version.
- The timeout is set to 15 minutes, well above the expected build time (~3 minutes), so the OOM scenario is prevented by recursion elimination rather than a timeout guard.
- The working directory is `apps/web` so that `wrangler.toml` (if present) is found automatically.

### 5.3 Add Recursion Safeguard (Fast-Follow)

If the team wants to keep using `opennextjs-cloudflare build` as the primary build command, add a guard script that detects recursive invocation:

```json
{
  "scripts": {
    "build": "node -e \"if(process.env.OPENNEXT_BUILD) { console.error('Recursive build detected'); process.exit(1); } process.env.OPENNEXT_BUILD='1'\" && opennextjs-cloudflare build"
  }
}
```

This is a fast-follow item (Acceptance Criteria checkbox) and is optional for the main fix.

### 5.4 Verify Build Output

The `next build` output goes to `.next/`. The `opennextjs-cloudflare build` step reads `.next/` and produces the Cloudflare Workers bundle. The `wrangler-action` deploy step reads that bundle. No additional configuration changes are needed if `wrangler.toml` exists and points to the correct output directory.

---

## 6. Affected Components

| Component | Change |
|-----------|--------|
| `apps/web/package.json` | Change `build` script to `"next build"`, add `"build:cf": "opennextjs-cloudflare build"` |
| `apps/web/package.json` (dependencies) | Add `@opennextjs/cloudflare` as a devDependency if not already present |
| `.github/workflows/deploy-web.yml` | Create new workflow file with correct build sequence |
| `apps/web/wrangler.toml` | Create if not exists (Cloudflare Workers config) |

---

## 7. Risks & Mitigations

| Risk | Likelihood | Impact | Mitigation |
|------|------------|--------|------------|
| `next build` output is incompatible with `opennextjs-cloudflare` expectations | Low | High | Verify adapter docs; test locally before CI; the adapter is designed to consume standard `.next/` output. |
| Missing `wrangler.toml` causes deploy step to fail | Medium | High | Include `wrangler.toml` creation in scope; use `wrangler-action` with explicit `apiToken`. |
| Other CI workflows (index-memes, etc.) that call `pnpm build` at root level break | Low | Medium | Root `pnpm build` delegates to TurboRepo which runs each package's `build` script independently. Changing only `apps/web/package.json` will not affect other packages. |
| Local dev workflow breaks for developers who used `pnpm build` as the adapter command | Medium | Low | Document the new `build:cf` script; existing `pnpm dev` (next dev) is unaffected. |

---

## 8. Validation

### 8.1 Local Build Verification

```bash
cd apps/web
pnpm build                  # Must run "next build" without recursion
pnpm build:cf               # Must run "opennextjs-cloudflare build" successfully
```

### 8.2 CI Simulation

The workflow must be tested by pushing to a non-`main` branch and manually triggering the workflow (or using `workflow_dispatch`), verifying:

- `pnpm build` completes without spawning child processes recursively.
- The `opennextjs-cloudflare build` step exits 0.
- The full job completes within the 15-minute timeout.

### 8.3 Dry-Run Deploy

Use `wrangler-action` with `command: 'deploy --dry-run'` (or equivalent) to verify the deploy step would succeed without actually deploying.

### 8.4 Regression Checks

- `pnpm dev` still starts the Next.js dev server correctly.
- `pnpm lint` still passes.
- `pnpm test` in related packages (ui, design-system) still passes.

### 8.5 What NOT to Test

- Do not test extreme memory pressure scenarios beyond standard CI limits (eliminating the recursion is sufficient).
- Do not test the OpenNext adapter's internal build correctness (it is a third-party tool assumed to work correctly when invoked once).

---

## 9. Further Notes

- **Why not use `opennextjs-cloudflare build` directly?** The adapter internally calls `pnpm build` which is the root cause of the recursion. The fix separates the Next.js build from the adapter transformation.
- **Why run the adapter as a separate step?** This makes the CI pipeline explicit and debuggable. If the adapter fails, the `.next/` output is still available for inspection from the build step.
- **Existing workflows unaffected**: The `index-memes.yml` workflow triggers only on `memes/**` changes and does not build the web app, so it is unaffected by this change.
- **Cloudflare Workers configuration**: If `wrangler.toml` does not exist, create one with the template below. This is a pre-requisite for the deploy step.

```toml
name = "openmeme-web"
compatibility_date = "2026-05-15"
main = ".opennext/worker"
```

---

## 10. User Stories

1. As a **deploy engineer**, I want the Deploy Web workflow to exit successfully so that production deployments are unblocked.
2. As a **developer**, I want `pnpm build` in `apps/web` to produce a standard Next.js build without triggering the adapter, so that local development and CI are predictable.
3. As a **reviewer**, I want clear separation between the Next.js build and the Cloudflare adapter step in CI, so that build failures can be diagnosed quickly.
