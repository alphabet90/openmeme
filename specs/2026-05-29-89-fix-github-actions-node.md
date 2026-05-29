# Spec: Fix GitHub Actions Node.js 20 deprecation warnings in deploy-web workflow

| Field | Value |
|-------|-------|
| **Issue** | [alphabet90/openmeme#89](https://github.com/alphabet90/openmeme/issues/89) |
| **Branch** | `looper/planner/89-fix-github-actions-node` |
| **Base** | `main` |
| **Date** | 2026-05-29 |
| **Estimate** | XS (1 hour, version bumps only) |

---

## Problem

The [deploy-web workflow run #26641901831](https://github.com/alphabet90/openmeme/actions/runs/26641901831) shows deprecation warnings because several GitHub Actions are still pinned to versions that run on the **Node.js 20** runtime.

> Node.js 20 actions are deprecated. The following actions are running on Node.js 20 and may not work as expected: actions/checkout@v4, actions/setup-node@v4, cloudflare/wrangler-action@v3, pnpm/action-setup@v4. Actions will be forced to run with Node.js 24 by default starting June 16th, 2026. Node.js 20 will be removed from the runner on September 16th, 2026.

## Goals

1. Update all four affected actions in `.github/workflows/deploy-web.yml` to versions that run on Node.js 24.
2. Update the `node-version` input in `actions/setup-node` from `20` to `24` (or `lts/*`).
3. Optionally apply the same `actions/checkout` version bump to `.github/workflows/index-memes.yml` for consistency (that workflow does not use the other three actions).

## Approach

### 1. Research updated action versions

The following compatible versions exist as of 2026-05-29:

| Action | Current | Target | Runtime | Notes |
|--------|---------|--------|---------|-------|
| `actions/checkout` | `v4` | `v6` | node24 (since v5) | Latest v6.0.2. v5 also uses node24; skipping to v6 for latest. |
| `actions/setup-node` | `v4` | `v6` | node24 (since v5) | Latest v6.4.0. Also update `node-version: 20` → `node-version: 24`. |
| `pnpm/action-setup` | `v4` | `v6` | node24 (since v5) | Latest v6.0.8. v4.4.0+ also runs on node24 but v6 is current. |
| `cloudflare/wrangler-action` | `v3` | `v4` | node24 | Latest v4.0.0 (released 2026-05-12). Defaults to Wrangler v4. |

### 2. Update `.github/workflows/deploy-web.yml`

**Before:**
```yaml
- uses: actions/checkout@v4
- uses: pnpm/action-setup@v4
- uses: actions/setup-node@v4
  with:
    node-version: 20
    cache: pnpm
- ...
- uses: cloudflare/wrangler-action@v3
```

**After:**
```yaml
- uses: actions/checkout@v6
- uses: pnpm/action-setup@v6
- uses: actions/setup-node@v6
  with:
    node-version: 24
    cache: pnpm
- ...
- uses: cloudflare/wrangler-action@v4
```

### 3. Optionally update `.github/workflows/index-memes.yml`

That workflow only uses `actions/checkout@v4` among the affected actions. Bump it to `v6` for consistency:

```yaml
- uses: actions/checkout@v6
```

No other changes needed — this workflow does not use `setup-node`, `pnpm/action-setup`, or `wrangler-action`.

### 4. Verify no other workflows are affected

Check all `.github/workflows/*.yml` files for any other usage of these four actions at deprecated versions.

## Risks & Mitigations

| Risk | Mitigation |
|------|------------|
| `cloudflare/wrangler-action@v4` defaults to Wrangler v4 instead of v3, which may change deployment behavior | The workflow does not pin a `wranglerVersion`. Wrangler v4 should be backward-compatible for `wrangler deploy`. If issues arise, pin `wranglerVersion: "3"` explicitly. |
| `actions/checkout@v6` changes credential persistence behavior (separate temp file instead of `.git/config`) | Only relevant for Docker container actions. Our workflow runs directly on the runner — no impact. |
| `actions/setup-node@v6` with `node-version: 24` may break if the project code relies on Node.js 20 APIs | The project already targets Node 20+ per `package.json` engines and `AGENTS.md`. Node 24 is backward-compatible for the subset of APIs used here. |
| Runner version requirement: v2.327.1+ needed for node24 actions | GitHub-hosted `ubuntu-latest` runners already meet this requirement. |

## Validation

1. **Dry-run the workflow change:** Submit a PR with the version bumps and verify the `deploy-web` workflow runs without Node.js 20 deprecation warnings.
2. **Verify deployment succeeds:** Confirm the workflow completes successfully and the web app is deployed to Cloudflare Workers.
3. **Check for other workflows:** Run a grep for `@v4` (for the four action names) across `.github/workflows/` to catch any other files needing updates.
4. **Check `index-memes.yml`:** Ensure it still runs successfully after the `checkout@v6` bump.

## Affected Files

- `.github/workflows/deploy-web.yml` — Update four action versions + node-version
- `.github/workflows/index-memes.yml` — Update `actions/checkout` from v4 to v6 (optional consistency fix)

## Definition of Done

- [ ] `actions/checkout` updated from `v4` to `v6` in `deploy-web.yml`
- [ ] `pnpm/action-setup` updated from `v4` to `v6` in `deploy-web.yml`
- [ ] `actions/setup-node` updated from `v4` to `v6` with `node-version: 24` in `deploy-web.yml`
- [ ] `cloudflare/wrangler-action` updated from `v3` to `v4` in `deploy-web.yml`
- [ ] (Optional) `actions/checkout` updated from `v4` to `v6` in `index-memes.yml`
- [ ] No Node.js 20 deprecation warnings in workflow run
- [ ] Web app deployment completes successfully
