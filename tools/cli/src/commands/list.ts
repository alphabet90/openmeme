/**
 * openmeme list - List memes in the repository
 */

import { existsSync, readdirSync, readFileSync, statSync } from "fs";
import { join, extname } from "path";
import chalk from "chalk";

interface ListOptions {
  category?: string;
  limit?: string;
  json?: boolean;
}

interface MemeEntry {
  title: string;
  category: string;
  author: string;
  subreddit: string;
  tags: string[];
  imagePath: string;
  size: number;
}

export async function listCommand(options: ListOptions): Promise<void> {
  const memesPath = join(process.cwd(), "memes");
  if (!existsSync(memesPath)) {
    console.log(chalk.yellow("No memes directory found."));
    return;
  }

  const entries: MemeEntry[] = [];
  const limit = parseInt(options.limit || "50", 10);

  function scanDir(dir: string) {
    for (const entry of readdirSync(dir, { withFileTypes: true })) {
      const fullPath = join(dir, entry.name);
      if (entry.isDirectory()) {
        scanDir(fullPath);
      } else if (entry.isFile() && extname(entry.name).toLowerCase() === ".mdx") {
        try {
          const content = readFileSync(fullPath, "utf8");
          const frontmatter = content.match(/---\n([\s\S]*?)\n---/);
          if (frontmatter) {
            const data = parseFrontmatter(frontmatter[1]);
            const imagePath = fullPath.replace(/\.mdx$/, ".jpg");
            const size = existsSync(imagePath) ? statSync(imagePath).size : 0;
            if (!options.category || data.category === options.category) {
              entries.push({
                title: String(data.title || entry.name),
                category: String(data.category || "unknown"),
                author: String(data.author || "unknown"),
                subreddit: String(data.subreddit || "unknown"),
                tags: (data.tags || []) as string[],
                imagePath: fullPath,
                size,
              });
            }
          }
        } catch {
          // skip
        }
      }
    }
  }

  scanDir(memesPath);

  if (options.json) {
    console.log(JSON.stringify(entries.slice(0, limit), null, 2));
    return;
  }

  if (entries.length === 0) {
    console.log(chalk.yellow("No memes found."));
    return;
  }

  console.log(chalk.blue(`\n📂 Memes (${Math.min(entries.length, limit)} of ${entries}):\n`));

  const grouped = entries.reduce<Record<string, MemeEntry[]>>((acc, e) => {
    acc[e.category] = acc[e.category] || [];
    acc[e.category].push(e);
    return acc;
  }, {});

  for (const [cat, memes] of Object.entries(grouped)) {
    console.log(chalk.cyan(`  ${cat} (${memes.length})`));
    for (const meme of memes.slice(0, limit)) {
      const sizeStr = meme.size > 0 ? `${(meme.size / 1024).toFixed(0)}KB` : "?";
      console.log(`    ${chalk.white(meme.title.slice(0, 50))}${meme.title.length > 50 ? "..." : ""} ${chalk.gray(`[${sizeStr}]`)}`);
    }
  }
  console.log();
}

function parseFrontmatter(fm: string): Record<string, string | string[]> {
  const data: Record<string, string | string[]> = {};
  for (const line of fm.split("\n")) {
    const match = line.match(/^([a-z_]+):\s*(.*)$/);
    if (match) {
      const [, key, value] = match;
      if (value.startsWith("[") && value.endsWith("]")) {
        data[key] = value
          .slice(1, -1)
          .split(",")
          .map((s) => s.trim().replace(/^"|"$/g, ""))
          .filter(Boolean);
      } else if (value.startsWith('"') && value.endsWith('"')) {
        data[key] = value.slice(1, -1);
      } else {
        data[key] = value;
      }
    }
  }
  return data;
}
