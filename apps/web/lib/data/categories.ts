import type { Category, CategoryIcon, CategoryImage as UiCategoryImage } from "@openmeme/ui";
import { fetchCategories, type ApiCategorySummary, type ApiCategoryImage, type LocaleCode } from "@/lib/api";

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

function mapCategoryImage(img: ApiCategoryImage): UiCategoryImage {
  return {
    path: img.path,
    width: img.width ?? undefined,
    height: img.height ?? undefined,
    bytes: img.bytes ?? undefined,
    mimeType: img.mime_type ?? undefined,
    imageType: img.image_type,
    position: img.position,
    isPrimary: img.is_primary,
  };
}

function getCategoryIcon(apiCategory: ApiCategorySummary): CategoryIcon {
  // Prefer icon from images if available
  const iconImage = apiCategory.images?.find((img) => img.image_type === "icon");
  if (iconImage) {
    // For icon images, we could use the path directly in UI
    // For now, return default but images will be rendered separately
    return defaultIcon;
  }
  return iconBySlug[apiCategory.category] ?? defaultIcon;
}

function toCategory(api: ApiCategorySummary, locale: LocaleCode): Category {
  const translation =
    api.translations?.find((t) => t.locale === locale) ??
    api.translations?.[0];
  return {
    slug: api.category,
    name: translation?.name ?? humanize(api.category),
    description: translation?.description ?? undefined,
    count: api.count,
    topScore: api.top_score,
    iconName: getCategoryIcon(api),
    images: api.images?.map(mapCategoryImage),
  };
}

export function getCategoryImage(
  category: Category,
  imageType: "icon" | "banner" | "thumbnail",
): UiCategoryImage | undefined {
  return category.images?.find((img) => img.imageType === imageType);
}

export async function getCategories(locale: LocaleCode): Promise<Category[]> {
  const page = await fetchCategories({ limit: 100, locale });
  return page.data
    .map((c) => toCategory(c, locale))
    .sort((a, b) => b.count - a.count);
}

export async function getCategory(
  slug: string,
  locale: LocaleCode,
): Promise<Category | undefined> {
  const list = await getCategories(locale);
  return list.find((c) => c.slug === slug);
}
