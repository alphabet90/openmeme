# CLI Reference

OpenMeme provides multiple command-line interfaces for different workflows:

| Command | Package | Purpose |
|---------|---------|---------|
| `pnpm scrape` | `@openmeme/scraper` | Reddit scraper pipeline |
| `pnpm guard` | `@openmeme/scripts` | Pre-commit validation |
| `pnpm sync` | `@openmeme/scripts` | Reddit auto-sync |
| `pnpm optimize` | `@openmeme/scripts` | Image optimization |
| `pnpm add-meme` | `@openmeme/cli` | Add meme interactively |
| `pnpm validate` | `@openmeme/cli` | Validate all memes |
| `pnpm stats` | `@openmeme/cli` | Repository statistics |
| `pnpm import` | `@openmeme/cli` | Import from URL |

---

## Scraper (`pnpm scrape`)

```bash
pnpm scrape --subreddit argentina --limit 100
pnpm --filter @openmeme/scraper scrape -- --subreddit argentina
```

### Feed Selection

| Flag | Default | Description |
|---|---|---|
| `--subreddit NAME` | `argentina` | Subreddit to scrape |
| `--limit N` | `100` | Max posts to scan |
| `--sort {hot,new,top}` | `hot` | Feed sort order |
| `--timeframe {hour,day,week,month,year,all}` | `day` | Time window for `top` |
| `--page N` | `1` | Start page |

### Processing

| Flag | Default | Description |
|---|---|---|
| `--batch-size N` | `10` | Images per git commit batch |
| `--classifier {claude,codex}` | `claude` | AI vision classifier |
| `--locale LOCALE` | `en` | Prompt locale |
| `--classify-workers N` | `4` | Parallel classifier processes |
| `--min-comment-upvotes N` | `0` | Min upvotes for comment images |
| `--dry-run` | off | Classify without saving |
| `--no-branch` | off | Skip auto branch creation |
| `--skip-content-dedup` | off | Disable SHA1 dedup |

### Examples

```bash
# Basic scrape
pnpm scrape --subreddit argentina

# Newest 50
pnpm scrape --subreddit argentina --sort new --limit 50

# Top of week, dry run
pnpm scrape --subreddit argentina --sort top --timeframe week --dry-run

# Single post
pnpm scrape --post-url https://old.reddit.com/r/argentina/comments/abc123/title/

# Reset and reprocess
pnpm scrape --subreddit argentina --reset-bloom --dry-run
```

---

## Guard (`pnpm guard`)

Pre-commit validation for meme quality.

```bash
pnpm guard --staged          # Validate staged files only
pnpm guard --all             # Validate all memes
pnpm guard --strict          # Treat warnings as errors
pnpm guard --verbose         # Detailed output
pnpm guard --rules           # List all validation rules
```

**Validation rules**: image exists, valid extension, title present, description present, valid source URL, category present, tags present, valid slug, author present, subreddit present.

---

## Sync (`pnpm sync`)

Automated Reddit synchronization.

```bash
pnpm sync --subreddit argentina --limit 50
pnpm sync --config sync.config.json
pnpm sync --daemon --interval 3600
```

| Flag | Description |
|---|---|
| `--config <path>` | Load multi-subreddit config |
| `--subreddit <name>` | Single subreddit |
| `--limit N` | Max posts |
| `--sort {hot,new,top}` | Sort order |
| `--classifier {claude,codex}` | Classifier backend |
| `--daemon` | Run continuously |
| `--interval <seconds>` | Daemon interval (default 3600) |
| `--init-config` | Create sample config file |

**Sample config** (`sync.config.json`):
```json
{
  "subreddits": [
    { "name": "argentina", "limit": 100 },
    { "name": "memes", "limit": 50, "sort": "hot" },
    { "name": "dankmemes", "limit": 50, "sort": "hot" }
  ]
}
```

---

## Optimize (`pnpm optimize`)

Image compression and size budget enforcement.

```bash
pnpm optimize --all            # Optimize all images
pnpm optimize --staged         # Optimize staged files
pnpm optimize --webp           # Convert to WebP
pnpm optimize --avif           # Convert to AVIF
pnpm optimize --quality 85     # JPEG/WebP quality
pnpm optimize --budget 100MB   # Enforce size budget
pnpm optimize --dry-run        # Preview without modifying
pnpm optimize --report         # Generate JSON report
```

---

## CLI Utility (`pnpm add-meme`)

Interactive meme management.

### `openmeme add [image]`
```bash
pnpm add-meme ./image.jpg --title "Meme Title" --category funny
pnpm add-meme --batch ./folder/ --category other
pnpm add-meme --interactive
```

### `openmeme list`
```bash
pnpm stats list --category funny --limit 50
pnpm stats list --json
```

### `openmeme search <query>`
```bash
pnpm stats search "politics" --tag
```

### `openmeme validate`
```bash
pnpm validate --strict --fix
```

### `openmeme stats`
```bash
pnpm stats --json
```

### `openmeme import <url>`
```bash
pnpm stats import https://reddit.com/r/argentina/comments/abc123/
```

---

## Dev Tools (`pnpm dev-tools`)

```bash
pnpm --filter @openmeme/dev lint --fix              # Lint MDX frontmatter
pnpm --filter @openmeme/dev generate-prompt es-AR   # Generate classifier prompt
pnpm --filter @openmeme-dev benchmark               # Benchmark pipeline
pnpm --filter @openmeme-dev db-check                # Check consistency
pnpm --filter @openmeme-dev setup-hooks             # Install git hooks
```

---

## Exit Codes

| Code | Meaning |
|---|---|
| `0` | Success |
| `1` | Runtime error |
| `2` | Argument error (argparse) |
size → save/commit
flush remaining memes at the end
```

```bash
python main.py --subreddit argentina --per-post
python main.py --subreddit argentina --per-post --batch-size 5
```

Best when you want to see results sooner during a long scrape, or when individual posts tend to have many images.

---

## Classifiers

The `--classifier` flag selects which vision-capable CLI tool performs meme classification. Both backends use the same JSON output schema and retry logic (2 attempts, 120 s timeout each).

### `claude` (default)

Spawns the [Claude Code CLI](https://claude.ai/code) via subprocess with `--tools Read` (enables reading the image file) and `--dangerously-skip-permissions`.

```bash
python main.py --classifier claude
```

**Prerequisites**: `claude` must be in PATH and authenticated.

### `codex`

Spawns the [OpenAI Codex CLI](https://developers.openai.com/codex/cli/features#image-inputs) via subprocess, passing the image with `--image`.

```bash
python main.py --classifier codex
```

**Prerequisites**: `codex` must be in PATH and authenticated.

### Adding a custom classifier

Subclass `BaseClassifier` from `src/classifiers/base.py`, implement `classify_image(image_path, url) -> ClassificationResult`, register it in `src/classifiers/__init__.py` and add it to the `_backends` dict in `main.py`.

---

## Locale Prompts

The `--locale` flag selects which prompt file is sent to the classifier. This controls the language of the instructions and the output fields (descriptions, tags, etc.) returned by the model.

### Prompt file resolution

Prompt files live in `prompts/` at the repository root and follow the naming convention `prompt.{locale}.txt`, where the locale uses BCP 47 format with a hyphen separator (e.g. `es-AR`, not `es_AR`).

The loader normalizes the input automatically — `es_AR`, `es-ar`, and `es-AR` all resolve to `prompt.es-AR.txt`.

**Fallback order** (first file found wins):

1. `prompts/prompt.{locale}.txt` (e.g. `prompt.es-AR.txt`)
2. `prompts/prompt.{language}.txt` (e.g. `prompt.es.txt`)
3. `prompts/prompt.en.txt`

A warning is logged when a fallback is used.

### Examples

```bash
# Use Spanish (Argentina) prompt
python main.py --subreddit argentina --locale es_AR

# Use English prompt explicitly
python main.py --subreddit dankmemes --locale en

# Codex backend with locale
python main.py --subreddit argentina --classifier codex --locale es-AR
```

### Adding a new locale

Create `prompts/prompt.{locale}.txt` following BCP 47 naming. The file must be a plain-text prompt template. Use `{image_path}` as a placeholder where the classifier should insert the path to the image being analyzed.

The prompt should instruct the model to respond with a JSON object containing:

| Field | Type | Description |
|---|---|---|
| `is_meme` | boolean | Whether the image is a meme |
| `title` | string | Well-known or inferred meme name (3–6 words, title case) |
| `category` | string | Lowercase label for the meme type |
| `filename_slug` | string | 3–7 kebab-case words describing the content |
| `description` | string | One short sentence describing the visual content |
| `tags` | array of strings | 5–10 searchable lowercase keywords |

---

## Sort Modes

### `hot` (default)

Fetches the current hot feed — posts ranked by Reddit's score and recency algorithm.

```
GET https://old.reddit.com/r/{subreddit}/.json
```

### `new`

Fetches posts in strict reverse-chronological order (newest first).

```
GET https://old.reddit.com/r/{subreddit}/new/.json
```

### `top`

Fetches top-scoring posts within a time window set by `--timeframe`.

```
GET https://old.reddit.com/r/{subreddit}/top/.json?sort=top&t={timeframe}
```

#### Timeframe values

| `--timeframe` | Window |
|---|---|
| `hour` | Past hour |
| `day` | Past 24 hours *(default)* |
| `week` | Past week |
| `month` | Past month |
| `year` | Past year |
| `all` | All time |

> `--timeframe` is silently ignored when `--sort` is `hot` or `new`.

---

## Page Navigation

Reddit paginates its feeds in chunks of 25 posts. `--page N` tells the scraper to skip to page N before starting to collect posts.

**How it works:**

- Pages 1 through N-1 are traversed in sequence to obtain Reddit's pagination cursor (`after`).
- Each traversal page costs one HTTP request (plus the standard `REQUEST_DELAY`).
- Collection begins at page N and continues until `--limit` posts are gathered (which may span multiple pages beyond N).

**Example — page costs:**

| `--page` | Extra traversal requests |
|---|---|
| 1 (default) | 0 |
| 2 | 1 |
| 4 | 3 |
| 10 | 9 |

**Warnings:**

- If the subreddit has fewer pages than requested, the scraper logs a warning and exits with 0 memes saved.
- `--page` cannot be combined with `--from-file` or `--post-url` (exits with code 2).

---

## Configuration (`.env`)

Copy `.env.example` to `.env` and set these variables to avoid passing them on every run:

```dotenv
SUBREDDIT=argentina
REPO_PATH=/path/to/your/repo
```

CLI flags always override `.env` values.

---

## Restoring the Bloom Filter

If `processed.bloom` is lost or corrupted the pipeline "forgets" all prior work and would re-download and re-classify every image on the next run. `--restore-bloom` reconstructs the filter from the memes that are already saved on disk without re-running the pipeline.

```bash
python main.py --restore-bloom
```

**What it recovers:**

- Image URLs (`source_url` from each `.mdx` sidecar file)
- Reddit post IDs (derived from `post_url` in each `.mdx`)
- SHA1 content hashes (computed directly from each image file)

**What it cannot recover:**

- Images that were downloaded and classified as *not* a meme — those were never saved, so the pipeline may re-classify them on the next run. They will be rejected again without being saved, so there is no harm.

**Typical use cases:**

- Bloom filter file accidentally deleted or corrupted
- Restoring state after cloning the repo onto a new machine (where only the `memes/` folder was synced via git)
- Recovering from a failed `--reset-bloom` run

**Combining with `--reset-bloom`:**

Passing both flags first deletes the existing filter, then immediately rebuilds it from saved memes:

```bash
python main.py --reset-bloom --restore-bloom
```

---

## Exit Codes

| Code | Meaning |
|---|---|
| `0` | Success (even if zero memes were found) |
| `2` | Argument error (printed by argparse — e.g. incompatible flags) |
| `1` | Runtime error (unhandled exception) |
