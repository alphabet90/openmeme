# Spec: Create Missing Listing Pages /memes/nuevos, /memes/top, /memes/clasicos, /memes/aleatorio

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
| New | `/memes/nuevos` | `sort=created_at` (most recent) |
| Classic | `/memes/clasicos` | `sort=score` with a `created_at` ceiling filter (older than N days, e.g. 90 days) — replace when a dedicated backend endpoint is available |
| Random | `/memes/aleatorio` | Listing page that fetches a page of memes with `sort=score` and shuffles client-side; pagination is disabled because random ordering is inherently non-deterministic across pages |

> **Naming rationale**: All listing routes under `/memes/` use Spanish slugs to match the existing `/memes/populares` convention and the Spanish nav/footer labels (`nuevos`, `clasicos`, `aleatorio`). `top` remains English because it is already the established label in both the UI and the `SortKey` type.

Each page replicates the layout, metadata generation, pagination, sidebar, breadcrumbs, and JSON-LD structure from `/memes/populares`. If duplication becomes excessive, extract a shared `MemeListingPage` shell.

### 2. Reference Update Sweep

Update all hard-coded links in:

- `components/nav/Nav.tsx`
- `components/Footer.tsx`
- `components/hero/Hero.tsx` (convert inert `role="tab"` buttons into links to the new listing pages)
- `app/[locale]/page.tsx` (verify the "more" link still works)
- Any other components or helpers referencing `/top`, `/nuevos`, `/clasicos`, `/aleatorio`, or `/categorias/random`

### 3. Legacy Route Redirects

Add `next.config.ts` `redirects` for:

- `/top` → `/memes/top`
- `/nuevos` → `/memes/nuevos`
- `/clasicos` → `/memes/clasicos`
- `/aleatorio` → `/memes/aleatorio`

This is declarative, testable, and preserves external link equity.

### 4. i18n Keys

Add a new namespace for each page (`top`, `nuevos`, `clasicos`, `aleatorio`) in `messages/*.json`, following the `populares` pattern. English base strings are sufficient for V1; missing translations fall back to English.

## Risks & Mitigations

| Risk | Impact | Mitigation |
|------|--------|------------|
| `/memes/clasicos` and `/memes/aleatorio` may require backend endpoints that do not yet exist | Medium | Ship with frontend fallbacks that use the existing `SortKey` type (`"score" \| "created_at" \| "title"`) in `apps/web/lib/api.ts:121`. For `/memes/clasicos`, fetch with `sort=score` and apply a `created_at` ceiling filter in the page component. For `/memes/aleatorio`, fetch with `sort=score` and shuffle the results client-side. Document that `SortKey` does not need to be extended for V1. |
| Extracting a shared `MemeListingPage` shell could introduce regressions in `/memes/populares` | Low | Keep the extraction minimal; do not redesign `populares`. Test `/memes/populares` manually after extraction. |
| Redirects in `next.config.ts` may conflict with existing dynamic routes | Low | Place redirect entries before catch-all/dynamic routes; test with `next build`. |
| i18n namespaces increase bundle size slightly | Low | Only add English base strings for V1; lazy-load page namespaces if needed. |

## Validation

- **Link integrity test**: Assert that `Nav.tsx`, `Footer.tsx`, and `Hero.tsx` only emit hrefs resolving to existing App Router pages (snapshot or route-manifest test).
- **Smoke test**: Visit `/memes/top`, `/memes/nuevos`, `/memes/clasicos`, `/memes/aleatorio` and assert 200 status, presence of `MemeListingGrid`, and correct `<h1>` / metadata.
- **Classic date ceiling test**: Assert that `/memes/clasicos` only renders memes with `created_at` older than the configured threshold (e.g. 90 days).
- **Redirect test**: Assert that `/top`, `/nuevos`, `/clasicos`, `/aleatorio` return 308/307 to their `/memes/*` counterparts.
- **Regression test**: Verify `/memes/populares`, `/memes/[category]/[slug]`, and `/buscar` remain untouched and functional.

## Out of Scope

- Redesign of `/memes/populares` or changes to its URL.
- Adding sort dropdowns / filters to the listing UI.
- Back-filling a "classic" algorithm or "random" backend endpoint if they do not exist yet.
- Creating actual `memes/classic` or `memes/random` filesystem categories (these are virtual browsing modes, not taxonomy folders).
- Translation of the new pages into all 7 locales (English base strings are sufficient for V1).
- Hero tab interactivity beyond basic navigation links (e.g. active-state synchronization, keyboard roving tabindex) is deferred to a future UX polish PR.

## Implementation Order

1. Add new App Router pages (`app/[locale]/memes/nuevos/page.tsx`, `.../top/...`, `.../clasicos/...`, `.../aleatorio/...`).
2. Update `Nav.tsx`, `Footer.tsx`, `Hero.tsx`, and any other link sources.
3. Add `next.config.ts` redirects for legacy routes.
4. Add base English i18n strings.
5. Include smoke tests and link-integrity verification.
