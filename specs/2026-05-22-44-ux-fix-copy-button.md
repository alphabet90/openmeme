# Spec: Fix Copy button in meme detail to copy image blob to clipboard

| Field | Value |
|-------|-------|
| **Issue** | [alphabet90/openmeme#44](https://github.com/alphabet90/openmeme/issues/44) |
| **Branch** | `looper/planner/44-ux-fix-copy-button` |
| **Base** | `main` |
| **Date** | 2026-05-22 |
| **Estimate** | S (1 dev, ~1/2 day) |

---

## Problem

On the meme detail page (`/memes/[category]/[slug]`) both copy buttons currently write the **image URL as plain text** (`text/plain`) to the clipboard via `navigator.clipboard.writeText()`. This is inconsistent with the browser's native "Copy Image" context-menu action, which copies the **actual image bitmap** (`image/png` blob). Users expect to paste the image directly into chat apps (WhatsApp, Telegram, Discord, Slack) or image editors, but instead they paste a raw URL string.

## Goals

1. Both copy buttons must copy the **image file blob** to the system clipboard, matching native browser "Copy Image" behavior.
2. Pasting into any image-aware target must insert the image bitmap.
3. Graceful degradation when the Clipboard API is unavailable, permission is denied, or CORS blocks blob access.
4. Preserve existing UI states (2-second "Copied" success label, aria-labels, focus states, keyboard operability).
5. No regression in download button behavior or SSR/hydration.

## Approach

### 1. Reusable Hook: `useCopyImage`

Create a new hook `apps/web/components/meme-detail/useCopyImage.ts` to centralize the copy logic and avoid duplication between `CopyButton.tsx` and `MemeActions.tsx`.

**Interface:**
```ts
export type CopyState = 'idle' | 'copied' | 'error';

export interface UseCopyImageResult {
  state: CopyState;
  copy: () => Promise<void>;
}

export function useCopyImage(imageUrl: string): UseCopyImageResult;
```

**Algorithm:**
1. Try `fetch(imageUrl, { mode: 'cors' })`.
   - If successful, read the response as `Blob`.
   - Ensure the blob MIME type is `image/png` (Safari requirement). If the source is JPEG/WebP, convert via a temporary canvas.
   - Write to clipboard: `navigator.clipboard.write([new ClipboardItem({ 'image/png': blob })])`.
2. If `fetch` fails due to CORS:
   - Load an `<img crossOrigin="anonymous">` pointing to `imageUrl`.
   - Draw the image onto a temporary `<canvas>`.
   - Export the canvas as a PNG blob via `canvas.toBlob(..., 'image/png')`.
   - Write the PNG blob to the clipboard.
3. If the canvas approach also fails (e.g., CORS blocks `crossOrigin`):
   - Fall back to `navigator.clipboard.writeText(imageUrl)`.
   - Transition state to `'copied'` (the URL was copied) — do **not** show an error, because the user still has something useful on the clipboard.
4. If `navigator.clipboard` is entirely unavailable:
   - Transition state to `'error'`.
5. On successful image blob copy, transition state to `'copied'` for 2 seconds, then back to `'idle'`.
6. On any unhandled failure, transition state to `'error'` for 2 seconds, then back to `'idle'`.

**Implementation notes:**
- The hook must run in a client-side effect or event handler (clipboard API is not available during SSR).
- Use a `useRef` to avoid state updates after unmount.
- Safari only accepts `image/png` in `ClipboardItem`; always normalize to PNG.

### 2. Update `CopyButton.tsx`

- Replace direct `navigator.clipboard.writeText` call with `useCopyImage(meme.imageUrl)`.
- Map the hook's `state` to the existing UI:
  - `'idle'` → show "Copy" label / icon.
  - `'copied'` → show "Copied" label / checkmark (existing 2-second timeout handled by the hook).
  - `'error'` → show a brief error indicator (e.g., red icon or tooltip). If the design system does not have an error icon, reuse the copy icon with a red tint or simply keep the button in its idle state (the fallback to URL copy makes a visible error rare).
- Keep existing `aria-label`, focus states, and keyboard operability.

### 3. Update `MemeActions.tsx`

- Same changes as `CopyButton.tsx`: consume `useCopyImage`.
- Map the hook's `state` to the existing UI, including the `'error'` state:
  - `'idle'` → show `copyLabel`.
  - `'copied'` → show `copiedLabel`.
  - `'error'` → show `copyErrorLabel`.
- Add a `copyErrorLabel` prop to `MemeActions`'s Props interface and pass `t("copy_error")` from `page.tsx`.
- Ensure the download button is untouched.

### 4. i18n

- Existing keys (`copiar`, `copiado`, `descargar`) remain valid.
- Add a new key `copy_error` to all `apps/web/messages/*.json` files for the error state label.
  - `en`: `"Could not copy image"`
  - `es`: `"No se pudo copiar la imagen"`
  - `es-AR`: `"No se pudo copiar la imagen"`
  - `pt`: `"Não foi possível copiar a imagem"`
  - `fr`: `"Impossible de copier l'image"`
  - `de`: `"Bild konnte nicht kopiert werden"`
  - `ar`: `"تعذر نسخ الصورة"`

### 5. Update `page.tsx`

Pass the new `copyErrorLabel` prop to `MemeActions` by translating the `copy_error` key:

```tsx
<MemeActions
  imageUrl={meme.imageUrl}
  downloadLabel={t("descargar")}
  copyLabel={t("copiar")}
  copiedLabel={t("copiado")}
  copyErrorLabel={t("copy_error")}
/>
```

## Risks & Mitigations

| Risk | Mitigation |
|------|------------|
| CORS on CDN image URLs | Try `fetch(..., { mode: 'cors' })` first; on failure use the canvas-draw technique with `crossOrigin="anonymous"`. If both fail, fall back to URL text copy. |
| Large image files | Canvas export can be memory-intensive. Do not resize/cap unless profiling shows a problem; rely on the original blob when possible. |
| Safari ClipboardItem quirks | Safari only accepts `image/png`. Convert JPEG/WebP to PNG blob via canvas if `blob.type !== 'image/png'`. |
| HTTPS requirement | Async Clipboard API requires a secure context. Production is HTTPS; dev uses `localhost` (allowed). |
| SSR / hydration | Clipboard logic lives inside a hook triggered by user interaction; no clipboard access during render. |

## Validation

1. **Manual browser verification:**
   - Open a meme detail page in Chrome, Firefox, and Safari.
   - Click both copy buttons.
   - Paste into WhatsApp Web, Telegram, Discord, Slack, Gmail compose, and Figma.
   - Confirm the image bitmap is pasted, not a URL string.
2. **Fallback verification:**
   - Block the image domain via dev-tools network blocking or use a non-CORS CDN.
   - Click copy; confirm the URL is pasted as text and the UI still shows a success state.
3. **Regression checks:**
   - Download button still triggers a file download.
   - `pnpm lint` in `apps/web` passes with no new errors.
   - `pnpm build` in `apps/web` succeeds.
   - Existing translation keys still resolve correctly.

## Affected Files

- `apps/web/components/meme-detail/useCopyImage.ts` *(new)*
- `apps/web/components/meme-detail/CopyButton.tsx`
- `apps/web/components/meme-detail/MemeActions.tsx`
- `apps/web/messages/en.json`
- `apps/web/messages/es.json`
- `apps/web/messages/es-AR.json`
- `apps/web/messages/pt.json`
- `apps/web/messages/fr.json`
- `apps/web/messages/de.json`
- `apps/web/messages/ar.json`

## Definition of Done

- [ ] `useCopyImage` hook is implemented and typed.
- [ ] Both `CopyButton.tsx` and `MemeActions.tsx` use the hook.
- [ ] Image blob is copied to clipboard on supported browsers.
- [ ] Graceful fallback to URL-as-text when image copy is impossible.
- [ ] Existing UI states (copied feedback, aria-labels) are preserved.
- [ ] New `copy_error` i18n key added to all locale files.
- [ ] No new lint / TypeScript errors.
- [ ] Verified manually in Chrome, Firefox, and Safari (or latest 2 browsers).
