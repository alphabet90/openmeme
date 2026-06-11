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
| `pnpm generate-prompt` | `@openmeme/dev` | Generate classifier prompt |
| `pnpm benchmark` | `@openmeme/dev` | Benchmark pipeline |
| `pnpm db-check` | `@openmeme/dev` | Check repo consistency |
| `pnpm setup-hooks` | `@openmeme/dev` | Install git hooks |

---

## Scraper (`pnpm scrape`)

```bash
pnpm scrape --subreddit argentina --limit 100
pnpm --filter @openmeme/scraper scrape -- --subreddit argentina
```

### Feed Selection

| Flag | Default | Description |
|---|---|---|
| `-s, --subreddit NAME` | `argentina` | Subreddit to scrape |
| `-l, --limit N` | `100` | Max posts to scan |
| `--sort {hot,new,top}` | `hot` | Feed sort order |
| `--timeframe {hour,day,week,month,year,all}` | `day` | Time window for `top` |
| `--page N` | `1` | Start page |
| `--post-url <url>` | — | Scrape a single Reddit post |
| `--from-file <path>` | — | Read post URLs from file |

### Processing

| Flag | Default | Description |
|---|---|---|
| `-b, --batch-size N` | `10` | Images per git commit batch |
| `-c, --classifier {claude,codex}` | `claude` | AI vision classifier |
| `--locale LOCALE` | `en` | Prompt locale |
| `-w, --classify-workers N` | `4` | Parallel classifier processes |
| `--min-comment-upvotes N` | `0` | Min upvotes for comment images |
| `--per-post` | off | Commit after each post instead of batch |
| `--dry-run` | off | Classify without saving |
| `--no-branch` | off | Skip auto branch creation |
| `--skip-content-dedup` | off | Disable SHA1 dedup |
| `--reset-bloom` | off | Delete Bloom filter before run |
| `--restore-bloom` | off | Rebuild Bloom filter from saved memes |
| `--repo-path <path>` | `REPO_PATH` | Repository root path |

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

# Restore Bloom filter from saved memes
pnpm scrape --restore-bloom
```

### Classifiers

The `--classifier` flag selects which vision-capable CLI tool performs meme classification:

- `claude` (default): spawns the `claude` CLI with `--tools Read`.
- `codex`: spawns the `codex` CLI with `--image`.

Both require the respective CLI to be installed and authenticated.

### Locale Prompts

Prompt files are expected at `packages/scraper/prompts/prompt.{locale}.txt`. The loader normalizes locale codes and falls back to language-only and then English. If no prompt file exists, the classifier uses a hard-coded fallback prompt. Generate a new locale prompt with:

```bash
pnpm generate-prompt es-AR
```

---

## Scraper Validation (`pnpm --filter @openmeme/scraper validate`)

```bash
pnpm --filter @openmeme/scraper validate --path ./memes
```

| Flag | Default | Description |
|---|---|---|
| `-p, --path <path>` | `./memes` | Path to meme collection |

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
| `-c, --config <path>` | Load multi-subreddit config |
| `-s, --subreddit <name>` | Single subreddit |
| `-l, --limit N` | Max posts |
| `--sort {hot,new,top}` | Sort order |
| `--timeframe <tf>` | Timeframe for `top` |
| `--batch-size N` | Commit batch size |
| `--classifier {claude,codex}` | Classifier backend |
| `--locale LOCALE` | Prompt locale |
| `--workers N` | Classifier workers |
| `--daemon` | Run continuously |
| `-i, --interval <seconds>` | Daemon interval (default 3600) |
| `--dry-run` | Preview without saving |
| `--init-config` | Create sample `sync.config.json` |
| `-v, --verbose` | Detailed output |

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

| Flag | Default | Description |
|---|---|---|
| `-a, --all` | — | Optimize all images |
| `-s, --staged` | — | Optimize staged images |
| `-p, --path <path>` | `./memes` | Path to meme collection |
| `-q, --quality N` | `85` | JPEG/WebP quality |
| `--webp` | — | Convert to WebP |
| `--avif` | — | Convert to AVIF |
| `--max-width N` | `1200` | Max width |
| `--max-height N` | `1200` | Max height |
| `--strip-metadata` | `true` | Strip metadata |
| `--budget <size>` | — | Size budget (e.g. `100MB`, `1GB`) |
| `--threshold <bytes>` | `102400` | Only process files larger than threshold |
| `--dry-run` | — | Preview only |
| `--report` | — | Write `optimization-report.json` |
| `-v, --verbose` | — | Detailed output |

---

## CLI Utility (`openmeme`)

Entry binary: `tools/cli/dist/index.js`. Root exposes it as `pnpm add-meme`, `pnpm validate`, `pnpm stats`, and `pnpm import`.

### `openmeme add [image]`
```bash
pnpm add-meme ./image.jpg --title "Meme Title" --category funny
pnpm add-meme --batch ./folder/ --category other
pnpm add-meme --interactive
```

| Flag | Description |
|---|---|
| `-t, --title <title>` | Meme title |
| `-d, --description <text>` | Meme description |
| `-c, --category <cat>` | Category folder |
| `--subreddit <sub>` | Source subreddit |
| `--author <author>` | Original author |
| `--tags <tags>` | Comma-separated tags |
| `--url <url>` | Source URL |
| `--batch <dir>` | Batch-add a directory |
| `--interactive` | Force interactive mode |
| `-y, --yes` | Skip confirmations |

### `openmeme list`
```bash
pnpm stats list --category funny --limit 50
pnpm stats list --json
```

| Flag | Description |
|---|---|
| `-c, --category <cat>` | Filter by category |
| `-l, --limit N` | Max results (default 50) |
| `--json` | Output JSON |

### `openmeme search <query>`
```bash
pnpm stats search "politics" --tag
```

| Flag | Description |
|---|---|
| `-t, --tag` | Search tags only |
| `--json` | Output JSON |

### `openmeme validate`
```bash
pnpm validate --strict --fix
```

| Flag | Description |
|---|---|
| `-p, --path <path>` | Path to memes (default `./memes`) |
| `-s, --strict` | Treat warnings as errors |
| `--fix` | Attempt auto-fix |

### `openmeme stats`
```bash
pnpm stats --json
```

| Flag | Description |
|---|---|
| `-p, --path <path>` | Path to memes (default `memes`) |
| `--json` | Output JSON |

### `openmeme import <url>`
```bash
pnpm import https://reddit.com/r/argentina/comments/abc123/
```

| Flag | Description |
|---|---|
| `-t, --title <title>` | Override title |
| `-c, --category <cat>` | Override category |
| `--auto-classify` | Run AI classifier |

---

## Dev Tools (`pnpm --filter @openmeme/dev`)

```bash
pnpm --filter @openmeme/dev lint --fix              # Lint MDX frontmatter
pnpm --filter @openmeme/dev generate-prompt es-AR   # Generate classifier prompt
pnpm --filter @openmeme/dev benchmark               # Benchmark pipeline
pnpm --filter @openmeme/dev db-check                # Check consistency
pnpm --filter @openmeme/dev setup-hooks             # Install git hooks
```

### `generate-prompt <locale>`

```bash
pnpm --filter @openmeme/dev generate-prompt es-AR
pnpm --filter @openmeme/dev generate-prompt pt-BR --output prompts/prompt.pt-BR.txt
```

### `db-check`

```bash
pnpm --filter @openmeme/dev db-check
pnpm --filter @openmeme/dev db-check --orphaned-images --orphaned-mdx
```

### `setup-hooks`

```bash
pnpm --filter @openmeme/dev setup-hooks
```

Installs `pre-commit` and `pre-push` hooks into `.git/hooks/`. Pre-commit runs `scripts/dist/guard.js --staged --verbose`.

---

## Exit Codes

| Code | Meaning |
|---|---|
| `0` | Success (even if zero memes were found) |
| `1` | Runtime error |
| `2` | Argument error |
