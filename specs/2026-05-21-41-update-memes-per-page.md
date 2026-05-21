# Spec: Update Memes Per Page to 100

**Issue:** [#41 — UPDATE memes per page](https://github.com/alphabet90/openmeme/issues/41)
**Date:** 2026-05-21
**Status:** Proposed

---

## Problem

The memes feed currently displays 24 items per page on the web client (`PAGE_SIZE = 24` in all listing pages) while the Java API defaults to 20 items per page. This forces users to navigate through many pages to browse the full meme catalog, creating unnecessary friction. The product team wants to raise the per-page limit to 100 to improve browsing efficiency.

---

## Goals

- Display 100 memes per page on all meme listing surfaces (feed, category pages, search results).
- Ensure pagination controls remain correct (total page count recalculates automatically from the new page size).
- Keep the layout responsive on mobile and desktop.
- Update tests to reflect the new default so the test suite stays green.

---

## Out of Scope

- Infinite scroll or virtual list — pagination controls are retained as-is.
- Changes to widget/preview areas that intentionally show small counts (`getTopMemes(5)`, `getPopularMemes(12)`, `getRecentMemes(12)`).
- Increasing `maximum` beyond 100 in the OpenAPI spec (it is already capped at 100, matching the new target).

---

## Current State

| Location | File | Current Value |
|---|---|---|
| Web — popular feed | `apps/web/app/[locale]/memes/populares/page.tsx:23` | `PAGE_SIZE = 24` |
| Web — category page | `apps/web/app/[locale]/categorias/[slug]/page.tsx:24` | `PAGE_SIZE = 24` |
| Web — search page | `apps/web/app/[locale]/buscar/page.tsx:18` | `PAGE_SIZE = 24` |
| API — delegate (listMemes) | `apps/api/src/main/java/com/memes/api/controller/MemesApiDelegateImpl.java:46` | `orElse(20)` |
| API — delegate (listMemesByCategory) | `apps/api/src/main/java/com/memes/api/controller/MemesApiDelegateImpl.java:58` | `orElse(20)` |
| API — delegate (searchMemes) | `apps/api/src/main/java/com/memes/api/controller/MemesApiDelegateImpl.java:81` | `orElse(20)` |
| API — delegate (listCategories) | `apps/api/src/main/java/com/memes/api/controller/MemesApiDelegateImpl.java:35` | `orElse(20)` |
| OpenAPI spec — all endpoints | `apps/api/src/main/resources/openapi.yaml` (lines 63, 99, 150, 243) | `default: 20` |
| TypeScript API client | `apps/web/lib/data/memes.ts:201` | `limit ?? 20` |

---

## Proposed Changes

### 1. Web — `PAGE_SIZE` constants (3 files)

Change `PAGE_SIZE` from `24` to `100` in each meme listing page:

- `apps/web/app/[locale]/memes/populares/page.tsx`
- `apps/web/app/[locale]/categorias/[slug]/page.tsx`
- `apps/web/app/[locale]/buscar/page.tsx`

These constants are passed directly as the `limit` argument when calling the API client, so updating them is sufficient for the frontend change.

### 2. API — Default fallback in `MemesApiDelegateImpl`

Update the four `Optional.ofNullable(limit).orElse(20)` calls to `orElse(100)` so that callers that omit the `limit` query parameter also receive 100 results:

- `listCategories()` — line 35
- `listMemes()` — line 46
- `listMemesByCategory()` — line 58
- `searchMemes()` — line 81

### 3. OpenAPI spec — `default` value

Update `default: 20` → `default: 100` for the `limit` parameter on all four endpoints in `apps/api/src/main/resources/openapi.yaml`. The existing `maximum: 100` already accommodates the new default; no schema boundary changes are needed.

### 4. TypeScript API client — fallback default

Update the fallback in `apps/web/lib/data/memes.ts:201` from `limit ?? 20` to `limit ?? 100` to keep the client-side default consistent.

### 5. Tests — update assertions

Several tests hard-code the 20-item default. Update them to 100:

- `apps/api/src/test/java/com/memes/api/controller/MemesControllerTest.java` — assertions that set or expect `limit = 20`
- `apps/api/src/test/java/com/memes/api/service/MemeServiceTest.java:65` — `memeRepository.search("afip", "en", 20, 0)` mock expectation

---

## Risks & Mitigations

| Risk | Likelihood | Mitigation |
|---|---|---|
| Higher payload size slows page load | Medium | 100 meme cards with lazy-loaded images is well within acceptable range for a masonry grid; monitor via Web Vitals after deploy |
| Redis cache entries grow larger | Low | Cache TTLs are short (300–3600 s); existing `-v2` cache keys will naturally refresh with new page size |
| Masonry grid layout breaks on mobile with 100 items | Low | `MasonryGrid` and `MemeCard` are CSS-driven; existing responsive breakpoints handle variable item counts |
| API max exceeded if default > maximum | None | OpenAPI `maximum: 100` already matches the target default; no boundary violation |

---

## Validation

### Automated
- `cd apps/api && mvn test` — all `MemesControllerTest`, `MemeServiceTest`, and `MemeRepositoryTest` suites pass with updated assertions.
- `cd apps/web && pnpm lint` — no ESLint errors introduced.

### Manual QA Checklist
- [ ] Navigate to the memes feed — 100 memes appear on first load.
- [ ] Click "Next" page — next batch of 100 memes loads, no duplicates.
- [ ] Navigate to any category page — 100 memes per page.
- [ ] Perform a search — results paginate at 100 per page.
- [ ] Verify total page count is recalculated correctly (e.g., 350 memes → 4 pages).
- [ ] Verify layout on a narrow viewport (mobile, ~375 px width) — no overflow or layout breakage.
- [ ] Verify layout on a wide viewport (desktop, ≥1280 px).

---

## Implementation Order

1. Update `PAGE_SIZE` in the three web pages (no test changes needed).
2. Update `openapi.yaml` defaults.
3. Update `MemesApiDelegateImpl` fallbacks.
4. Update `apps/web/lib/api.ts` fallback.
5. Update test assertions.
6. Run `mvn test` and `pnpm lint` to confirm green.
