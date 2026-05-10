import { defaultLocale, locales, localeLangMap, type Locale } from "@/i18n/routing";
import { site } from "@/lib/site";

export function localePath(locale: Locale, path: string): string {
  const normalized = path.startsWith("/") ? path : `/${path}`;
  return `/${locale}${normalized}`;
}

export function localeUrl(locale: Locale, path: string): string {
  return `${site.url}${localePath(locale, path)}`;
}

export function buildAlternates(path: string): Record<string, string> {
  const normalized = path.startsWith("/") ? path : `/${path}`;
  const languages: Record<string, string> = { "x-default": `/${defaultLocale}${normalized}` };
  for (const l of locales) {
    languages[localeLangMap[l]] = `/${l}${normalized}`;
  }
  return languages;
}
