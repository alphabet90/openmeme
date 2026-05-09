/**
 * openmeme stats - Show repository statistics
 */

import { existsSync, readdirSync, statSync, readFileSync } from "fs";
import { join, extname } from "path";
import chalk from "chalk";

interface StatsOptions {
  path?: string;
  json?: boolean;
}

interface RepoStats {
  totalImages: number;
  totalSize: number;
  categories: Record<string, { count: number; size: number }>;
  formats: Record<string, number>;
  topAuthors: Array<{ author: string; count: number }>;
  topTags: Array<{ tag: string; count: number }>;
  newestFile?: string;
  oldestFile?: string;
}

export async function statsCommand(options: StatsOptions): Promise<void> {
  const memesPath = join(process.cwd(), options.path || "memes");
  if (!existsSync(memesPath)) {
    console.log(chalk.yellow("No memes directory found."));
    return;
  }

  const stats: RepoStats = {
    totalImages: 0,
    totalSize: 0,
    categories: {},
    formats: {},
    topAuthors: [],
    topTags: [],
  };

  const authorCounts: Record<string, number> = {};
  const tagCounts: Record<string, number> = {};
  let newestTime = 0;
  let oldestTime = Infinity;
  let newestFile = "";
  let oldestFile = "";

  function scanDir(dir: string) {
    for (const entry of readdirSync(dir, { withFileTypes: true })) {
      const fullPath = join(dir, entry.name);
      if (entry.isDirectory()) {
        scanDir(fullPath);
      } else if (entry.isFile()) {
        const ext = extname(entry.name).toLowerCase();
        if ([".jpg", ".jpeg", ".png", ".gif", ".webp"].includes(ext)) {
          const size = statSync(fullPath).size;
          const stat = statSync(fullPath);
          stats.totalImages++;
          stats.totalSize += size;

          const format = ext.replace(".", "");
          stats.formats[format] = (stats.formats[format] || 0) + 1;

          // Category from parent dir
          const category = dir.split("/").pop() || "unknown";
          if (!stats.categories[category]) {
            stats.categories[category] = { count: 0, size: 0 };
          }
          stats.categories[category].count++;
          stats.categories[category].size += size;

          // File age
          if (stat.mtimeMs > newestTime) {
            newestTime = stat.mtimeMs;
            newestFile = entry.name;
          }
          if (stat.mtimeMs < oldestTime) {
            oldestTime = stat.mtimeMs;
            oldestFile = entry.name;
          }

          // Parse MDX for authors and tags
          const mdxPath = fullPath.replace(/\.[^.]+$/, ".mdx");
          if (existsSync(mdxPath)) {
            try {
              const content = readFileSync(mdxPath, "utf8");
              const authorMatch = content.match(/author:\s*"([^"]+)"/);
              if (authorMatch) {
                authorCounts[authorMatch[1]] = (authorCounts[authorMatch[1]] || 0) + 1;
              }
              const tagsMatch = content.match(/tags:\s*\[([^\]]*)\]/);
              if (tagsMatch) {
                const tags = tagsMatch[1]
                  .split(",")
                  .map((t) => t.trim().replace(/^"|"$/g, ""))
                  .filter(Boolean);
                for (const tag of tags) {
                  tagCounts[tag] = (tagCounts[tag] || 0) + 1;
                }
              }
            } catch {
              // skip
            }
          }
        }
      }
    }
  }

  scanDir(memesPath);

  stats.topAuthors = Object.entries(authorCounts)
    .sort((a, b) => b[1] - a[1])
    .slice(0, 5)
    .map(([author, count]) => ({ author, count }));

  stats.topTags = Object.entries(tagCounts)
    .sort((a, b) => b[1] - a[1])
    .slice(0, 10)
    .map(([tag, count]) => ({ tag, count }));

  stats.newestFile = newestFile;
  stats.oldestFile = oldestFile;

  if (options.json) {
    console.log(JSON.stringify(stats, null, 2));
    return;
  }

  function humanSize(bytes: number): string {
    const units = ["B", "KB", "MB", "GB"];
    let size = bytes;
    let i = 0;
    while (size >= 1024 && i < units.length - 1) {
      size /= 1024;
      i++;
    }
    return `${size.toFixed(1)} ${units[i]}`;
  }

  console.log(chalk.blue("\n📊 OpenMeme Repository Statistics\n"));
  console.log(chalk.gray("─".repeat(40)));

  console.log(`  ${chalk.white("Total Memes:")}    ${chalk.cyan(stats.totalImages.toString())}`);
  console.log(`  ${chalk.white("Total Size:")}     ${chalk.cyan(humanSize(stats.totalSize))}`);
  console.log(`  ${chalk.white("Avg Size:")}       ${chalk.cyan(stats.totalImages > 0 ? humanSize(stats.totalSize / stats.totalImages) : "0 B")}`);
  console.log(`  ${chalk.white("Categories:")}     ${chalk.cyan(Object.keys(stats.categories).length.toString())}`);

  console.log(chalk.gray("\n  Categories:"));
  for (const [cat, data] of Object.entries(stats.categories).sort((a, b) => b[1].count - a[1].count)) {
    const bar = "█".repeat(Math.min(20, Math.round(data.count / Math.max(...Object.values(stats.categories).map((d) => d.count)) * 20)));
    console.log(`    ${chalk.cyan(cat.padEnd(15))} ${bar} ${data.count}`);
  }

  console.log(chalk.gray("\n  Formats:"));
  for (const [fmt, count] of Object.entries(stats.formats)) {
    console.log(`    ${chalk.cyan(fmt.padEnd(10))} ${count}`);
  }

  if (stats.topAuthors.length > 0) {
    console.log(chalk.gray("\n  Top Authors:"));
    for (const { author, count } of stats.topAuthors) {
      console.log(`    ${chalk.cyan(author.padEnd(20))} ${count} memes`);
    }
  }

  if (stats.topTags.length > 0) {
    console.log(chalk.gray("\n  Top Tags:"));
    const tagLine = stats.topTags.map((t) => t.tag).join(", ");
    console.log(`    ${chalk.cyan(tagLine)}`);
  }

  console.log(chalk.gray("\n─".repeat(40)));
}
