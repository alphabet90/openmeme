# Localized MDX Template

Template for generating `{slug}.{locale}.mdx` files.

## Frontmatter Fields

```yaml
---
title: "<Translated title>"
description: "<Translated description with cultural context in parentheses>"
author: "<Original author - keep unchanged>"
subreddit: "<Original subreddit - keep unchanged>"
category: "<Original category - keep unchanged>"
slug: "<Original slug - keep unchanged>"
locale: "<Target locale code, e.g., es-AR, pt-BR, en-US>"
score: <Original score - keep unchanged>
created_at: "<Original created_at - keep unchanged>"
source_url: "<Original source_url - keep unchanged>"
post_url: "<Original post_url - keep unchanged>"
image: "<Original image path - keep unchanged>"
tags: ["<Translated or adapted tags>"]
---
```

## Body Structure

```markdown
# <Translated Title>

<Translated description>

**Category**: <category> | **Author**: u/<author> | **Score**: <score> upvotes

[View original post on Reddit](<post_url>)
```

## With Cultural Context Section

When the meme requires cultural explanation, append:

```markdown
## <Cultural Context Title (localized)>

- <Bullet point explaining a cultural reference>
- <Bullet point explaining visual or contextual element>
- <Bullet point explaining the humor for the target audience>
```

Example section titles by locale:
- es-AR: `Contexto Cultural`
- pt-BR: `Contexto Cultural`
- en-US / en-GB: `Cultural Context`
- fr-FR: `Contexte Culturel`
- de-DE: `Kultureller Kontext`

## Full Example: es-AR

```yaml
---
title: "Homer Simpson Babeando"
description: "Homer Simpson con la boca abierta babeando, usado para expresar deseo intenso por algo (comida, un objeto, una situacion ideal)."
author: "Tokita_Caju"
subreddit: "argentina"
category: "simpsons"
slug: "homer-simpson-drooling-hungry"
locale: "es-AR"
score: 29
created_at: "2026-05-02T18:39:55Z"
source_url: "https://preview.redd.it/49sb176ysryg1.jpeg?width=800&format=pjpg&auto=webp&s=ad9193ef08854ffedd94936a5d3cb775e22d2f4e"
post_url: "https://reddit.com/r/argentina/comments/1t1y0ht/chota/"
image: "./homer-simpson-drooling-hungry.jpg"
tags: ["argentina", "simpsons", "homer", "babeando", "hambre", "deseo", "reaccion", "ansias", "animado", "clasico"]
---
```

## Tag Translation Reference

Common tag translations for es-AR:

| English | es-AR |
|---------|-------|
| reaction | reaccion |
| funny | divertido / gracioso |
| classic | clasico |
| animated | animado |
| hungry | hambriento / con hambre |
| desire | deseo / ansias |
| craving | antojo / ansias |
| politics | politica |
| football | futbol |
| food | comida |
| angry | enojado / caliente |
| sad | triste |
| happy | feliz |
| confused | confundido |
| surprised | sorprendido |
