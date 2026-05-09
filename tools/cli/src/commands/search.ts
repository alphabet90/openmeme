/**
 * openmeme search - Search memes by tag, title, or description
 */

import { existsSync, readdirSync, readFileSync } from "fs";
import { join, extname } from "path";
import chalk from "chalk";

interface SearchOptions {
  tag?: boolean;
  json?: boolean;
}

interface SearchResult {
  title: string;
  category: string;
  description: string;
  tags: string[];
  score: number;
  path: string;
}

export async function searchCommand(query: string, options: SearchOptions): Promise<void> {
  const memesPath = join(process.cwd(), "memes");
  if (!existsSync(memesPath)) {
    console.log(chalk.yellow("No memes directory found."));
    return;
  }

  const lowerQuery = query.toLowerCase();
  const results: SearchResult[] = [];

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
            const title = String(data.title || "").toLowerCase();
            const desc = String(data.description || "").toLowerCase();
            const tags = (data.tags || []).map((t: string) => t.toLowerCase());
            const category = String(data.category || "").toLowerCase();

            let match = false;
            if (options.tag) {
              match = tags.some((t: string) => t.includes(lowerQuery));
            } else {
              match =
                title.includes(lowerQuery) ||
                desc.includes(lowerQuery) ||
                tags.some((t: string) => t.includes(lowerQuery)) ||
                category.includes(lowerQuery);
            }

            if (match) {
              results.push({
                title: String(data.title || "Untitled"),
                category: String(data.category || "unknown"),
                description: String(data.description || ""),
                tags: data.tags || [],
                score: parseInt(String(data.score || "0"), 10),
                path: fullPath,
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
    console.log(JSON.stringify(results, null, 2));
    return;
  }

  console.log(chalk.blue(`\n🔍 Search results for "${query}" (${results.length} found):\n`));

  for (const result of results) {
    console.log(chalk.white(`  ${result.title}`));
    console.log(chalk.gray(`    Category: ${result.category} | Score: ${result.score}`));
    if (result.tags.length > 0) {
      console.log(chalk.gray(`    Tags: ${result.tags.join(", ")}`));
    }
    if (result.description) {
      console.log(chalk.gray(`    ${result.description.slice(0, 80)}${result.description.length > 80 ? "..." : ""}`));
    }
    console.log();
  }
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
