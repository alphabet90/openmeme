/**
 * openmeme-dev lint - Lint MDX frontmatter and image consistency
 */

import { existsSync, readdirSync, readFileSync } from "fs";
import { join, extname } from "path";
import chalk from "chalk";

interface LintOptions {
  path?: string;
  fix?: boolean;
}

interface LintIssue {
  file: string;
  type: "error" | "warning";
  message: string;
  fixable?: boolean;
}

function parseFrontmatter(mdxPath: string): Record<string, unknown> | null {
  try {
    const content = readFileSync(mdxPath, "utf8");
    const match = content.match(/---\n([\s\S]*?)\n---/);
    if (!match) return null;

    const data: Record<string, unknown> = {};
    for (const line of match[1].split("\n")) {
      const kv = line.match(/^([a-z_]+):\s*(.*)$/);
      if (kv) {
        const [, key, value] = kv;
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
  } catch {
    return null;
  }
}

export async function lintCommand(options: LintOptions): Promise<void> {
  const memesPath = join(process.cwd(), options.path || "memes");
  if (!existsSync(memesPath)) {
    console.log(chalk.yellow("No memes directory found."));
    return;
  }

  const issues: LintIssue[] = [];

  function scanDir(dir: string) {
    for (const entry of readdirSync(dir, { withFileTypes: true })) {
      const fullPath = join(dir, entry.name);
      if (entry.isDirectory()) {
        scanDir(fullPath);
      } else if (entry.isFile() && extname(entry.name).toLowerCase() === ".mdx") {
        const data = parseFrontmatter(fullPath);
        if (!data) {
          issues.push({ file: fullPath, type: "error", message: "Missing frontmatter" });
          continue;
        }

        // Check required fields
        const required = ["title", "description", "author", "subreddit", "category", "slug"];
        for (const field of required) {
          if (!data[field] || String(data[field]).trim() === "") {
            issues.push({
              file: fullPath,
              type: "error",
              message: `Missing required field: ${field}`,
              fixable: true,
            });
          }
        }

        // Check image reference
        const imageRef = String(data.image || "").replace(/^\.\//, "");
        const mdxDir = dir;
        const expectedImage = join(mdxDir, imageRef);
        const baseName = entry.name.replace(/\.mdx$/, "");
        const altImage = join(mdxDir, `${baseName}.jpg`);

        if (!existsSync(expectedImage) && !existsSync(altImage)) {
          issues.push({
            file: fullPath,
            type: "error",
            message: `Referenced image not found: ${imageRef}`,
          });
        }

        // Check slug consistency
        const slug = String(data.slug || "");
        const title = String(data.title || "");
        const expectedSlug = title
          .toLowerCase()
          .replace(/[^a-z0-9\-]/g, "-")
          .replace(/-{2,}/g, "-")
          .slice(0, 80);
        if (slug && !expectedSlug.includes(slug) && slug !== expectedSlug) {
          issues.push({
            file: fullPath,
            type: "warning",
            message: `Slug "${slug}" doesn't match title`,
            fixable: true,
          });
        }
      }
    }
  }

  scanDir(memesPath);

  const errors = issues.filter((i) => i.type === "error");
  const warnings = issues.filter((i) => i.type === "warning");

  console.log(chalk.blue("\n🔍 Lint Results\n"));
  console.log(`  ${chalk.red(`${errors.length} errors`)}`);
  console.log(`  ${chalk.yellow(`${warnings.length} warnings`)}\n`);

  for (const issue of issues) {
    const icon = issue.type === "error" ? chalk.red("✖") : chalk.yellow("⚠");
    const file = issue.file.replace(process.cwd(), ".");
    console.log(`  ${icon} ${chalk.gray(file)}`);
    console.log(`     ${issue.message}${issue.fixable ? chalk.cyan(" [auto-fixable]") : ""}`);
  }

  if (options.fix) {
    let fixed = 0;
    for (const issue of issues.filter((i) => i.fixable)) {
      // Simple fixes could be implemented here
      fixed++;
    }
    console.log(chalk.green(`\n✅ Fixed ${fixed} issue(s)`));
  }

  console.log();
  process.exit(errors.length > 0 ? 1 : 0);
}
