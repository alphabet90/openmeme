---
name: meme-classifier
description: Standardize meme categorization for OpenMeme by preventing duplicate or misspelled category directories. Decides whether a meme belongs to an existing category or deserves a new one. Detects semantically identical categories for merging, enforces a minimum existence threshold, and handles cross-categorization. Use when the user asks to categorize memes, standardize categories, merge duplicate categories, clean up category folders, review category structure, classify new memes, fix category organization, or mentions category duplicates. Triggered by phrases like "clasifica memes", "estandariza categorías", "merge duplicate categories", "clean up categories", "organiza las carpetas de memes", "category audit", or "rebalance categories". Operates in three modes: Sugerencia (suggests changes for approval), Autónomo (applies changes and commits), and Interactivo (reviews category by category together).
---

# Meme Classifier

Category standardization engine for OpenMeme. Prevents database pollution from duplicate or malformed category directories by enforcing taxonomy rules, semantic merge detection, existence thresholds, and cross-categorization logic.

## Core Rules

### 1. Categoría Primaria Única
Cada meme vive en **exactamente una** carpeta de categoría. La categoría primaria se elige por dominio principal del contenido. Los elementos secundarios van en tags, nunca en una segunda categoría.

### 2. Nomenclatura Kebab-Case
Todas las categorías usan kebab-case minúsculas: `simpsons-argentina`, `dragon-ball-reaction`. Sin camelCase, sin snake_case, sin espacios.

### 3. Sin Categorías Vacías
Una categoría con 0 memes se elimina inmediatamente. Una categoría con 1 meme se evalúa para migración.

## Workflow

Category management involves these steps:

1. **Analyze current state** — inventory all categories with meme counts
2. **Detect merge candidates** — find semantically identical or similar categories
3. **Apply existence threshold** — flag categories below minimum meme count
4. **Classify new memes** — route incoming memes to correct category
5. **Execute changes** — apply moves, rewrites, and cleanup in the chosen mode
6. **Git workflow** — create branch, commit, and push

## Step 1: Analyze Current State

Run `python scripts/analyze_categories.py --repo <path>` to generate a full inventory:

```
Category Inventory: 388 total
  Top: simpsons (179), reaction (126), argentina-politics (40)
  Below threshold (< 2): 304 categories
  Empty: 1 (ratatouille)
  Merge candidates: 47 pairs detected
```

The analysis script outputs:
- **category-report.json**: Full inventory with counts, paths, and merge scores
- **merge-candidates.json**: Ranked list of category pairs to merge
- **threshold-violators.json**: Categories below the existence threshold

Read `references/taxonomy.md` for the canonical category hierarchy and naming conventions.

## Step 2: Detect Merge Candidates

The analyzer detects four types of merge candidates. Review each and decide action:

### Type A: Exact Semantic Match
Same franchise/concept with different naming. **Always merge.**

| From | To | Rationale |
|------|-----|-----------|
| `courage-cowardly-dog` | `courage-the-cowardly-dog` | Same show, incomplete name |
| `fairly-odd-parents` | `fairly-oddparents` | Spelling variant |
| `southpark` | `south-park` | Missing hyphen |
| `south-park` | `south-park` | Canonical form |
| `simpsons-argentina` | `simpsons` | Franchise + region hybrid; use `simpsons` + tag `argentina` |
| `reaccion-espanol` | `reaccion-espanol` | Normalize charset (already fixed) |

### Type B: Franchise + Suffix Subcategory
A franchise category split into overly specific subcategories. **Merge into parent** unless the subcategory has 10+ memes and distinct identity.

| From | To | Condition |
|------|-----|-----------|
| `simpsons-argentina-politics` | `simpsons` | Always merge, use tags |
| `simpsons-comparison` | `simpsons` | Always merge |
| `simpsons-courtroom` | `simpsons` | Always merge |
| `simpsons-krusty` | `simpsons` | Always merge |
| `simpsons-milei` | `simpsons` | Always merge |
| `simpsons-political-satire` | `simpsons` | Always merge |
| `star-wars-dark-humor` | `star-wars` | Always merge |
| `star-wars-force-ghost` | `star-wars` | Always merge |
| `star-wars-prequel` | `star-wars` | Always merge |
| `star-wars-reaction` | `star-wars` | Always merge |
| `star-wars-spanish-pun` | `star-wars` | Always merge |
| `dragon-ball-argentina-politics` | `dragon-ball` | Always merge |
| `dragon-ball-reaction` | `dragon-ball` | Always merge |
| `jojo-approaching` | `jojo-bizarre-adventure` | Always merge |
| `jojo-reference-parody` | `jojo-bizarre-adventure` | Always merge |
| `jojo-reaction` | `jojo-bizarre-adventure` | Always merge |

### Type C: Granular → General
Region-specific or overly narrow categories with few memes. **Merge into general equivalent** if < 5 memes.

| From | To | Threshold |
|------|-----|-----------|
| `argentina-football-fandom` | `argentina-football` | < 5 memes |
| `argentina-football-parody` | `argentina-football` | < 5 memes |
| `argentina-football-shitpost` | `futbol-shitpost` | < 5 memes |
| `anime-argentina` | `anime` | < 5 memes |
| `anime-reaction` | `anime` | < 5 memes |

### Type D: Conceptual Overlap
Different names for the same concept. **Merge** after semantic review.

| From | To | Rationale |
|------|-----|-----------|
| `absurd` | `absurdist-humor` | Same concept |
| `absurd-humor` | `absurdist-humor` | Same concept, pick canonical |
| `futbol-reaction` | `football-reaction` | Spanish/English synonym |
| `argentina-futbol` | `argentina-football` | Spanish/English synonym |
| `reaccion-espa-ol` | `reaccion-espanol` | Charset normalization |
| `harry-potter-crossover` | `harry-potter` | Use tags for crossover |
| `cursed-car` | `cursed` | Too specific, use tags |

### Merge Decision Matrix

```
if count < 2:
    → FORCE merge (no exceptions)
elif count < 5 and is_suffix_subcategory:
    → RECOMMEND merge
elif semantic_similarity > 0.85:
    → RECOMMEND merge
elif count >= 10 and distinct_identity:
    → KEEP (e.g., argentina-politics with 40 memes)
else:
    → REVIEW case by case
```

## Step 3: Apply Existence Threshold

Categories below the threshold are flagged for action:

| Count | Action | Destination |
|-------|--------|-------------|
| 0 | **Delete** folder | N/A |
| 1 | **Migrate** to parent/related category | Best match via tag/content analysis |
| 2-4 | **Review** for merge | Suggest target in report |

**Migration priority** for single-meme categories:
1. Match against known franchise parent (e.g., `a-bugs-life-reaction` → `reaction`)
2. Match against content tags in the meme's MDX
3. Fall back to `others` if no match found

Run `python scripts/analyze_categories.py --threshold 2 --repo <path>` to generate the threshold violation report.

## Step 4: Classify New Memes

When a new meme arrives, route it through the classification decision tree:

```
1. Extract content signals:
   - Franchise mentioned in title/description?
   - Character names detected?
   - Language/region detected?
   - Format type identified (reaction, template, shitpost)?

2. Check franchise match:
   IF franchise_exists(category):
     → Route to franchise category
     IF region_specific:
       → Use franchise category + region tag
     IF political:
       → Use franchise category + political tag

3. Check format match:
   IF matches format_category (reaction, shitpost, template):
     → Route to format category + franchise tag

4. Check region match:
   IF argentine_content AND no franchise:
     → Route to argentina-humor or argentina-reaction

5. Default:
   → Create new category ONLY if:
     a. No existing category matches semantically
     b. Expected volume justifies it (5+ memes projected)
     c. Name follows kebab-case convention
```

Run `python scripts/classify_meme.py --source <mdx-path> --repo <path>` to get a classification recommendation with confidence score.

## Cross-Categorization Rule

When a meme belongs to two worlds (e.g., Simpsons + Argentina):

1. **Primary category**: The franchise/source material always wins — `simpsons`
2. **Secondary via tags**: Add `argentina`, `political`, `satire` as tags
3. **Never create**: `simpsons-argentina-politics` — this is the anti-pattern

Exception: When a cross-category has **10+ memes** with a **distinct, self-sustaining identity** (e.g., `dragon-ball-argentina-politics` with 20+ memes), it may remain as a dedicated category. Review case by case.

## Three Operating Modes

The skill adapts to user preference via explicit mode selection or inferred from phrasing.

### Modo Sugerencia (Default)

**Trigger phrases**: "sugiéreme cambios", "qué moverías", "muéstrame un plan", "modo sugerencia"

Actions:
1. Run full analysis
2. Generate structured proposal with:
   - `mv` commands for each category merge
   - Frontmatter changes for each affected meme
   - Before/after category map
3. Present for user approval
4. Do NOT modify any files

Output format:
```
PROPOSED CHANGES (Modo Sugerencia)
====================================
Merges (47):
  mv memes/courage-cowardly-dog/* memes/courage-the-cowardly-dog/
  rmdir memes/courage-cowardly-dog
  [update category field in affected MDX files]
  ...

Threshold migrations (304):
  mv memes/ratatouille/* → DELETE (empty)
  mv memes/a-bugs-life-reaction/...mdx memes/reaction/
  [update category: a-bugs-life-reaction → reaction]
  ...

New canonical structure:
  47 categories merged into 23
  304 sub-threshold categories resolved
  Final count: ~120 categories
```

### Modo Autónomo

**Trigger phrases**: "hacelo", "modo autónomo", "aplica los cambios", "ejecuta", "autonomous mode"

Actions:
1. Run full analysis
2. Apply ALL changes automatically:
   - Execute `mv` for merges
   - Delete empty categories
   - Migrate threshold violators
   - Update `category` field in all affected MDX frontmatters
3. Create git branch, commit, push
4. Generate comprehensive report explaining reasoning

Safety rules in autonomous mode:
- Never merge categories with 10+ memes without explicit review
- Always create a report before committing
- If > 50 memes affected, pause and ask for confirmation
- Backup affected MDX files to `.backup/` before modifying

### Modo Interactivo

**Trigger phrases**: "modo interactivo", "revisemos uno por uno", "vamos categoría por categoría", "interactive mode"

Actions:
1. Run analysis and generate ranked list
2. Present ONE merge candidate at a time:
   - Show both categories with meme counts
   - Show sample memes from each
   - Ask: "Merge `[from]` → `[to]`? (y/n/skip)"
3. Accumulate approved changes
4. After all reviewed, ask to apply batch
5. Apply approved changes, git workflow

## Step 5: Execute Changes

### File Operations

For each approved merge:
```bash
# 1. Move meme files
mv memes/<from-category>/<slug>.mdx memes/<to-category>/<slug>.mdx
mv memes/<from-category>/<slug>.png memes/<to-category>/<slug>.png

# 2. Update frontmatter category field
# (done via script to ensure consistency)

# 3. Remove empty source directory
rmdir memes/<from-category>
```

For each threshold migration:
```bash
# Move to best-match target and update frontmatter
mv memes/<small-cat>/<slug>.* memes/<target-cat>/
# Update category field in MDX
```

### Frontmatter Updates

When a meme moves categories, update its MDX:
```yaml
# Before
category: "simpsons-argentina-politics"
tags: ["argentina", "simpsons"]

# After
category: "simpsons"
tags: ["argentina", "simpsons", "political", "satire"]
```

Use `python scripts/classify_meme.py --apply` to batch-update frontmatter.

## Step 6: Git Workflow

```bash
# Branch naming
git checkout -b classify/category-cleanup-$(date +%Y%m%d)

# Commit message
git commit -m "classify: standardize categories — merge duplicates, apply threshold

- Merge 47 duplicate/similar categories into 23 canonical ones
- Migrate 304 sub-threshold categories (< 2 memes)
- Delete 1 empty category
- Update category frontmatter in affected memes
- Final category count: ~120 (from 388)

Merged:
  - courage-cowardly-dog → courage-the-cowardly-dog
  - fairly-odd-parents → fairly-oddparents
  - southpark → south-park
  - [full list in report]"
```

## Classification Confidence Score

When classifying a meme, report confidence:

| Score | Meaning | Action |
|-------|---------|--------|
| 95-100 | Exact franchise match | Route immediately |
| 80-94 | Strong semantic match | Route with note |
| 60-79 | Ambiguous, multiple candidates | Present options to user |
| < 60 | No clear match | Suggest new category or `others` |

## Troubleshooting

**Error: Category has memes but analysis reports 0**
Cause: Only `.jpg`/`.png` files, no `.mdx` files in directory.
Solution: Report as "image-only category — needs MDX creation or migration."

**Error: Merge would create slug collision**
Cause: Same slug exists in both source and target categories.
Solution: Append `-2` to the moved file's slug, update frontmatter.

**Error: Circular merge detected (A→B→A)**
Cause: Merge chain creates loop.
Solution: Resolve to the category with highest meme count as canonical.

**Error: User requests interactive but > 100 merges**
Cause: Scope too large for interactive review.
Solution: Suggest Sugerencia or Autónomo mode instead. Offer top 20 by impact.
