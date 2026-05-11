# Quality Rubric for Meme Curation

Detailed scoring system for evaluating and improving meme metadata quality.

## Scoring System (0-100)

### Title Score (0-25 points)

| Points | Criteria |
|--------|----------|
| 25 | 30-70 chars, specific character/scene, searchable, proper case |
| 20 | Good but slightly short (< 30) or slightly long (70-80) |
| 15 | Generic or vague, missing character name, or too short (< 20) |
| 10 | Overly long (> 80), ALL CAPS, emoji, or clickbait-style |
| 5 | Extremely short (< 10) or single word |
| 0 | Missing title |

### Description Score (0-30 points)

| Points | Criteria |
|--------|----------|
| 30 | 80-200 chars, specific visual detail, usage context, no filler |
| 25 | Good but slightly short (60-80) or slightly long (200-250) |
| 20 | Has visual detail but lacks usage context |
| 15 | Generic filler ("A meme showing..."), no specific detail |
| 10 | Extremely short (< 40) or just repeats title |
| 5 | Missing or nonsensical |
| 0 | Missing description |

### Tags Score (0-25 points)

| Points | Criteria |
|--------|----------|
| 25 | 8-12 well-chosen tags covering all categories |
| 20 | 5-7 tags, covers main categories |
| 15 | 3-4 tags, missing key categories |
| 10 | 1-2 tags, barely useful |
| 5 | Tags present but irrelevant or poorly formatted |
| 0 | No tags |

### Slug Score (0-10 points)

| Points | Criteria |
|--------|----------|
| 10 | Matches title, kebab-case, < 80 chars |
| 7 | Good but slightly mismatched with title |
| 5 | Acceptable but uses underscores or > 80 chars |
| 2 | Auto-generated hash or random string |
| 0 | Missing slug |

### Completeness Score (0-10 points)

| Points | Criteria |
|--------|----------|
| 10 | All fields present: title, description, author, subreddit, category, slug, score, created_at, source_url, post_url, image, tags |
| 7 | Missing 1-2 optional fields (e.g., source_url) |
| 5 | Missing 3+ fields but core present |
| 2 | Core fields missing (title, description, tags) |
| 0 | Empty or broken frontmatter |

## Score Interpretation

| Range | Grade | Action |
|-------|-------|--------|
| 90-100 | A+ | Excellent, no changes needed |
| 80-89 | A | Good, minor tweaks optional |
| 70-79 | B | Acceptable, could benefit from polish |
| 60-69 | C | Below standard, needs improvement |
| 50-59 | D | Poor, significant rewrite needed |
| 0-49 | F | Critical, full rewrite required |

## Priority Queue

When curating multiple memes, sort by lowest score first to maximize impact.

Use the `--sort-by-score` flag in `audit_memes.py` to generate a priority-sorted list.

## Common Score Distributions

Based on analysis of 1000+ OpenMeme entries:

- **Median score**: 68 (B range)
- **Most common issue**: Tag starvation (2-3 tags instead of 8-12)
- **Second most common**: Generic descriptions lacking visual specificity
- **Easiest fix**: Expanding tags (+15 points average)
- **Highest impact fix**: Rewriting descriptions (+20 points average)
