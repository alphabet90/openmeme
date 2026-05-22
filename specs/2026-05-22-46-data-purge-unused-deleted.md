# Spec: Purge Unused/Deleted Categories During Indexing Job

**Issue:** [alphabet90/openmeme#46](https://github.com/alphabet90/openmeme/issues/46)  
**Date:** 2026-05-22  
**Status:** Planning  
**Branch:** `looper/planner/46-data-purge-unused-deleted`  
**Base Branch:** `main`

---

## 1. Problem

When category directories are renamed or deleted in the `memes/` folder (e.g., `argentina-politica` → `argentina-politics`), the indexing job creates the new category and inserts memes under it, but **never removes the old category or its associated memes from the database**. This produces:

- **Orphan categories**: rows in `categories` that no longer map to any existing folder on disk.
- **Meme duplication**: the same slug exists under the old orphan category and the new live category, because the indexer treats `(category_id, slug)` as the unique boundary but the old category is never cleaned up.
- **Stale file references**: `meme_images` still point to paths under deleted folders even though the folder no longer exists in the repo.
- **Data drift**: over time the database becomes an inconsistent superset of the filesystem, forcing manual SQL fixes and breaking user expectations when searching or browsing by category.

Concrete example from production:

- Category `argentina-politica` (`id = 613`) was deleted from `memes/` after being renamed to `argentina-politics` (`id = 71`).
- Meme `mayra-mendoza-supervisa-limpieza-sumideros` now exists twice:
  - `memes.id = 630` → `category_id = 613` (orphan)
  - `memes.id = 1219` → `category_id = 71` (live)
- The orphan copy has `score = 2403` while the live copy has `score = 0`, so search/ranking may surface the stale, un-maintained version.

---

## 2. Goals

1. Automatically detect and remove orphan categories and their memes during the indexing job.
2. Ensure the database remains consistent with the `memes/` filesystem after every run.
3. Prevent meme duplication caused by category renames or deletions.
4. Provide observability (logging/summary) for every purge operation.
5. Do not delete categories or memes that still exist on disk.

---

## 3. Non-Goals

- Automatic renaming / migration of memes from old category to new category (the indexer already handles recreation under the new folder).
- Detecting duplicate slugs within the same category (this is a separate data-quality issue).
- Purging orphan authors, subreddits, or tags that are no longer referenced (can be addressed in a future cleanup pass).
- A dry-run CLI flag or interactive confirmation prompt (acceptable as fast-follow but not required for V1).
- Back-filling historical data or retroactive cleanup scripts for existing orphan rows (the fix should prevent new orphans; a one-off manual cleanup may still be needed for existing drift).

---

## 4. Approach

### 4.1 Authority Model

The filesystem (`memes/`) is the single source of truth. Any category in the DB whose directory no longer exists on disk is considered orphan and must be purged.

### 4.2 Purge Pass

Introduce a **category purge pass** that runs at the end of every indexing job (both full and incremental reindex workflows):

1. **Timing**: Run after all MDX files are parsed and upserted so that live categories are never accidentally purged due to race conditions or transient filesystem errors.
2. **Detection**: Enumerate every `categories.name` in the DB. If `memes/{name}/` does not exist, mark the category for deletion.
3. **Cascade deletion**: Deleting a category must cascade to:
   - All `memes` rows referencing that `category_id`
   - All dependent rows (`meme_images`, `meme_tags`, translations, etc.) via existing FK cascades or explicit cleanup.
4. **Safety boundary**: The purge is **opt-out** (runs by default) but guarded by a dry-run flag or confirmation threshold for the first production run. A hard cap or explicit allow-list is not required for V1 because the authority model is unambiguous: if the folder is gone, the category is dead.
5. **Transaction safety**: Wrap the entire purge operation in a single DB transaction so that either all orphan categories are removed or none are, preventing partial cleanup states.

### 4.3 Implementation Decisions

- **Filesystem as authority**: The `memes/` directory structure is the canonical registry of valid categories. This is already the source of truth for the indexer, so extending it to cleanup is consistent and requires no new configuration.
- **Purge runs after upserts**: This prevents a transient missing folder (e.g., due to a partially checked-out branch) from wiping legitimate data. The indexer must finish parsing all live files first, then evaluate deletions.
- **Hard delete, not soft delete**: Orphan categories and memes have no value once the folder is gone. Soft-deleting would require adding `deleted_at` filters to every query path (search, category listing, related memes, sitemap, etc.) and complicates the unique constraints. Since the folder is gone forever, a hard delete is the simpler and correct model.
- **Reuse existing cascade rules**: If the schema already has `ON DELETE CASCADE` on `meme_images` and `meme_tags`, rely on them. If not, perform explicit deletions in the correct order inside a transaction.
- **Logging/observability**: Emit structured logs (or return a summary payload) listing every deleted category and the count of memes removed. This makes the purge auditable.

### 4.4 Scope Boundary for V1

Only purge categories that no longer have a matching folder. Do not attempt to merge memes across renamed categories or resolve slug conflicts manually; the indexer will naturally recreate memes under the new category name on the next run.

---

## 5. Affected Components

| Component | Change |
|-----------|--------|
| `apps/api/src/main/java/.../IndexerService.java` | Add purge pass after upserts |
| `apps/api/src/main/java/.../MemeRepository.java` | Add method to list all category names and delete categories by ID |
| `apps/api/src/main/java/.../CategoryRepository.java` | Add method to fetch all categories for filesystem comparison |
| `apps/api/src/test/java/.../IndexerServiceTest.java` | Unit tests for purge logic |
| `apps/api/src/test/java/.../MemeRepositoryTest.java` | Integration tests for cascade deletion |
| `AGENTS.md` or `docs/` | Document cleanup behavior |

---

## 6. Risks & Mitigations

| Risk | Likelihood | Impact | Mitigation |
|------|------------|--------|------------|
| Accidental deletion of live categories due to filesystem race condition | Low | High | Run purge **after** all upserts; transient missing folders will be recreated during upsert phase. |
| Partial cleanup due to transaction failure | Low | Medium | Wrap entire purge in a single DB transaction; all-or-nothing semantics. |
| Operator anxiety about destructive phase | Medium | Low | Add `--dry-run` flag in fast-follow PR; V1 relies on unambiguous authority model. |
| Existing orphan data in production | High | Medium | V1 prevents new orphans; one-off manual cleanup may still be needed for historical drift. |

---

## 7. Validation

### 7.1 Unit Tests

- Given a mock DB state and a mock filesystem, assert that orphan categories are identified and deleted, while live categories are untouched.

### 7.2 Integration Tests (Testcontainers)

- Seed Postgres with a category that has a folder, a category that does not, and a duplicate slug across both.
- Run the indexer and assert that only the orphan category + its memes are removed, and the live meme remains.

### 7.3 Safety Invariant Tests

- Assert that the purge step never deletes a category whose folder still exists, even if the folder is empty (empty categories may still be valid).

### 7.4 What NOT to Test

- Do not test external filesystem edge cases (e.g., network mounts, permission errors) beyond standard Java/Node FS behavior; those are out of scope and should be handled by normal I/O error propagation.

---

## 8. Further Notes

- **Why not soft-delete?** Soft-deleting orphan categories would require adding `deleted_at` filters to every query path (search, category listing, related memes, sitemap, etc.) and complicates the unique constraints. Since the folder is gone forever, a hard delete is the simpler and correct model.
- **Why run after upserts?** Running the purge before upserts creates a window where the DB is missing categories that are about to be re-inserted, which is unnecessary churn. Running after ensures the DB converges cleanly to the filesystem state.
- **Failure mode prevented**: Without this purge, every category rename or cleanup creates permanent ghost data. Over months this accumulates into significant storage waste, broken search results, and inconsistent API responses.
- **New complexity introduced**: The indexer gains a destructive phase. This is mitigated by the unambiguous authority model (filesystem) and transaction wrapping. If operators are nervous, a `--dry-run` flag can be added quickly in a follow-up PR.
- **PR guidance**: The implementation PR should include:
  - The purge logic in the indexer service.
  - Updated unit/integration tests.
  - A note in `AGENTS.md` or `docs/` describing the cleanup behavior.

---

## 9. User Stories

1. As a **content maintainer**, I want renamed or deleted category folders to be automatically reflected in the database, so that I do not have to run manual SQL deletes to fix orphan data.
2. As a **user browsing the site**, I want to see only one instance of each meme under its correct category, so that I am not confused by duplicate entries or broken category pages.
3. As a **developer running the indexer**, I want the indexing job to leave the database consistent with the `memes/` filesystem after every run, so that I can trust the DB state without additional validation scripts.
4. As a **search engine consumer**, I want ranking signals (score, freshness) to be attached to the live meme record rather than an invisible orphan duplicate, so that search results are accurate.

---

*Generated-By: looper 0.0.0-dev (runner=planner, agent=opencode)*
