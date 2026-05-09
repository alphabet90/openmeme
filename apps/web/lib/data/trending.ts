import type { TrendingTag } from "@openmeme/ui";
import { fetchMemes, type LocaleCode } from "@/lib/api";

export async function getTrending(
  limit = 5,
  locale: LocaleCode = "en",
): Promise<TrendingTag[]> {
  const page = await fetchMemes({ limit: 60, sort: "score", locale });

  const totals = new Map<string, number>();
  for (const meme of page.data) {
    const score = Math.max(1, meme.score ?? 1);
    for (const raw of meme.tags ?? []) {
      const tag = raw.trim().toLowerCase();
      if (!tag || tag.length > 32) continue;
      totals.set(tag, (totals.get(tag) ?? 0) + score);
    }
  }

  const ranked = [...totals.entries()]
    .sort((a, b) => b[1] - a[1])
    .slice(0, limit);

  return ranked.map(([tag, count], i) => ({
    rank: i + 1,
    tag: `#${tag.replace(/^#/, "")}`,
    count,
  }));
}
