# Refero Research-First Design: OpenMeme Application

## Brand Essence

OpenMeme is an open-source meme aggregation and curation platform. The brand conveys **irreverence, energy, and accessibility** — memes are universal content, and the platform treats them with the seriousness they deserve while keeping the experience fun and lightweight.

### Brand Pillars

1. **Open**: Open source, open community, open culture
2. **Energetic**: Lime accents, bold typography, dynamic interactions
3. **Argentine roots**: Celeste blue as secondary, reflecting the project's origin
4. **Community-first**: User-generated, community-curated

## Color System

### Primary Palette

| Token | Hex | Role |
|-------|-----|------|
| `--color-negro` | #0D0D0D | Page background |
| `--color-lima-meme` | #D4FF00 | Primary accent — CTAs, badges, highlights |
| `--color-blanco` | #FFFFFF | Primary text, icons |
| `--color-celeste-arg` | #74C6F4 | Secondary accent — links, badges, info |
| `--color-gris-humo` | #A7A7A7 | Secondary text, muted content |

### Extended Palette

| Token | Hex | Role |
|-------|-----|------|
| `--color-negro-soft` | #1A1A1A | Card surfaces |
| `--color-negro-border` | #2C2C2C | Dividers, borders |
| `--color-negro-muted` | #3A3A3A | Input backgrounds |
| `--color-gris-dark` | #6B6B6B | Tertiary text |
| `--color-gris-light` | #D0D0D0 | Disabled text |
| `--badge-top` | #FF6B00 | TOP badge (fire orange) |

## Typography

### Fonts

- **Anton** (display): Bold, uppercase headlines. Impact replacement.
- **Space Grotesk** (UI): Modern grotesque for body, labels, navigation.
- **Loading**: Anton from local file; Space Grotesk from Google Fonts.

### Scale

| Name | Size | Weight | Leading | Tracking | Transform |
|------|------|--------|---------|----------|-----------|
| Hero | 80px | 400 | 1.1 | -0.02em | uppercase |
| Display XL | 60px | 400 | 1.1 | -0.01em | uppercase |
| Heading 1 | 32px | 700 | 1.25 | -0.01em | none |
| Heading 2 | 24px | 700 | 1.25 | 0.04em | uppercase |
| Body | 18px | 400 | 1.65 | 0em | none |
| Body Small | 14px | 400 | 1.5 | 0.01em | none |
| Label | 14px | 600 | 1 | 0.04em | uppercase |
| Badge | 10px | 600 | 1 | 0.1em | uppercase |

### Type Rules
- ALL CAPS display text always gets letter-spacing (0.04em+)
- Body text on dark: high contrast (#FFF on #0D0D0D)
- Line length max: 65ch for body text
- No uppercase for body paragraphs

## Spacing

- **Base unit**: 4px
- **Container max**: 1240px
- **Nav height**: 56px (fixed)
- **Grid**: 5-column responsive (→ 4 → 3 → 2)
- **Section padding**: 64px-96px
- **Card gap**: 10px

## Motion

| Duration | Use Case |
|----------|----------|
| 90ms | Hover, press, toggle |
| 120ms | Micro-interactions |
| 200ms | State changes, tabs |
| 240ms | Modals, accordions |
| 350ms | Large transitions |

- Easing: cubic-bezier(0.16, 1, 0.3, 1) for exits
- Easing: cubic-bezier(0.7, 0, 0.84, 0) for enters
- Reduced motion: all durations → 0ms via media query

## Components

### Navigation
- Fixed top bar, 56px height
- Logo: "OPEN" (white) + "MEME" (lime) — Anton font
- Links: Pill-shaped tabs with underline indicator
- Search: Pill-shaped input, search icon
- CTA: "SUBÍ TU MEME" lime button

### Meme Card
- Square thumbnail, border-radius 8px
- Hover: scale(1.03), overlay appears, action buttons slide in
- Badges: NUEVO (lime), TOP (orange), rank number
- Overlay actions: like, save, share — pill buttons

### Buttons
- **Primary**: Lime background, black text, uppercase, 12px tracking
- **Secondary**: Dark background, white border
- **Ghost**: Transparent, hover → subtle background
- **Shape**: Pill (border-radius: 999px)

### Badges
- **NUEVO**: Lime background, black text
- **TOP**: Orange background, white text
- **CLASICO**: Celeste blue background
- **Rank**: Colored circle (gold/silver/bronze/gray)

## Layout Patterns

### Home Page
```
[Nav] (fixed 56px)
[Hero: headline + search + filter pills + visual collage]
[Content: 5-col grid + sidebar (categories + Meme del Día)]
[Pagination]
```

### Meme Detail
```
[Nav]
[Full meme image + actions + metadata + tags + related memes]
```

### Upload
```
[Nav]
[Upload zone (drag-drop) + title + category + tags]
```

## Responsive Breakpoints

| Breakpoint | Grid Columns | Container Padding |
|------------|-------------|-------------------|
| > 1200px | 5 | 24px |
| 768-1200px | 4 | 20px |
| 480-768px | 3 | 16px |
| < 480px | 2 | 12px |

## Assets

- **Logo**: SVG, "OPENMEME" wordmark
- **Font**: Anton-Regular.ttf (bundled)
- **Icons**: System emoji or icon library
- **No external images** — all visuals are CSS/generated
