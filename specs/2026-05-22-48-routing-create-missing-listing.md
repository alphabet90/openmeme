# Spec: Create Missing Listing Pages /memes/new, /memes/top, /memes/classic, /memes/random

**Issue:** [alphabet90/openmeme#48](https://github.com/alphabet90/openmeme/issues/48)  
**Branch:** `looper/planner/48-routing-create-missing-listing`  
**Base:** `main`  
**Date:** 2026-05-22

---

## Problem

The navigation (`Nav.tsx`) and footer (`Footer.tsx`) currently link to top-level routes that **do not exist as Next.js pages**, causing 404 errors for users:

- `/top` (linked from Nav & Footer)
- `/nuevos` (linked from Nav & Footer) → corresponds to "new"
- `/clasicos` (linked from Footer) → corresponds to "classic"
- `/aleatorio` (linked from Nav & Footer) → corresponds to "random"

Additionally, the footer contains a broken link to `/categorias/random`, which is not a real category folder in `memes/`.

The only functional listing page under `/memes/` today is `/memes/populares` (sort by score). There is no consistent URL scheme for the other browsing modes. This creates:

- **Broken UX**: users clicking "Top", "New", "Classic", or "Random" hit a 404.
- **URL inconsistency**: `/memes/populares` lives under `/memes/` while the rest are (broken) top-level routes.
- **SEO / permalink fragility**: scattered routing makes it impossible to guarantee stable canonical URLs for each browsing mode.

## Goals

1. Create four new App Router pages under `/memes/` for the missing browsing modes.
2. Update every internal reference (Nav, Footer, home page, etc.) to point to the new routes.
3. Redirect legacy top-level routes to the new `/memes/*` routes so existing external links do not 404.
4. Remove the broken `/categorias/random` link from the Footer.
5. Leave `/memes/populares`, category detail routes, and the search page untouched.

## Approach

### 1. New App Router Pages

Create the following pages under `apps/web/app/[locale]/memes/`:

| Mode | New Route | Sort / Behavior |
|------|-----------|-----------------|
| Top | `/memes/top` | `sort=score` (highest score) |
| New | `/memes/new` | `sort=created_at` (most recent) |
| Classic | `/memes/classic` | Placeholder sort or defined backend contract (see Risks) |
| Random | `/memes/random` | Random shuffle or redirect to random meme (see Risks) |

Each page replicates the layout, metadata generation, pagination, sidebar, breadcrumbs, and JSON-LD structure from `/memes/populares`. If duplication becomes excessive, extract a shared `MemeListingPage` shell.

### 2. Reference Update Sweep

Update all hard-coded links in:

- `components/nav/Nav.tsx`
- `components/Footer.tsx`
- `app/[locale]/page.tsx` (verify the "more" link still works)
- Any other components or helpers referencing `/top`, `/nuevos`, `/clasicos`, `/aleatorio`, or `/categorias/random`

### 3. Legacy Route Redirects

Add `next.config.js` `redirects` for:

- `/top` → `/memes/top`
- `/nuevos` → `/memes/new`
- `/clasicos` → `/memes/classic`
- `/aleatorio` → `/memes/random`

This is declarative, testable, and preserves external link equity.

### 4. i18n Keys

Add a new namespace for each page (`top`, `new`, `classic`, `random`) in `messages/*.json`, following the `populares` pattern. English base strings are sufficient for V1; missing translations fall back to English.

## Risks & Mitigations

| Risk | Impact | Mitigation |
|------|--------|------------|
| `/memes/classic` and `/memes/random` may require backend endpoints that do not yet exist | Medium | Ship with sensible frontend fallbacks (e.g., classic = score + old date filter, random = client-side shuffle of a small fetched set) and coordinate a backend PR later. |
| Extracting a shared `MemeListingPage` shell could introduce regressions in `/memes/populares` | Low | Keep the extraction minimal; do not redesign `populares`. Test `/memes/populares` manually after extraction. |
| Redirects in `next.config.js` may conflict with existing dynamic routes | Low | Place redirect entries before catch-all/dynamic routes; test with `next build`. |
| i18n namespaces increase bundle size slightly | Low | Only add English base strings for V1; lazy-load page namespaces if needed. |

## Validation

- **Link integrity test**: Assert that `Nav.tsx` and `Footer.tsx` only emit hrefs resolving to existing App Router pages (snapshot or route-manifest test).
- **Smoke test**: Visit `/memes/top`, `/memes/new`, `/memes/classic`, `/memes/random` and assert 200 status, presence of `MemeListingGrid`, and correct `<h1>` / metadata.
- **Redirect test**: Assert that `/top`, `/nuevos`, `/clasicos`, `/aleatorio` return 308/307 to their `/memes/*` counterparts.
- **Regression test**: Verify `/memes/populares`, `/memes/[category]/[slug]`, and `/buscar` remain untouched and functional.

## Out of Scope

- Redesign of `/memes/populares` or changes to its URL.
- Adding sort dropdowns / filters to the listing UI.
- Back-filling a "classic" algorithm or "random" backend endpoint if they do not exist yet.
- Creating actual `memes/classic` or `memes/random` filesystem categories (these are virtual browsing modes, not taxonomy folders).
- Translation of the new pages into all 7 locales (English base strings are sufficient for V1).

## Implementation Order

1. Add new App Router pages (`app/[locale]/memes/new/page.tsx`, `.../top/...`, `.../classic/...`, `.../random/...`).
2. Update `Nav.tsx`, `Footer.tsx`, and any other link sources.
3. Add `next.config.js` redirects for legacy routes.
4. Add base English i18n strings.
5. Include smoke tests and link-integrity verification.
