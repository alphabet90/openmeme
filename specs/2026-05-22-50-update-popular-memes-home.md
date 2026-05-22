# Spec: Update Popular Memes home section to show full first page in a 5-column grid

| Field | Value |
|-------|-------|
| **Issue** | [alphabet90/openmeme#50](https://github.com/alphabet90/openmeme/issues/50) |
| **Branch** | `looper/planner/50-update-popular-memes-home` |
| **Base** | `main` |
| **Date** | 2026-05-22 |
| **Estimate** | XS (1 dev, ~1/2 day) |

---

## Problem

The "Popular memes" section on the home page (`/`) currently behaves inconsistently with the dedicated popular memes page (`/memes/populares`):

- It fetches only **25 items** (`getPopularMemes(25, …)`) instead of the full first page, creating an artificial preview that hides trending content behind a "See more" click.
- It renders those items in a **4-column CSS masonry grid** (`MasonryGrid.module.css` uses `columns: 4`), while the full listing page uses a **5-column grid** (`MemeListingGrid.module.css` uses `columns: 5`). The visual density mismatch makes the home page feel lighter and less engaging.
- Users landing on the home page get an incomplete view of what is trending, reducing discovery and increasing friction.

## Goals

1. The home page "Popular memes" section must display the **complete first page** of popular results, with no client-side truncation or artificial limit.
2. The grid must use the **same 5-column layout** already established on the dedicated `/memes/populares` page, ensuring visual parity.
3. Existing responsive breakpoints must remain unchanged.
4. The existing "See more" link must continue to route users to `/memes/populares`.
5. No changes to API sorting, pagination logic, or the dedicated page itself.

## Approach

### 1. Unify grid layout token

Both the home section and the full listing page should derive their desktop column count from a single source of truth so they cannot drift again.

**Decision:** Extract a shared CSS custom property (or shared module class) in `@openmeme/ui` that defines the desktop column count. The simplest, least-intrusive change is to update `MasonryGrid.module.css` to match the 5-column breakpoint map already used by `MemeListingGrid.module.css`, because `MasonryGrid` is the component consumed by the home page.

| Breakpoint | `MemeListingGrid` (today) | `MasonryGrid` (today) | `MasonryGrid` (target) |
|------------|---------------------------|-----------------------|------------------------|
| > 1100 px  | 5 columns                 | 4 columns             | **5 columns**          |
| ≤ 1100 px  | 4 columns                 | 3 columns             | 4 columns              |
| ≤ 820 px   | 3 columns                 | 2 columns             | 3 columns              |
| ≤ 540 px   | 2 columns                 | 2 columns             | 2 columns              |

Update `packages/ui/src/components/MasonryGrid.module.css`:
- Change `columns: 4` → `columns: 5` in `.grid`.
- Change `columns: 3` → `columns: 4` in the `1100px` breakpoint.
- Change `columns: 2` → `columns: 3` in the `820px` breakpoint.
- Keep the `540px` breakpoint at `columns: 2`.
- Optionally align `column-gap` to `12px` to match `MemeListingGrid.module.css` (today it is `10px`).

### 2. Show the full first page

The home page currently calls `getPopularMemes(25, apiLocale)`. The dedicated page uses `PAGE_SIZE = 100` (`getMemeListing({ sort: "score", page: 0, limit: 100, … })`).

**Decision:** Change the home page to request the same first-page limit so the API is the single source of truth for what constitutes the first page.

In `apps/web/app/[locale]/page.tsx`:
- Replace `getPopularMemes(25, apiLocale)` with `getPopularMemes(100, apiLocale)`.
- Alternatively, and more explicitly, replace the call with `getMemeListing({ sort: "score", page: 0, limit: 100, locale: apiLocale })` and map `listing.data` directly. This avoids hard-coding a magic number in `getPopularMemes`.

**Preferred option:** Keep `getPopularMemes` but change its default/limit to `100`, or call `getPopularMemes(100, apiLocale)` from the home page. The function already wraps `fetchMemes({ limit, sort: "score", page: 1, locale })`; note that the API uses 0-based pages, so `page: 1` in `getPopularMemes` actually means the *second* page. Verify and fix if necessary:
- `getPopularMemes` currently passes `page: 1`. The dedicated page passes `page: 0` for the first page. This looks like a bug in `getPopularMemes`; it should pass `page: 0` (or omit `page`, because `fetchMemes` defaults it to `undefined` which the API treats as page 0).

**Correction:** Update `getPopularMemes` in `apps/web/lib/data/memes.ts` to pass `page: 0` (or omit the `page` parameter) so it truly returns the first page.

Then, in `apps/web/app/[locale]/page.tsx`, call `getPopularMemes(100, apiLocale)`.

### 3. Preserve "See more"

`MasonryGrid` already accepts an optional `moreHref` prop and renders a "Ver más" link. The home page passes `moreHref={localePath(locale, "/memes/populares")}`. No change is needed here except ensuring the link remains after layout updates.

## Risks & Mitigations

| Risk | Mitigation |
|------|------------|
| Home page payload grows from 25 to 100 memes | 100 memes is already the production page size on `/memes/populares`; the API and CDN handle it. Images are lazy-loaded by `MemeCard`. |
| Responsive breakpoints accidentally altered | Only change the numeric values inside the existing `@media` blocks; do not add or remove breakpoints. |
| `getPopularMemes` used elsewhere with a smaller limit | The function signature keeps an optional `limit` parameter; other callers can still pass a custom limit. |
| `page: 1` in `getPopularMemes` is intentional | Audit all usages. It is only called from the home page. Fixing it to `page: 0` aligns it with the concept of "first page". |

## Validation

1. **Visual regression:**
   - Open the home page at a desktop viewport (> 1100 px).
   - Confirm the "Popular memes" grid renders 5 columns.
   - Resize to 1100 px, 820 px, and 540 px; confirm columns degrade to 4, 3, and 2 respectively.
2. **Content completeness:**
   - Confirm the number of memes rendered in the home section equals the `limit` returned by the API (100, or whatever the API's first-page size is).
   - Confirm no client-side truncation is applied.
3. **Integration:**
   - Click "Ver más" and confirm it navigates to `/memes/populares`.
   - Confirm the dedicated `/memes/populares` page still paginates correctly and its grid is unchanged.
4. **Build checks:**
   - `pnpm lint` in `apps/web` passes.
   - `pnpm build` in `apps/web` succeeds.

## Affected Files

- `packages/ui/src/components/MasonryGrid.module.css`
- `apps/web/app/[locale]/page.tsx`
- `apps/web/lib/data/memes.ts`

## Definition of Done

- [ ] `MasonryGrid.module.css` uses a 5-column desktop grid with the same breakpoint map as `MemeListingGrid.module.css`.
- [ ] Home page fetches the full first page of popular memes (100 items, page 0) with no artificial limit.
- [ ] Existing responsive breakpoints are preserved.
- [ ] "See more" link remains and routes to `/memes/populares`.
- [ ] Dedicated `/memes/populares` page is unaffected.
- [ ] No new lint / TypeScript errors.
- [ ] Visual regression verified at desktop, tablet, and mobile widths.
