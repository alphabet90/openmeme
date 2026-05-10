---
name: openmeme-design-system
description: |
  OpenMeme Design System — AI-first design tokens and components for the OpenMeme monorepo.
  Provides: color tokens (lime #D4FF00, argentine blue #74C6F4, dark #0D0D0D), 
  typography (Anton display + Space Grotesk UI), spacing (4px base unit), 
  motion (Refero 80-350ms standards), shadows, border-radius, and pre-built 
  CSS component classes for nav, meme cards, buttons, badges, search, upload, 
  grids, pagination, avatars, sidebar, category pills, and empty states.
  
  Use when: creating OpenMeme web interfaces, building meme browsing UIs, 
  designing dark-theme meme platforms, or styling any component in the 
  OpenMeme ecosystem. Covers color, type, spacing, motion, and component 
  patterns.
---

# OpenMeme Design System

## Philosophy

OpenMeme's design system follows **Refero Research-First Design** principles:

1. **Dark-first**: `#0D0D0D` page background — memes are the star
2. **Lime accent**: `#D4FF00` primary — energetic, distinctive, accessible
3. **Argentine blue**: `#74C6F4` secondary — brand heritage
4. **Type contrast**: Anton (bold uppercase display) vs Space Grotesk (clean UI)
5. **4px grid**: All spacing based on a 4px unit system
6. **Motion minimal**: 80-350ms durations, no unnecessary animation

## Quick Start

```css
@import '@openmeme/design-system/tokens/index.css';
@import '@openmeme/design-system/src/components.css';
```

## Token Reference

### Colors
- **Background**: `--bg-primary` (#0D0D0D), `--bg-surface` (#1A1A1A), `--bg-elevated` (#2C2C2C)
- **Foreground**: `--fg-primary` (white), `--fg-secondary` (#A7A7A7), `--fg-muted` (#6B6B6B)
- **Accent**: `--accent-primary` (#D4FF00), `--accent-secondary` (#74C6F4)

### Typography
- **Display**: Anton, uppercase, -0.02em tracking, 1.1 leading
- **UI**: Space Grotesk, 400-700 weight, normal tracking
- **Scale**: 10px to 80px, ~1.25 ratio

### Spacing
- **Base unit**: 4px
- **Gaps**: 4px, 8px, 12px, 16px, 24px, 32px, 48px
- **Max container**: 1240px
- **Nav height**: 56px

### Motion
- **Instant**: 90ms (hover, press)
- **Base**: 200ms (state changes)
- **Medium**: 240ms (modals, accordions)
- **Slow**: 350ms (large transitions)
- **Easing**: cubic-bezier(0.16, 1, 0.3, 1) for exits

## Component Classes

| Class | Description |
|-------|-------------|
| `.logo` | Brand logo with .open (white) and .meme (lime) |
| `.nav` | Fixed top navigation bar |
| `.search-bar` | Pill-shaped search input |
| `.btn-primary` | Lime CTA button, uppercase, tracking |
| `.btn-secondary` | Outlined button on dark surface |
| `.meme-card` | Thumbnail card with hover scale + overlay |
| `.badge-nuevo` | Lime "NUEVO" badge |
| `.badge-top` | Orange "TOP" badge |
| `.category-pill` | Rounded category tag, border hover |
| `.filter-pill` | Tab pill with active lime state |
| `.avatar` | Circular user avatar |
| `.meme-grid` | 5-column responsive grid |
| `.pagination` | Page number controls |
| `.upload-zone` | Dashed drag-drop area |
| `.empty-state` | Centered no-content placeholder |
| `.sort-tabs` | Horizontal tab switcher |

## Files

```
tokens/
  index.css        # Import all tokens
colors.css         # Brand palette + semantic aliases
typography.css     # Font families + scale + styles
spacing.css        # 4px grid + semantic gaps
shadows.css        # Shadows + glows + elevation
motion.css         # Durations + easings + reduced-motion
radius.css         # Border radius scale
src/components.css # All component classes
fonts/
  Anton-Regular.ttf
design-refero.md   # Refero methodology documentation
```
