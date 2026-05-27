# Spec: Add missing legal and support pages

| Field | Value |
|-------|-------|
| **Issue** | [alphabet90/openmeme#56](https://github.com/alphabet90/openmeme/issues/56) |
| **Branch** | `looper/planner/56-add-missing-legal-and` |
| **Base** | `main` |
| **Date** | 2026-05-27 |
| **Estimate** | M (1 dev, ~2–3 days) |

---

## Problem

The OpenMeme web client (`apps/web`) is missing four essential pages: Terms and Conditions, Privacy Policy, DMCA/Copyright policy, and a Contact page. The footer already renders links to these routes (`/terminos`, `/privacidad`, `/dmca`, `/contacto`) with translated labels, but each link leads to a 404. This gaps compliance, erodes user trust, and leaves the platform without a clear legal framework or support channel.

## Goals

1. Serve four static (SSG) legal/support pages under `apps/web/app/[locale]/` that match the existing site layout, SEO metadata, and i18n patterns.
2. Each page must carry substantive legal or support content, not placeholder text.
3. Add i18n translation namespaces for page content to all 7 locale files.
4. Reachable from the footer — no additional navigation wiring needed (footer already points to the correct routes).
5. Include these new pages in the sitemap.

## Approach

### 1. Page structure and conventions

Create four route groups under `apps/web/app/[locale]/`:

```
apps/web/app/[locale]/
├── terminos/
│   ├── page.module.css
│   └── page.tsx
├── privacidad/
│   ├── page.module.css
│   └── page.tsx
├── dmca/
│   ├── page.module.css
│   └── page.tsx
└── contacto/
    ├── page.module.css
    └── page.tsx
```

Each page follows the established pattern from `categorias/page.tsx`:
- `export const revalidate = 86400` (ISR, revalidates daily since legal text changes infrequently)
- `generateMetadata` with translated title/description, alternates, OpenGraph
- Default export renders `<Nav />`, `<main id="contenido">` with content, `<Footer />`

**Content approach:** Hardcode the legal text directly in each page component using `getTranslations` for the relevant namespace. This avoids the complexity of MDX or a CMS for content that changes at most a few times per year.

### 2. Terms and Conditions (`/terminos`)

**Route:** `apps/web/app/[locale]/terminos/page.tsx`

**i18n namespace:** `"terminos"`

**Content sections (heading + body, all translatable):**
- Title / meta title
- Last updated date
- Acceptance of terms
- User conduct and prohibited uses
- Content ownership (user retains ownership of uploaded memes; OpenMeme gets a license to display)
- DMCA / copyright notice (cross-link to `/dmca`)
- Limitation of liability
- Governing law (Argentina)
- Contact information (cross-link to `/contacto`)

### 3. Privacy Policy (`/privacidad`)

**Route:** `apps/web/app/[locale]/privacidad/page.tsx`

**i18n namespace:** `"privacidad"`

**Content sections (all translatable):**
- Title / meta title
- Last updated date
- Information collected (user-provided, automatically collected via cookies/analytics)
- Cookies and tracking (PostHog, next.intl, etc.)
- Third-party services (CDN, hosting, analytics)
- Data sharing and disclosure
- User rights (access, correction, deletion, data portability)
- Data retention
- Security measures
- Contact information (cross-link to `/contacto`)

### 4. DMCA / Copyright Policy (`/dmca`)

**Route:** `apps/web/app/[locale]/dmca/page.tsx`

**i18n namespace:** `"dmca"`

**Content sections (all translatable):**
- Title / meta title
- Designated agent contact: **support@openmeme.io**
- Infringement notification procedure (required elements per DMCA 512(c))
- Counter-notice procedure
- Repeat infringer policy
- Cross-link to `/contacto` and `/terminos`

### 5. Contact Page (`/contacto`)

**Route:** `apps/web/app/[locale]/contacto/page.tsx`

**i18n namespace:** `"contacto"`

**Content:**
- Title / meta title
- Direct email link: `mailto:support@openmeme.io`
- Response time expectations
- For legal/DMCA inquiries, refer to `/dmca`

**No contact form** (simple email link is sufficient for MVP; form can be added later).

### 6. i18n messages

Add a new top-level namespace to all 7 locale files (`en.json`, `es.json`, `es-AR.json`, `pt.json`, `fr.json`, `de.json`, `ar.json`):

| Namespace | Description |
|-----------|-------------|
| `terminos` | Terms of Service page content |
| `privacidad` | Privacy Policy page content |
| `dmca` | DMCA / Copyright page content |
| `contacto` | Contact page content |

Each namespace contains strings for the page title, meta description, section headings, and body text (structured as sub-keys for each content block).

**English content will be the authoritive source; other locales followed existing translation style.**

### 7. Sitemap

Update `apps/web/app/[locale]/sitemap.ts` to include the four new static entries:

```ts
const legalEntries: MetadataRoute.Sitemap = [
  { url: `${base}/terminos`,   lastModified: now, changeFrequency: "monthly", priority: 0.3 },
  { url: `${base}/privacidad`, lastModified: now, changeFrequency: "monthly", priority: 0.3 },
  { url: `${base}/dmca`,       lastModified: now, changeFrequency: "monthly", priority: 0.3 },
  { url: `${base}/contacto`,   lastModified: now, changeFrequency: "monthly", priority: 0.4 },
];
```

### 8. Shared CSS module

All four pages will share a single `legal.module.css` (or inline similar minimal styles) for the prose content layout: max-width container, vertical rhythm, heading hierarchy. Alternatively, each page can have its own small module importing from a shared partial — the spec recommends a shared `apps/web/app/[locale]/legal.module.css` imported by each legal page.

## Risks & Mitigations

| Risk | Mitigation |
|------|------------|
| Legal text could become outdated | Revalidate set to 86400s (daily); update cycle can be shortened via ISR revalidation. Hardcoded content makes updates require a deployment — acceptable for infrequent legal changes. |
| Footer routes may not match Spanish route names for legal pages | Footer already uses `/terminos`, `/privacidad`, `/dmca`, `/contacto` — routes are consistent with existing footer links. |
| Large i18n namespaces increase message file size | Legal text is static, short-medium length (~500 words per page). The impact on JSON file size is negligible. |
| RTL support for Arabic | Pages use standard layout with `dir` from locale layout; no special handling needed beyond providing Arabic translations. |

## Validation

1. **Route accessibility:**
   - Navigate to `/en/terms`, `/en/privacy`, `/en/dmca`, `/en/contact` (and Spanish equivalents) — each renders without 404.
2. **Layout consistency:**
   - Each page includes `<Nav />` and `<Footer />`, matches the visual style of other content pages.
3. **i18n:**
   - Switch locale to `es-AR` — all content renders in Spanish.
   - Check `ar` locale renders RTL correctly.
4. **Footer links:**
   - All four legal links in the footer navigate to the correct pages.
5. **SEO:**
   - Each page has a unique `<title>` and `<meta name="description">`.
   - Sitemap includes the new entries.
6. **Build:**
   - `pnpm lint` in `apps/web` passes.
   - `pnpm build` in `apps/web` succeeds.

## Affected Files

- `apps/web/app/[locale]/terminos/page.tsx` *(new)*
- `apps/web/app/[locale]/terminos/page.module.css` *(new)*
- `apps/web/app/[locale]/privacidad/page.tsx` *(new)*
- `apps/web/app/[locale]/privacidad/page.module.css` *(new)*
- `apps/web/app/[locale]/dmca/page.tsx` *(new)*
- `apps/web/app/[locale]/dmca/page.module.css` *(new)*
- `apps/web/app/[locale]/contacto/page.tsx` *(new)*
- `apps/web/app/[locale]/contacto/page.module.css` *(new)*
- `apps/web/app/[locale]/sitemap.ts`
- `apps/web/messages/en.json`
- `apps/web/messages/es.json`
- `apps/web/messages/es-AR.json`
- `apps/web/messages/pt.json`
- `apps/web/messages/fr.json`
- `apps/web/messages/de.json`
- `apps/web/messages/ar.json`

## Definition of Done

- [ ] Four new routes render at `/terminos`, `/privacidad`, `/dmca`, `/contacto`.
- [ ] Each page is fully translated in all 7 locales.
- [ ] Page content is substantive legal/support text, not placeholder.
- [ ] Sitemap includes the four new pages.
- [ ] Footer links resolve to the correct pages (no 404s).
- [ ] No new lint / TypeScript errors.
- [ ] Build succeeds with `pnpm build` in `apps/web`.
