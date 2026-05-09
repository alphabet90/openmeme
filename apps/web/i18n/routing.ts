import { defineRouting } from "next-intl/routing";

export const locales = ["en", "es", "es-AR", "pt", "fr", "de", "ar"] as const;
export type Locale = (typeof locales)[number];
export const defaultLocale: Locale = "es-AR";

export const localeLangMap: Record<Locale, string> = {
  en: "en",
  es: "es",
  "es-AR": "es-AR",
  pt: "pt",
  fr: "fr",
  de: "de",
  ar: "ar",
};

export const localeOgMap: Record<Locale, string> = {
  en: "en_US",
  es: "es_ES",
  "es-AR": "es_AR",
  pt: "pt_BR",
  fr: "fr_FR",
  de: "de_DE",
  ar: "ar_SA",
};

export const rtlLocales: Locale[] = ["ar"];

export const routing = defineRouting({
  locales,
  defaultLocale,
  localePrefix: "always",
});
