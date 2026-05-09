import type { Category, CategoryIcon } from "@openmeme/ui";
import { fetchCategories, type ApiCategorySummary, type LocaleCode } from "@/lib/api";

const iconBySlug: Record<string, CategoryIcon> = {
  "la-vida": "globe",
  "politica": "tv",
  "futbol": "circle",
  "argentinos": "user",
  "argentina-reaction": "user",
  "clasicos": "star",
  "random": "refresh",
};

const defaultIcon: CategoryIcon = "star";

function humanize(slug: string): string {
  return slug
    .split(/[-_]/)
    .filter(Boolean)
    .map((w) => w.charAt(0).toUpperCase() + w.slice(1))
    .join(" ");
}

function toCategory(api: ApiCategorySummary, locale: LocaleCode = "en"): Category {
  const translation =
    api.translations?.find((t) => t.locale === locale) ??
    api.translations?.[0];
  return {
    slug: api.category,
    name: translation?.name ?? humanize(api.category),
    count: api.count,
    topScore: api.top_score,
    iconName: iconBySlug[api.category] ?? defaultIcon,
  };
}

export async function getCategories(locale: LocaleCode = "en"): Promise<Category[]> {
  const page = await fetchCategories({ limit: 100, locale });
  return page.data
    .map((c) => toCategory(c, locale))
    .sort((a, b) => b.count - a.count);
}

export async function getCategory(
  slug: string,
  locale: LocaleCode = "en",
): Promise<Category | undefined> {
  const list = await getCategories(locale);
  return list.find((c) => c.slug === slug);
}
