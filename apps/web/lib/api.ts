import { site } from "@/lib/site";

export type LocaleCode = "en" | "es" | "es-AR" | "pt" | "fr" | "de" | "ar";
export type SortKey = "score" | "created_at" | "title";

export interface ApiMemeTranslation {
  locale: string;
  title: string;
  description: string | null;
}

export interface ApiMemeImage {
  path: string;
  width?: number | null;
  height?: number | null;
  bytes?: number | null;
  mime_type?: string | null;
  position: number;
  is_primary: boolean;
}

export interface ApiMeme {
  slug: string;
  category: string;
  default_locale?: string;
  author: string | null;
  subreddit: string | null;
  score: number | null;
  created_at: string | null;
  source_url: string | null;
  post_url: string | null;
  translations: ApiMemeTranslation[];
  images: ApiMemeImage[];
  tags: string[] | null;
}

export interface ApiMemePage {
  data: ApiMeme[];
  page: number;
  limit: number;
  total: number;
  total_pages: number;
}

export interface ApiSearchResult {
  slug: string;
  category: string;
  author: string | null;
  score: number;
  title: string;
  description: string | null;
  image_path: string | null;
  tags: string[];
}

export interface ApiCategoryTranslation {
  locale: string;
  name: string;
  description?: string | null;
}

export interface ApiCategorySummary {
  category: string;
  count: number;
  top_score: number;
  translations?: ApiCategoryTranslation[];
}

export interface ApiCategoryPage {
  data: ApiCategorySummary[];
  page: number;
  limit: number;
  total: number;
  total_pages: number;
}

function toApiLocale(locale: LocaleCode): string {
  return locale.toLowerCase();
}

export function cdnUrl(path: string | undefined | null): string | null {
  if (!path) return null;
  return `${site.cdnBaseUrl}/${path.replace(/^\//, "")}`;
}

export async function fetchMemes(params: {
  page?: number;
  limit?: number;
  sort?: SortKey;
  locale?: LocaleCode;
  category?: string;
} = {}): Promise<ApiMemePage> {
  const url = new URL(`${site.apiBaseUrl}/memes`);
  if (params.page != null) url.searchParams.set("page", String(params.page));
  if (params.limit != null) url.searchParams.set("limit", String(params.limit));
  if (params.sort) url.searchParams.set("sort", params.sort);
  if (params.locale) url.searchParams.set("locale", toApiLocale(params.locale));
  if (params.category) url.searchParams.set("category", params.category);

  const res = await fetch(url.toString(), { next: { revalidate: 300 } });
  if (!res.ok) throw new Error(`fetchMemes failed: ${res.status}`);
  return res.json() as Promise<ApiMemePage>;
}

export async function fetchMeme(
  category: string,
  slug: string,
  locale: LocaleCode = "en",
): Promise<ApiMeme | null> {
  const url = new URL(
    `${site.apiBaseUrl}/memes/${encodeURIComponent(category)}/${encodeURIComponent(slug)}`,
  );
  url.searchParams.set("locale", toApiLocale(locale));

  const res = await fetch(url.toString(), { next: { revalidate: 3600 } });
  if (res.status === 404) return null;
  if (!res.ok) throw new Error(`fetchMeme failed: ${res.status}`);
  return res.json() as Promise<ApiMeme>;
}

export async function fetchMemesByCategory(
  category: string,
  params: { page?: number; limit?: number; sort?: SortKey; locale?: LocaleCode } = {},
): Promise<ApiMemePage | null> {
  const url = new URL(`${site.apiBaseUrl}/memes/${encodeURIComponent(category)}`);
  if (params.page != null) url.searchParams.set("page", String(params.page));
  if (params.limit != null) url.searchParams.set("limit", String(params.limit));
  if (params.sort) url.searchParams.set("sort", params.sort);
  if (params.locale) url.searchParams.set("locale", toApiLocale(params.locale));

  const res = await fetch(url.toString(), { next: { revalidate: 600 } });
  if (res.status === 404) return null;
  if (!res.ok) throw new Error(`fetchMemesByCategory failed: ${res.status}`);
  return res.json() as Promise<ApiMemePage>;
}

export async function fetchCategories(params: {
  limit?: number;
  locale?: LocaleCode;
  page?: number;
} = {}): Promise<ApiCategoryPage> {
  const url = new URL(`${site.apiBaseUrl}/categories`);
  if (params.page != null) url.searchParams.set("page", String(params.page));
  if (params.limit != null) url.searchParams.set("limit", String(params.limit));
  if (params.locale) url.searchParams.set("locale", toApiLocale(params.locale));

  const res = await fetch(url.toString(), { next: { revalidate: 1800 } });
  if (!res.ok) throw new Error(`fetchCategories failed: ${res.status}`);
  return res.json() as Promise<ApiCategoryPage>;
}

export async function searchMemes(params: {
  q: string;
  page?: number;
  limit?: number;
  locale?: LocaleCode;
}): Promise<ApiSearchResult[]> {
  if (!params.q.trim()) return [];

  const url = new URL(`${site.apiBaseUrl}/search`);
  url.searchParams.set("q", params.q);
  if (params.page != null) url.searchParams.set("page", String(params.page));
  if (params.limit != null) url.searchParams.set("limit", String(params.limit));
  if (params.locale) url.searchParams.set("locale", toApiLocale(params.locale));

  const res = await fetch(url.toString(), { next: { revalidate: 60 } });
  if (!res.ok) throw new Error(`searchMemes failed: ${res.status}`);
  return res.json() as Promise<ApiSearchResult[]>;
}
