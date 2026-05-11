---
name: i18n-localizer
description: Translate and culturally adapt OpenMeme meme entries for international audiences. Creates localized .{locale}.mdx variants with culturally-aware descriptions, context explanations, and adapted humor. Use when the user asks to translate memes, localize memes, create cultural adaptations, add locale variants, or convert memes to another language. Triggered by phrases like "translate memes to X", "localize memes for Y", "adapt memes culturally", "create spanish/french/german/english versions of memes", or mentions of specific locales like es-AR, pt-BR, en-US, etc. Requires a meme search term or name and a target locale or country name.
---

# I18n Localizer

Culturally-aware meme localization engine for OpenMeme. Takes meme entries from `memes/` and produces `{slug}.{locale}.mdx` files with translations that preserve humor while adapting cultural references for the target audience.

## Workflow

Meme localization involves these steps:

1. **Resolve target locale** — convert country name or locale input to standard locale code
2. **Find matching memes** — search `memes/` for entries matching the query
3. **Analyze cultural context** — identify references that need adaptation
4. **Generate localized MDX** — create `slug.{locale}.mdx` with translated content
5. **Git workflow** — create branch, commit, and push

## Step 1: Resolve Target Locale

Convert the user's locale input to a standard BCP-47 locale code.

- Direct locale codes pass through: `es-AR`, `pt-BR`, `en-US`, `fr-FR`
- Country names resolve via `references/locale-codes.md`
- Region hints resolve to dominant locale: `argentina` -> `es-AR`, `brazil` -> `pt-BR`

If the locale cannot be resolved, ask the user for clarification. Do not guess.

## Step 2: Find Matching Memes

Search the `memes/` directory in the OpenMeme repository.

Run `python scripts/find_memes.py <query>` to search by:
- Meme title or description keywords
- Category name (e.g., `simpsons`, `argentina`, `reaction`)
- Tag matching
- Slug partial match

The script returns a JSON array of matching meme paths with their metadata. Review results and confirm with the user which memes to localize if the match count exceeds 10.

**Search rules:**
- Search `memes/` recursively for `.mdx` files
- Match against title, description, category, tags, and slug
- Prioritize exact matches, then partial matches
- Exclude already-localized `.es-AR.mdx` files when the target is `es-AR`
- Read the full MDX content of each match to understand context

## Step 3: Analyze Cultural Context

For each selected meme, analyze what cultural elements need adaptation:

**Common adaptation types:**
| Type | Example | Adaptation |
|------|---------|------------|
| Untranslated cultural phrase | "chota" | Explain with regional context |
| Argentina-specific reference | "AFIP", "carpincho" | Add parenthetical explanation |
| Local humor pattern | Self-deprecating economic jokes | Adapt tone for target culture |
| Untranslated Spanish in English meme | "Que?" | Contextualize for target locale |
| Country-specific visual context | Argentine flags, currency | Note in description |

**When adapting:**
- Keep the original humor intent
- Add cultural context that the target audience would not know
- Do not over-explain universally understood references
- Preserve character names, show names, and proper nouns
- Translate descriptive text, not quoted dialogue unless needed

## Step 4: Generate Localized MDX

Create `{slug}.{locale}.mdx` following the reference template in `references/mdx-template.md`.

**Content generation rules:**

1. **Title**: Translate or adapt the title for the target locale
2. **Description**: Full translation with cultural notes in parentheses for obscure references. Keep it natural — not a footnote explosion.
3. **Locale field**: Set to the resolved locale code (e.g., `es-AR`, `pt-BR`)
4. **Tags**: Translate tags where cultural equivalents exist; keep universal tags in English
5. **Body content**: Translate the body text. Add a `# Contexto Cultural` (or localized equivalent) section at the end when the meme requires significant cultural explanation

**Special handling for `es-AR` (Argentine Spanish):**
- Use voseo (`vos` instead of `tu`)
- Include Argentine slang where natural (boliche, che, piola)
- Keep references to Argentine culture (mate, asado, fútbol) with brief context
- Use Argentine monetary references (pesos) where the original uses them

**Special handling for `pt-BR` (Brazilian Portuguese):**
- Use Brazilian Portuguese conventions
- Adapt Argentine cultural references to Brazilian equivalents where appropriate
- Keep humor pattern but adapt cultural touchstones

**Special handling for `en-US` / `en-GB`:**
- Remove or explain Argentine-specific references
- Use natural English phrasing
- Add context section explaining the cultural background

Run `python scripts/create_localization.py --source <mdx-path> --locale <locale>` to generate the localized MDX file with proper formatting.

## Step 5: Git Workflow

After generating all localized files:

1. **Create branch**: Use descriptive naming — `i18n/locale-slug-timestamp`
   ```bash
   git checkout -b i18n/es-AR-simpsons-20250511
   ```

2. **Stage files**: Add only the new `{locale}.mdx` files
   ```bash
   git add memes/<category>/<slug>.<locale>.mdx
   ```

3. **Commit**: Use structured commit message
   ```bash
   git commit -m "i18n(es-AR): localize simpsons memes

   - Add cultural context for Argentine audience
   - Adapt 15 meme descriptions with local references
   - Translate tags and titles"
   ```

4. **Push**:
   ```bash
   git push origin <branch-name>
   ```

Run `python scripts/create_localization.py --push` to automate the full git workflow after generation.

## Cultural Adaptation Guidelines

### Reference: Argentine Culture in Memes

Common Argentine references found in OpenMeme and how to handle them:

| Reference | Type | Notes for Translation |
|-----------|------|----------------------|
| AFIP | Government agency | Keep name, add "(tax agency)" or equivalent |
| Carpincho | Animal + symbol | "capybara" + note on Argentine meme culture |
| Chota | Slang (context-dependent) | Keep with parenthetical explanation |
| River Plate / Boca | Football clubs | Keep names, add "(football club)" |
| Mate | Cultural practice | Keep "mate" + brief description |
| Asado | Social gathering | "barbecue" or keep with explanation |
| Crónica TV | TV channel | Keep name + "(Argentine news channel)" |
| Voseo (vos) | Linguistic | Maintain in es-AR, convert to tú for other Spanish |

### When to Add Cultural Context Section

Add a `# Contexto Cultural` section when:
- The meme references an Argentine event, person, or institution
- The humor depends on understanding Argentine social dynamics
- Visual elements carry cultural meaning (flags, currency, locations)
- The original post's Reddit comments reveal cultural context

Keep the section concise — 2-4 bullet points maximum.

## Quality Checklist

Before committing, verify each localized MDX:

- [ ] Frontmatter fields are complete and accurate
- [ ] Title is translated naturally for the target locale
- [ ] Description preserves humor while adding necessary context
- [ ] Tags are relevant and translated where appropriate
- [ ] Image reference points to the correct original file
- [ ] Cultural references are explained without over-explaining
- [ ] Tone matches the original (funny stays funny, sarcastic stays sarcastic)
- [ ] No literal translations that lose the joke

## Examples

### Example 1: Simpsons to es-AR

User: "Translate the simpsons memes to argentina spanish"

Actions:
1. Resolve: `argentina spanish` -> `es-AR`
2. Find: Search `memes/simpsons/` for `.mdx` files without `.es-AR.` variant
3. Generate: For each match, create `.es-AR.mdx` with Argentine Spanish adaptations
4. Git: Create `i18n/es-AR-simpsons-20250511`, commit, push

### Example 2: Specific Meme to Multiple Locales

User: "Localize 'always-has-been' memes for Brazil and USA"

Actions:
1. Resolve: `Brazil` -> `pt-BR`, `USA` -> `en-US`
2. Find: Search `memes/always-has-been/` for base `.mdx` files
3. Generate: Create both `.pt-BR.mdx` and `.en-US.mdx` variants
4. Git: Single branch `i18n/multi-always-has-been-20250511`

## Troubleshooting

**Error: No matching memes found**
Cause: Search term too specific or memes don't exist in that category.
Solution: Try broader search terms or check `memes/` structure with `ls memes/`.

**Error: Locale not recognized**
Cause: Country name or locale code not in references.
Solution: Ask user for the standard locale code (e.g., `es-MX`, `de-DE`).

**Error: Git push fails**
Cause: No SSH key configured or branch already exists.
Solution: Verify `git remote -v` shows SSH URL. If branch exists, pull first or use unique branch name.

**Error: Target .{locale}.mdx already exists**
Cause: Localization was already done for that meme and locale.
Solution: Skip that file. Report count of skipped vs. newly created.
