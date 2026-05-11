---
name: meme-curator
description: Professional meme metadata curation and SEO optimization for OpenMeme. Reviews, improves, and rewrites .mdx metadata including titles, descriptions, tags, and slugs for searchability and discoverability. Use when the user asks to review meme metadata, improve descriptions for SEO, optimize meme titles, curate meme quality, fix tags, audit meme entries, rewrite meme descriptions, or polish meme metadata. Triggered by phrases like "revisa los metadatos", "mejora las descripciones para SEO", "optimize meme titles", "curate memes", "fix tags for", "audit meme descriptions", or "review meme quality". Operates on a specific meme, a category folder, or a filtered subset — never on the entire repository at once.
---

# Meme Curator

Professional metadata optimization engine for OpenMeme. Reviews individual memes or targeted subsets and produces SEO-optimized `.mdx` files with compelling titles, rich descriptions, and discoverable tags.

## Scope Rule

Operate on **specific targets only** — never on the full repository. Valid targets:

- **Single meme**: `memes/simpsons/homer-strangling-bart.mdx`
- **Category folder**: `memes/argentina-football/`
- **Filtered subset**: Memes matching a query (e.g., "all simpsons memes with score under 100")

Confirm the target with the user before making changes when the scope exceeds 20 memes.

## Workflow

Meme curation involves these steps:

1. **Identify targets** — locate the meme(s) to curate
2. **Audit metadata** — score current quality and identify issues
3. **Apply SEO improvements** — rewrite titles, descriptions, and tags
4. **Quality check** — verify output against standards
5. **Git workflow** — create branch, commit, and push

## Step 1: Identify Targets

Locate memes based on user input. Common patterns:

| User Request | Action |
|-------------|--------|
| "Revisa los nuevos memes de /argentina" | Search `memes/` for entries with `subreddit: argentina`, sort by `created_at` recent first |
| "Mejora los simpsons" | Target `memes/simpsons/*.mdx` excluding `.es-AR.` variants |
| "Optimiza este meme" | Read the specific `.mdx` file provided |
| "Audita los de score bajo" | Filter memes by `score` threshold |

Run `python scripts/audit_memes.py --query <query> --repo <path>` to get a quality audit report. Use this to prioritize which memes need the most work.

## Step 2: Audit Metadata

For each target meme, score the current metadata across these dimensions:

### Scoring Rubric (0-100)

| Dimension | Weight | Criteria |
|-----------|--------|----------|
| **Title** | 25% | 30-70 chars, compelling, includes character/scene name, no clickbait caps |
| **Description** | 30% | 80-200 chars, specific scene detail, explains the joke/format, no generic filler |
| **Tags** | 25% | 5-12 relevant tags, covers character, emotion, format, franchise, language |
| **Slug** | 10% | Matches title, kebab-case, under 80 chars, no stop words |
| **Completeness** | 10% | All frontmatter fields present, image path correct, no broken links |

### Common Issues Found

Run `python scripts/audit_memes.py` to auto-detect these:

- **Vague description**: "Funny meme from The Simpsons" → lacks specific scene detail
- **Title too long**: `"Homer Simpson Strangling Bart Simpson While Marge Watches In Horror And Lisa Plays Saxophone"` → truncate
- **Title too short**: `"Simpsons"` → not descriptive enough for search
- **Tag starvation**: Only 2 generic tags → expand to 5-12 targeted tags
- **Missing format tag**: No `reaction`, `template`, `macro`, or `shitpost` tag
- **Emoji or special chars in title**: `"🔥HOMER🔥"` → remove, not SEO-friendly
- **Description = title**: Redundant body where description just repeats title
- **ALL CAPS title**: `"GOLAZO!!!!"` → sentence case for readability

## Step 3: Apply SEO Improvements

Rewrite metadata following the standards in `references/seo-standards.md`. Key rules:

### Title Optimization

**Before**: `"GOLAZO DEL FIDEO EN EL CLÁSICO!!!!"`
**After**: `"Messi Scores Iconic Goal in El Clásico — Football Celebration Meme"`

Rules:
- 30-70 characters ideal
- Lead with the most recognizable name (character, person, format)
- Include format type if applicable: "Template", "Reaction", "Macro", "Shitpost"
- Use sentence case, not ALL CAPS
- Remove excessive punctuation (!!!, ???)
- Make it searchable — think "what would someone type to find this?"

### Description Optimization

**Before**: `"Classic yellow rage face meme showing exaggerated screaming expression"`
**After**: `"Yellow rage face character screaming with clenched teeth and wide eyes. Used as a reaction image for intense excitement, frustration, or sports celebration moments. Originates from early 2010s meme culture."`

Rules:
- 80-200 characters
- First sentence: what is visually happening (specific, concrete)
- Second sentence: how it is used (context, reaction type)
- Include franchise/show name for character-based memes
- Mention format origin if known ("from early 2010s 4chan", "Reddit screenshot format")
- No generic filler like "A funny meme showing..."
- No Reddit URL or source clutter in description

### Tag Optimization

**Before**: `["argentina", "simpsons"]`
**After**: `["argentina", "simpsons", "homer-simpson", "bart-simpson", "reaction", "strangling", "angry", "family", "animated", "tv-show", "template", "classic"]`

Rules:
- 5-12 tags minimum
- Categories to cover: **franchise**, **character(s)**, **emotion**, **format type**, **language/region**, **specific action**, **meme era**
- Use kebab-case for multi-word tags: `homer-simpson` not `homer simpson`
- Include the category slug as a tag
- Add discoverability tags: `reaction`, `template`, `shitpost`, `wholesome`, `surreal`
- Include language tag for Spanish content: `spanish`, `argentinian`, `es-AR`
- No redundant tags: if `homer-simpson`, do not also add `homer` alone

### Slug Optimization

Rules:
- Keep existing slug unless it is broken or misleading
- Max 80 characters
- Must match the title conceptually
- Kebab-case only

Run `python scripts/curate_meme.py --source <mdx> --repo <path>` to apply improvements. The script reads the source MDX, applies the optimized metadata, and writes the improved version.

## Step 4: Quality Check

Before staging changes, verify each curated meme against this checklist:

- [ ] Title is 30-70 characters, compelling, and searchable
- [ ] Description is 80-200 characters with specific scene + usage context
- [ ] Tags include 5-12 items covering franchise, character, emotion, format, region
- [ ] Slug is valid kebab-case under 80 characters
- [ ] All original frontmatter fields preserved (author, score, dates, URLs)
- [ ] Image path is unchanged and correct
- [ ] No grammatical errors in rewritten content
- [ ] Tone is consistent — professional but accessible
- [ ] Cultural references are preserved (Argentine slang kept with context)

## Step 5: Git Workflow

After curating all targets:

1. **Create branch**: `curate/<category>-<timestamp>`
   ```bash
   git checkout -b curate/simpsons-20250511
   ```

2. **Stage modified files**:
   ```bash
   git add memes/<category>/<slug>.mdx
   ```

3. **Commit**: Conventional commit format
   ```bash
   git commit -m "curate(simpsons): optimize 15 meme entries for SEO

   - Rewrite titles for searchability (30-70 chars)
   - Expand descriptions with scene details and usage context
   - Enrich tags from 2-3 to 8-12 per meme
   - Standardize metadata quality across category"
   ```

4. **Push**:
   ```bash
   git push origin <branch-name>
   ```

## Output Format

For each curated meme, produce a summary:

```
Meme: homer-strangling-bart
  Title: "Homer Strangles Bart" (28 chars) → "Homer Strangling Bart Rage Template" (36 chars) ✅
  Desc: 45 chars → 142 chars ✅
  Tags: 2 → 9 tags (+7) ✅
  Score: 68/100 → 95/100 (+27)
```

Report aggregate stats at the end:
```
Curated 15 memes in memes/simpsons/
  Average quality score: 62 → 91 (+29)
  Titles improved: 15/15
  Descriptions expanded: 14/15
  Tags enriched: 15/15 (avg +6.3 tags)
```

## Examples

### Example 1: Single Meme

User: "Revisa el meme de homer strangling bart"

Actions:
1. Read `memes/simpsons/homer-strangling-bart.mdx`
2. Audit: title too generic, description short, only 2 tags
3. Rewrite title, description, expand tags
4. Git: `curate/simpsons-homer-strangling-bart-20250511`, commit, push

### Example 2: Category Folder

User: "Mejora los metadatos de los nuevos memes de argentina-football"

Actions:
1. Target `memes/argentina-football/*.mdx`
2. Filter to recent (last 30 days) if specified, else all
3. Audit all, sort by lowest score first
4. Batch curate top 20 (confirm with user if more)
5. Git: `curate/argentina-football-20250511`, commit, push

### Example 3: Score-Based Filter

User: "Audita los memes de simpsons con score menor a 500"

Actions:
1. Find `memes/simpsons/*.mdx` where score < 500
2. Run full audit, report each meme's issues
3. Curate in priority order
4. Git: `curate/simpsons-low-score-20250511`

## Troubleshooting

**Error: No memes match the query**
Cause: Category doesn't exist or filter too restrictive.
Solution: List available categories with `ls memes/` and suggest alternatives.

**Error: All memes already high quality (score > 90)**
Cause: Target memes were already curated.
Solution: Report current scores and ask user if they want to re-curate anyway or target different memes.

**Error: Git push fails**
Cause: No SSH key or merge conflict.
Solution: Verify SSH auth. If conflict, pull latest main first or use unique branch name.

**Error: User requests full repo audit**
Cause: Scope too broad (1000+ memes).
Solution: Explain the scope rule. Suggest starting with worst-performing category or highest-impact subset.
