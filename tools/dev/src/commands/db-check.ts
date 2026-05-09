/**
 * openmeme-dev db-check - Check repository for inconsistencies
 */

import { existsSync, readdirSync, readFileSync, statSync } from "fs";
import { join, extname } from "path";
import chalk from "chalk";

interface DbCheckOptions {
  path?: string;
  orphanedImages?: boolean;
  orphanedMdx?: boolean;
}

export async function dbCheckCommand(options: DbCheckOptions): Promise<void> {
  const repoPath = join(process.cwd(), options.path || ".");
  const memesPath = join(repoPath, "memes");

  if (!existsSync(memesPath)) {
    console.log(chalk.yellow("No memes directory found."));
    return;
  }

  console.log(chalk.blue("\n🔍 Repository Consistency Check\n"));

  const images: string[] = [];
  const mdxFiles: string[] = [];

  function scanDir(dir: string) {
    for (const entry of readdirSync(dir, { withFileTypes: true })) {
      const fullPath = join(dir, entry.name);
      if (entry.isDirectory()) {
        scanDir(fullPath);
      } else if (entry.isFile()) {
        const ext = extname(entry.name).toLowerCase();
        if ([".jpg", ".jpeg", ".png", ".gif", ".webp"].includes(ext)) {
          images.push(fullPath);
        } else if (ext === ".mdx") {
          mdxFiles.push(fullPath);
        }
      }
    }
  }

  scanDir(memesPath);

  console.log(`  Images: ${chalk.cyan(images.length.toString())}`);
  console.log(`  MDX:    ${chalk.cyan(mdxFiles.length.toString())}`);

  // Orphaned images (no MDX)
  const orphanedImages = images.filter((img) => {
    const base = img.replace(/\.[^.]+$/, "");
    return !existsSync(`${base}.mdx`);
  });

  // Orphaned MDX (no image)
  const orphanedMdx = mdxFiles.filter((mdx) => {
    const base = mdx.replace(/\.mdx$/, "");
    return ![".jpg", ".jpeg", ".png", ".gif", ".webp"].some((ext) =>
      existsSync(`${base}${ext}`)
    );
  });

  if (orphanedImages.length > 0) {
    console.log(chalk.yellow(`\n  ⚠️  ${orphanedImages.length} image(s) without MDX:`));
    for (const img of orphanedImages.slice(0, 10)) {
      console.log(chalk.gray(`     ${img.replace(repoPath, ".")}`));
    }
    if (orphanedImages.length > 10) {
      console.log(chalk.gray(`     ... and ${orphanedImages.length - 10} more`));
    }
  }

  if (orphanedMdx.length > 0) {
    console.log(chalk.yellow(`\n  ⚠️  ${orphanedMdx.length} MDX file(s) without image:`));
    for (const mdx of orphanedMdx.slice(0, 10)) {
      console.log(chalk.gray(`     ${mdx.replace(repoPath, ".")}`));
    }
    if (orphanedMdx.length > 10) {
      console.log(chalk.gray(`     ... and ${orphanedMdx.length - 10} more`));
    }
  }

  // Size analysis
  const totalSize = images.reduce((sum, img) => sum + statSync(img).size, 0);
  const avgSize = images.length > 0 ? totalSize / images.length : 0;

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

  console.log(chalk.gray(`\n  Total size: ${humanSize(totalSize)}`));
  console.log(chalk.gray(`  Avg size:   ${humanSize(avgSize)}`));

  const hasIssues = orphanedImages.length > 0 || orphanedMdx.length > 0;
  if (!hasIssues) {
    console.log(chalk.green("\n  ✅ Repository is consistent!\n"));
  } else {
    console.log();
  }
}
