# Spec: Add meme upload page (Coming Soon)

| Field | Value |
|-------|-------|
| **Issue** | [alphabet90/openmeme#57](https://github.com/alphabet90/openmeme/issues/57) |
| **Branch** | `looper/planner/57-add-meme-upload-page` |
| **Base** | `main` |
| **Date** | 2026-05-27 |
| **Estimate** | XS (1 dev, ~1/2 day) |

---

## Problem

The platform has no page for users to upload their own memes. The Nav, Footer, and Sidebar all already link to `/subir` but the route returns a 404. This creates a broken user experience and provides no visibility into whether an upload feature is planned.

## Goals

1. Create a new Next.js App Router page at `apps/web/app/[locale]/subir/` that renders a "Coming Soon" message.
2. Use existing `@openmeme/design-system` tokens and `@openmeme/ui` components for styling.
3. Integrate with the existing i18n system — at minimum `en` and `es-AR` (the two primary locales).
4. Follow the established page component pattern (Server Component, explicit `<Nav>`/`<Footer>`, CSS Module).
5. No actual upload functionality — this is a placeholder that sets expectations.

## Approach

### 1. Create the route directory and page component

New directory: `apps/web/app/[locale]/subir/`

**`page.tsx`** — Server component following the pattern established by `categorias/page.tsx`:

```tsx
export const revalidate = 3600;

export async function generateMetadata({ params }: Props): Promise<Metadata> {
  const { locale } = await params;
  const t = await getTranslations({ locale, namespace: "subir" });
  return {
    title: t("meta_title"),
    description: t("meta_description"),
    alternates: {
      canonical: `/${locale}/subir`,
      languages: buildAlternates("/subir"),
    },
    openGraph: {
      title: t("og_title"),
      description: t("og_description"),
      url: `/${locale}/subir`,
      locale: localeOgMap[locale],
    },
  };
}

export default async function SubirPage({ params }: Props) {
  const { locale } = await params;
  const t = await getTranslations({ locale, namespace: "subir" });

  return (
    <>
      <Nav />
      <div className={styles.page}>
        <main id="contenido" className={styles.main}>
          <div className="container">
            {/* Coming Soon content */}
            <h1>{t("title")}</h1>
            <p>{t("description")}</p>
            {/* Optional: notification signup */}
          </div>
        </main>
      </div>
      <Footer />
    </>
  );
}
```

**`page.module.css`** — minimal styles:

```css
.page {
  padding-top: var(--nav-height);
  min-height: 100vh;
}

.main {
  padding: var(--space-12) 0 var(--space-16);
}
```

### 2. Add i18n keys

Add a `subir` namespace to all 7 locale files:

| Key | en | es-AR |
|-----|----|-------|
| `meta_title` | "Upload a Meme — OpenMeme" | "Subir un meme — OpenMeme" |
| `meta_description` | "Share your memes with the world. The upload feature is coming soon to OpenMeme." | "Compartí tus memes con el mundo. La función de subida llega pronto a OpenMeme." |
| `og_title` | "Upload a Meme — OpenMeme" | "Subir un meme — OpenMeme" |
| `og_description` | "Share your memes with the world. Coming soon to OpenMeme." | "Compartí tus memes con el mundo. Próximamente en OpenMeme." |
| `title` | "Upload a Meme" | "Subir un meme" |
| `description` | "The ability to upload your own memes is coming soon. Stay tuned!" | "La posibilidad de subir tus propios memes estará disponible pronto. ¡Mantenete atento!" |
| `eyebrow` | "Coming Soon" | "Próximamente" |
| `notify_heading` | "Want to be notified?" | "¿Querés recibir notificaciones?" |
| `notify_text` | "Leave your email and we'll let you know when uploads go live." | "Dejanos tu email y te avisaremos cuando las subidas estén disponibles." |
| `notify_placeholder` | "your@email.com" | "tu@email.com" |
| `notify_button` | "Notify Me" | "Notificarme" |
| `notify_success` | "You're on the list!" | "¡Estás en la lista!" |

Add the `subir` namespace to all 7 locale files. Provide translations for `es`, `pt`, `fr`, `de`, and `ar` following the established spec pattern:

**es:**
- `meta_title`: "Subir un meme — OpenMeme"
- `meta_description`: "Comparte tus memes con el mundo. La función de subida llegará pronto a OpenMeme."
- `og_title`: "Subir un meme — OpenMeme"
- `og_description`: "Comparte tus memes con el mundo. Próximamente en OpenMeme."
- `title`: "Subir un meme"
- `description`: "La posibilidad de subir tus propios memes estará disponible pronto. ¡Mantente atento!"
- `eyebrow`: "Próximamente"
- `notify_heading`: "¿Quieres recibir notificaciones?"
- `notify_text`: "Déjanos tu correo y te avisaremos cuando las subidas estén disponibles."
- `notify_placeholder`: "tu@email.com"
- `notify_button`: "Notificarme"
- `notify_success`: "¡Estás en la lista!"

**pt:**
- `meta_title`: "Enviar um Meme — OpenMeme"
- `meta_description`: "Compartilhe seus memes com o mundo. O recurso de envio estará disponível em breve no OpenMeme."
- `og_title`: "Enviar um Meme — OpenMeme"
- `og_description`: "Compartilhe seus memes com o mundo. Em breve no OpenMeme."
- `title`: "Enviar um Meme"
- `description`: "A possibilidade de enviar seus próprios memes estará disponível em breve. Fique ligado!"
- `eyebrow`: "Em Breve"
- `notify_heading`: "Quer ser notificado?"
- `notify_text`: "Deixe seu e-mail e avisaremos quando o envio estiver disponível."
- `notify_placeholder`: "seu@email.com"
- `notify_button`: "Notificar-me"
- `notify_success`: "Você está na lista!"

**fr:**
- `meta_title`: "Partager un mème — OpenMeme"
- `meta_description`: "Partagez vos mèmes avec le monde. La fonction de partage arrive bientôt sur OpenMeme."
- `og_title`: "Partager un mème — OpenMeme"
- `og_description`: "Partagez vos mèmes avec le monde. Bientôt sur OpenMeme."
- `title`: "Partager un mème"
- `description`: "La possibilité de partager vos propres mèmes sera bientôt disponible. Restez à l'écoute !"
- `eyebrow`: "Bientôt"
- `notify_heading`: "Vous voulez être informé ?"
- `notify_text`: "Laissez votre e-mail et nous vous préviendrons quand le partage sera disponible."
- `notify_placeholder`: "votre@email.com"
- `notify_button`: "M'informer"
- `notify_success`: "Vous êtes sur la liste !"

**de:**
- `meta_title`: "Meme hochladen — OpenMeme"
- `meta_description`: "Teilen Sie Ihre Memes mit der Welt. Die Upload-Funktion kommt bald zu OpenMeme."
- `og_title`: "Meme hochladen — OpenMeme"
- `og_description`: "Teilen Sie Ihre Memes mit der Welt. Bald auf OpenMeme."
- `title`: "Meme hochladen"
- `description`: "Die Möglichkeit, eigene Memes hochzuladen, ist bald verfügbar. Bleiben Sie dran!"
- `eyebrow`: "Demnächst"
- `notify_heading`: "Möchten Sie benachrichtigt werden?"
- `notify_text`: "Hinterlassen Sie Ihre E-Mail und wir informieren Sie, wenn Uploads verfügbar sind."
- `notify_placeholder`: "ihre@email.com"
- `notify_button`: "Benachrichtigen"
- `notify_success`: "Sie sind auf der Liste!"

**ar:**
- `meta_title`: "رفع ميم — OpenMeme"
- `meta_description`: "شارك ميماتك مع العالم. ميزة الرفع قادمة قريبًا إلى OpenMeme."
- `og_title`: "رفع ميم — OpenMeme"
- `og_description`: "شارك ميماتك مع العالم. قريبًا على OpenMeme."
- `title`: "رفع ميم"
- `description`: "إمكانية رفع ميماتك الخاصة ستكون متاحة قريبًا. ترقبوا!"
- `eyebrow`: "قريبًا"
- `notify_heading`: "هل تريد الإشعار؟"
- `notify_text`: "اترك بريدك الإلكتروني وسنخبرك عندما يصبح الرفع متاحًا."
- `notify_placeholder`: "بريدك@email.com"
- `notify_button`: "إشعاري"
- `notify_success`: "أنت على القائمة!"

### 3. No navigation changes needed

All three navigation surfaces already link to `localePath(locale, "/subir")`:
- `components/nav/Nav.tsx:55-58` — Nav CTA button with `UploadIcon`
- `components/Footer.tsx:18` — Footer link
- `components/sidebar/UploadWidget.tsx:18` — Sidebar widget

The route just needs to exist for these links to stop 404ing.

## Risks & Mitigations

| Risk | Mitigation |
|------|------------|
| Users expect a working upload form | The "Coming Soon" eyebrow and description text set clear expectations. No form fields are rendered. |
| Missing i18n keys cause runtime errors | All existing locale files follow the same JSON structure; keys are added to all 7 files in the same PR. |
| Route conflicts with future upload implementation | The route path (`/subir`) is the intended final path. The future implementation replaces the content of this page — no redirect needed. |
| SEO impact of a placeholder page | `generateMetadata` returns descriptive `title`, `description`, and `openGraph` fields. `revalidate = 3600` keeps ISR fresh. The alternates/hreflang links are set correctly. |
| Missing OG keys for social previews | `og_title` and `og_description` are included in the i18n namespace and `generateMetadata` returns them as `openGraph` metadata, ensuring rich link previews on Discord, Telegram, and Twitter. |

## Validation

1. **Route existence:**
   - Navigate to `/es-AR/subir` and `/en/subir`.
   - Confirm the page renders without a 404.
2. **Navigation integration:**
   - Click "Subir" / "Upload" in the Nav CTA, Footer, and Sidebar.
   - Confirm each link navigates to the correct locale-prefixed `/subir` route.
3. **i18n:**
   - Switch locale between `en` and `es-AR`.
   - Confirm all visible text changes accordingly.
4. **Visual consistency:**
   - Confirm the page uses the same `.page`/`.main` padding and `--nav-height` offset as other pages.
   - Confirm `<Nav>` and `<Footer>` render correctly.
5. **Build checks:**
   - `pnpm lint` in `apps/web` passes with no new errors.
   - `pnpm build` in `apps/web` succeeds.

## Affected Files

- `apps/web/app/[locale]/subir/page.tsx` *(new)*
- `apps/web/app/[locale]/subir/page.module.css` *(new)*
- `apps/web/messages/en.json`
- `apps/web/messages/es.json`
- `apps/web/messages/es-AR.json`
- `apps/web/messages/pt.json`
- `apps/web/messages/fr.json`
- `apps/web/messages/de.json`
- `apps/web/messages/ar.json`

## Definition of Done

- [ ] `/subir` route exists and renders a "Coming Soon" page.
- [ ] Page includes `<Nav>`, `<Footer>`, and follows the established CSS Module pattern.
- [ ] `generateMetadata` returns correct title, description, `openGraph` (with `og_title`/`og_description`), and alternate language links.
- [ ] i18n keys added to all 7 locale JSON files under the `subir` namespace.
- [ ] Nav CTA, Footer, and Sidebar links to `/subir` no longer 404.
- [ ] No new lint / TypeScript errors.
- [ ] `pnpm build` in `apps/web` succeeds.
