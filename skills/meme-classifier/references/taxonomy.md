# Category Taxonomy

Canonical category structure for OpenMeme, derived from analysis of the live repository.

## Current State (Snapshot)

- **Total categories**: ~388
- **Categories with 10+ memes**: ~15 (high-traffic)
- **Categories with 2-9 memes**: ~69 (medium)
- **Categories with 1 meme**: ~303 (below threshold)
- **Empty categories**: ~1

## Canonical Hierarchy

### Tier 1: Franchise Categories (always primary)

Major franchises with dedicated following. These are the primary routing targets.

| Category | Count | Notes |
|----------|-------|-------|
| `simpsons` | 179 | Dominant category; absorb all `simpsons-*` subcategories |
| `reaction` | 126 | General reaction format; absorb overly-specific subcategories |
| `argentina-politics` | 40 | Distinct identity; keep separate |
| `reaccion-espanol` | 31 | Spanish-language reaction memes |
| `argentina-politica` | 29 | Spanish-name variant of argentina-politics; MERGE |
| `cursed-car` | 28 | Strong identity; keep but consider merging into `cursed` |
| `futbol-shitpost` | 27 | Strong identity; keep |
| `argentina-humor` | 27 | General Argentine humor |
| `spongebob` | 10 | Franchise; absorb `spongebob-*` subcategories |
| `wojak` | 9 | Meme character; absorb `wojak-*` subcategories |
| `dragon-ball` | 10 | Franchise total across variants |
| `star-wars` | 20+ | Franchise total across variants |
| `jojo-bizarre-adventure` | 10+ | Franchise total across variants |
| `pepe` | 3 | Low count; keep as canonical frog meme category |

### Tier 2: Format Categories

Content-agnostic format types. Used when no franchise applies.

| Category | Role |
|----------|------|
| `reaction` | General reaction images |
| `reaccion-espanol` | Spanish-language reactions |
| `shitpost` | Absurdist/ironic low-effort |
| `template` | Exploitable formats |
| `cursed` | Unsettling/weird imagery |
| `surreal` | Surreal/absurdist humor |
| `wholesome` | Positive/uplifting |
| `absurdist-humor` | Canonical: absorb `absurd`, `absurd-humor` |
| `comparison` | Side-by-side formats |
| `wordplay` | Text-based humor |
| `meta-meme` | Memes about memes |

### Tier 3: Regional Categories

Used when content is region-specific with no franchise.

| Category | Role |
|----------|------|
| `argentina-humor` | General Argentine humor |
| `argentina-politics` | Argentine political (40 memes, keep) |
| `argentina-reaction` | Argentina-specific reactions |
| `argentina-football` | Argentine football |
| `argentina-shitpost` | Argentine shitposting |
| `latam-humor` | General Latin American |

## Subcategory Absorption Rules

Franchise subcategories with fewer than 10 memes MUST merge into parent.

### simpsons subcategories → simpsons

| Subcategory | Count | Action | Tag additions |
|-------------|-------|--------|---------------|
| `simpsons-argentina` | unknown | MERGE → `simpsons` | add `argentina` |
| `simpsons-argentina-politics` | unknown | MERGE → `simpsons` | add `argentina`, `political` |
| `simpsons-comparison` | unknown | MERGE → `simpsons` | add `comparison` |
| `simpsons-courtroom` | unknown | MERGE → `simpsons` | add `courtroom` |
| `simpsons-krusty` | unknown | MERGE → `simpsons` | add `krusty` |
| `simpsons-milei` | unknown | MERGE → `simpsons` | add `milei`, `political` |
| `simpsons-political-satire` | unknown | MERGE → `simpsons` | add `political`, `satire` |

### star-wars subcategories → star-wars

| Subcategory | Action | Tag additions |
|-------------|--------|---------------|
| `star-wars-dark-humor` | MERGE | add `dark-humor` |
| `star-wars-force-ghost` | MERGE | add `force-ghost` |
| `star-wars-prequel` | MERGE | add `prequel` |
| `star-wars-reaction` | MERGE | add `reaction` |
| `star-wars-spanish-pun` | MERGE | add `spanish`, `pun` |

### dragon-ball subcategories → dragon-ball

| Subcategory | Action | Tag additions |
|-------------|--------|---------------|
| `dragon-ball-argentina-politics` | MERGE | add `argentina`, `political` |
| `dragon-ball-reaction` | MERGE | add `reaction` |

### jojo subcategories → jojo-bizarre-adventure

| Subcategory | Action | Tag additions |
|-------------|--------|---------------|
| `jojo-approaching` | MERGE | add `approaching` |
| `jojo-reference-parody` | MERGE | add `reference`, `parody` |
| `jojo-reaction` | MERGE | add `reaction` |

### wojak subcategories → wojak

| Subcategory | Action | Tag additions |
|-------------|--------|---------------|
| `wojak-argentina-police` | MERGE | add `argentina`, `police` |
| `wojak-argentina-politics` | MERGE | add `argentina`, `political` |
| `wojak-brainlet` | MERGE | add `brainlet` |
| `wojak-creepy` | MERGE | add `creepy` |
| `crying-wojak` | MERGE | add `crying` |
| `npc-wojak` | MERGE | add `npc` |

## Spelling Normalization Map

Categories that differ only in spelling or hyphenation:

| Non-canonical | Canonical |
|---------------|-----------|
| `southpark` | `south-park` |
| `fairly-odd-parents` | `fairly-oddparents` |
| `courage-cowardly-dog` | `courage-the-cowardly-dog` |
| `reaccion-espa-ol` | `reaccion-espanol` |
| `argentina-futbol` | `argentina-football` |
| `futbol-reaction` | `football-reaction` |

## Existence Threshold Targets

Categories with 1 meme migrate as follows:

| Category | Target | Reasoning |
|----------|--------|-----------|
| `a-bugs-life-reaction` | `reaction` | Generic reaction |
| `absurd` | `absurdist-humor` | Semantic match |
| `absurd-humor` | `absurdist-humor` | Semantic match |
| `absurdist-humor` | KEEP (canonical) | Absorbs above |
| `ai-generated-politics` | `political-satire-photoshop` | Format match |
| `american-dad-reaction` | `reaction` | Generic reaction |
| `among-us` | `gaming` | Game category |
| `ancient-aliens` | `history-meme` | Format match |
| `animal-reaction` | `reaction` | Generic |
| `anime-argentina` | `anime` | Franchise + tag |
| `anti-piracy-parody` | `parody` | Format match |
| `archer` | `reaction` | Low-volume show |
| `argentina-afip` | `argentina-humor` | Specific → general |
| `argentina-carpincho` | `argentina-culture` | Culture topic |
| `argentina-clima` | `argentina-humor` | Humor topic |

For the remaining 280+ single-meme categories, use the classification script to determine best target.
