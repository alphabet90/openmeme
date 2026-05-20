# Spec: Add Hello World Comment to README

**Issue:** [#39 — Test: Add hello world comment to README](https://github.com/alphabet90/openmeme/issues/39)
**Date:** 2026-05-20
**Branch:** `looper/planner/39-test-add-hello-world`

---

## Problem

The root `README.md` does not contain a "hello world" comment at the top. This is a test issue to verify the end-to-end Looper planning and PR workflow.

---

## Goals

- Add a single Markdown comment (`<!-- Hello World -->`) at the very top of `README.md`.
- Confirm the Looper planner → implementer → PR pipeline works correctly for a trivial documentation change.

---

## Approach

1. Open `README.md` at the repo root.
2. Prepend the line `<!-- Hello World -->` followed by a blank line before the existing content.
3. Commit with message: `Add hello world comment to README`.
4. Push branch and open a PR targeting `main`.

No build steps, tests, or dependency changes are required — this is a documentation-only edit.

---

## Files Changed

| File | Change |
|------|--------|
| `README.md` | Prepend `<!-- Hello World -->` comment on line 1 |

---

## Risks

- **None significant.** A Markdown comment is invisible in rendered output and does not affect any build, test, or CI pipeline.
- The `index-memes.yml` CI workflow only triggers on changes to `memes/**`, so this change will not trigger reindexing.

---

## Validation

- [ ] `README.md` line 1 reads `<!-- Hello World -->`.
- [ ] Rendered GitHub README is visually unchanged (comment is hidden).
- [ ] No CI pipelines fail due to this change.
