# OpenMeme - Craft Rules

## Quality Manifesto for the OpenMeme Repository

> *"A good meme doesn't just make you laugh -- it tells a story, captures a cultural moment, and connects with the community."*

---

## 1. What Is a "Good Meme" for OpenMeme?

### 1.1 Positive Definition

A good meme on OpenMeme satisfies **at least 3** of the following criteria:

| Criterion | Description |
|-----------|-------------|
| **Cultural relevance** | Reflects a current or timeless cultural moment, trend, or reference |
| **Originality** | Presents an original perspective, format, or edit |
| **Emotional connection** | Generates an authentic emotional response (laughter, nostalgia, empathy) |
| **Technical quality** | Clear, readable image with adequate resolution (min 400x400px) |
| **Clear context** | The meme is understandable without extensive explanation |
| **Appropriate format** | Uses standard formats: JPG, PNG, WebP. GIF only when necessary |

### 1.2 Anti-Patterns (Warning Signs)

**REJECT** content that displays:

- [x] **Spam / Mass repurpose**: Same meme with minimal variations posted repeatedly
- [x] **Low visual quality**: Blurry images, excessively compressed, with intrusive watermarks
- [x] **Offensive content**: Racism, sexism, xenophobia, or any form of discrimination
- [x] **Explicit violence**: Gore, NSFW content, or disturbing material without context
- [x] **Misinformation**: Memes that spread false or misleading information presented as fact
- [x] **Promotional content**: Commercial spam or self-promotion disguised as a meme
- [x] **AI Slop**: AI-generated content without creative value, originality, or meaningful human context

---

## 2. Category System

Categories must be **specific** and **useful**. Avoid overly broad categories.

### 2.1 Main Categories

```
funny          - General humor, visual jokes
wholesome      - Positive content, feel-good
politics       - Political satire and social commentary
gaming         - Video games, gamer culture
tech           - Technology, programming, internet
culture        - Cultural references, music, movies, TV
relatable      - Universal everyday situations
absurd         - Surreal humor, anti-humor, shitposting
argentina      - Memes specific to Argentine culture
other          - Last-resort category (use with justification)
```

### 2.2 Creating New Categories

To create a new category, you need:

1. **Justification**: Why don't existing categories cover this content?
2. **Minimum examples**: At least 10 memes that fit the proposed category
3. **Approval**: PR with explanation and maintainer review

---

## 3. Technical Acceptance Criteria

### 3.1 Image

| Attribute | Requirement | Recommended |
|-----------|-------------|-------------|
| Format | JPG, PNG, WebP | WebP for new uploads |
| Minimum resolution | 400x400 px | 800x800 px or higher |
| Maximum size | 10 MB | < 2 MB |
| Quality | No excessive compression | 85%+ JPEG quality |

### 3.2 Metadata (Frontmatter)

All fields are **required** unless otherwise indicated:

```yaml
---
title:        "string - Descriptive title, max 200 chars"
description:  "string - Context / memeticness description"
author:       "string - Reddit username (u/username) or 'unknown'"
subreddit:    "string - Source subreddit (without r/)"
category:     "string - Must exist in the category list"
slug:         "string - URL-safe, lowercase, a-z0-9-"
score:        "number - Original post upvotes"
created_at:   "ISO 8601 - Post creation date"
source_url:   "Direct URL to the image"
post_url:     "Reddit post URL"
image:        "string - Relative filename (./name.jpg)"
tags:         "string[] - Minimum 1 tag (category counts)"
---
```

### 3.3 Commit Messages

Standard format for meme commits:

```
Add {N} memes from r/{subreddit} batch {N} [{category1}({count1}), {category2}({count2})]
```

Example:
```
Add 5 memes from r/argentina batch 3 [politics(2), funny(3)]
```

---

## 4. Review Process

### 4.1 Via Scraper (Automatic)

1. The scraper extracts posts from Reddit per configuration
2. The classifier (Claude / Codex) categorizes automatically
3. The guard script validates quality before committing
4. An automatic PR is created with the collected memes

### 4.2 Via CLI (Manual)

1. User runs `openmeme add <image>`
2. The image is validated against technical criteria
3. Metadata is filled in (interactive or via flags)
4. The guard script verifies before commit
5. A standard commit message is created

### 4.3 Via Direct PR

1. Fork the repository
2. Add memes in the correct structure: `memes/{category}/{slug}.{ext}`
3. Include an MDX file with complete frontmatter
4. Run `npx tsx scripts/src/guard.ts --all`
5. Create a PR with a description that includes:
   - Meme source (Reddit URL)
   - Category justification (if new)
   - Relevant cultural context

---

## 5. Anti-AI-Slop Checklist

Adapted from the Nexu anti-AI-slop checklist and Open Design practices:

### 5.1 Signs of Low-Quality AI-Generated Content

- [ ] Does the image have semantic sense or is it visual "soup"?
- [ ] Is the text in the image readable and grammatically correct?
- [ ] Does the meme require real cultural context (not generic)?
- [ ] Does the image have coherent elements (hands, text, proportions)?
- [ ] Is the humor specific and situational rather than generic?

### 5.2 AI Policy at OpenMeme

| AI Use | Policy |
|--------|--------|
| Automatic classification | Allowed (scraper) |
| Metadata generation | Allowed (with human review) |
| Image generation | Requires explicit labeling |
| Pure AI memes (no added value) | Not allowed |
| Upscaling / restoration | Allowed |
| Metadata translation | Allowed |

---

## 6. Naming Conventions

### 6.1 Files

```
memes/
  {category}/
    {slug}.{ext}           # Image file
    {slug}.mdx              # English metadata (default)
    {slug}.{locale}.mdx     # Translations (optional)
```

### 6.2 Slug

- Lowercase, numbers, and hyphens only
- Maximum 80 characters
- Must be descriptive of the content
- Do not include category in the slug

Examples:
```
milei-chainsaw-explainer.jpg           ✅
argentina-wins-world-cup.jpg           ✅
IMG_2024_001.jpg                       ❌  (not descriptive)
meme-funny-lol.jpg                     ❌  (too generic)
ArgentinA_MeMe.jpg                     ❌  (uppercase and underscores)
```

---

## 7. Internationalization (i18n)

### 7.1 Base Language

- **English** is the default language for all metadata
- Translations are optional but welcome

### 7.2 Supported Locales

```
en      - English (default)
es-AR   - Español rioplatense (Argentina)
es      - Español general
pt-BR   - Português brasileiro
fr      - Français
de      - Deutsch
```

### 7.3 Generating Localized Prompts

To add a new language to the classifier:

```bash
npx tsx tools/dev/src/index.ts generate-prompt {locale} \
  --output prompts/prompt.{locale}.txt
```

---

## 8. Visual Style Guide

### 8.1 Consistency

- Maintain consistency in metadata structure
- Use the same date format (ISO 8601)
- Always include attribution to the original author

### 8.2 Attribution

```yaml
author: "u/username"          # Reddit user
# or
author: "[deleted]"           # Deleted account
# or
author: "unknown"             # Unknown origin
```

### 8.3 Tags

- Minimum 1 tag (category is included automatically)
- Recommended maximum: 8 tags
- Use descriptive and specific tags
- Avoid redundant synonyms

---

## 9. Repository Maintenance

### 9.1 Periodic Cleanup

Review quarterly:
- [ ] Memes with incomplete metadata
- [ ] Orphan images (no MDX)
- [ ] Orphan MDX files (no image)
- [ ] Duplicate content (SHA1)
- [ ] Underused categories (< 5 memes)

### 9.2 Health Metrics

```
Target: > 90% of memes with complete metadata
Target: < 5% duplicate content
Target: < 2% memes without valid categorization
Target: 100% author attribution
```

---

## 10. References

- [Open Design Validation Scripts](https://opendesign.systems/)
- [Nexu Anti-AI-Slop Checklist](https://nexu.dev/)
- [Reddit Content Policy](https://www.redditinc.com/policies/content-policy)
- [Creative Commons Licensing](https://creativecommons.org/)

---

*Last updated: 2026-05-10*  
*Version: 1.0.0*  
*Maintained by: OpenMeme Contributors*
