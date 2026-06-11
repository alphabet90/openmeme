/**
 * Meme saver and git committer.
 * Ported from reddit.memes/src/saver.py
 */

import { copyFileSync, writeFileSync, mkdirSync } from "fs";
import { dirname, join, resolve } from "path";
import { execSync } from "child_process";
import type { ClassificationResult, PostMetadata } from "./models.js";

const logger = {
  info: (...args: unknown[]) => console.log("[saver]", ...args),
  warning: (...args: unknown[]) => console.warn("[saver]", ...args),
  error: (...args: unknown[]) => console.error("[saver]", ...args),
  debug: (...args: unknown[]) => {
    if (process.env.DEBUG) console.log("[saver:debug]", ...args);
  },
};

function yamlStr(s: string): string {
  return s
    .replace(/\\/g, "\\\\")
    .replace(/"/g, '\\"')
    .replace(/\n/g, "\\n")
    .replace(/\r/g, "\\r");
}

export function sanitizeSlug(slug: string): string {
  return (slug || "meme")
    .toLowerCase()
    .trim()
    .replace(/[^a-z0-9\-]/g, "-")
    .replace(/-{2,}/g, "-")
    .replace(/^-|-$/g, "")
    .slice(0, 80) || "meme";
}

function uniqueDest(basePath: string): string {
  const { existsSync } = require("fs");
  if (!existsSync(basePath)) return basePath;

  const ext = basePath.match(/\.[^.]+$/)?.[0] || "";
  const stem = basePath.replace(/\.[^.]+$/, "");
  for (let i = 2; i < 100; i++) {
    const candidate = `${stem}-${i}${ext}`;
    if (!existsSync(candidate)) return candidate;
  }
  const hash = Math.abs(basePath.split("").reduce((a, c) => a + c.charCodeAt(0), 0)) % 10000;
  return `${stem}-${hash}${ext}`;
}

export function saveMeme(
  imagePath: string,
  result: ClassificationResult,
  repoPath: string
): string | null {
  const category = sanitizeSlug(result.category) || "other";
  const slug = sanitizeSlug(result.filename_slug) || "meme";
  const extMatch = imagePath.match(/\.[^.]+$/);
  const ext = extMatch ? extMatch[0].toLowerCase() : ".jpg";

  const destDir = join(repoPath, "memes", category);
  mkdirSync(destDir, { recursive: true });

  const dest = uniqueDest(join(destDir, `${slug}${ext}`));
  copyFileSync(imagePath, dest);
  logger.info(`Saved meme -> ${dest}`);
  return dest;
}

function mdxPath(imageDest: string, locale: string): string {
  const normalized = locale.replace("_", "-").toLowerCase();
  const ext = imageDest.match(/\.[^.]+$/)?.[0] || "";
  const stem = imageDest.replace(/\.[^.]+$/, "");
  if (normalized === "en") {
    return `${stem}.mdx`;
  }
  return `${stem}.${normalized}.mdx`;
}

export function writeMdx(
  imageDest: string,
  result: ClassificationResult,
  meta: PostMetadata,
  repoPath: string,
  locale: string = "en"
): string | null {
  const mdxFile = mdxPath(imageDest, locale);
  const imageName = imageDest.split("/").pop() || "";
  const createdAt = new Date(meta.created_utc * 1000).toISOString();
  const postUrl = `https://reddit.com${meta.permalink}`;
  const slug = sanitizeSlug(result.filename_slug) || "meme";
  const title = result.title || meta.title || slug;

  const tagItems = [meta.subreddit, result.category, ...result.tags];
  const seen = new Set<string>();
  const uniqueTags: string[] = [];
  for (const t of tagItems) {
    const trimmed = (t || "").trim();
    if (trimmed && !seen.has(trimmed)) {
      seen.add(trimmed);
      uniqueTags.push(trimmed);
    }
  }
  const tagsStr = uniqueTags.map((t) => `"${yamlStr(t)}"`).join(", ");

  const content = `---
title: "${yamlStr(title)}"
description: "${yamlStr(result.description)}"
author: "${yamlStr(meta.author)}"
subreddit: "${yamlStr(meta.subreddit)}"
category: "${yamlStr(result.category)}"
slug: "${slug}"
score: ${meta.score}
created_at: "${createdAt}"
source_url: "${yamlStr(result.url)}"
post_url: "${postUrl}"
image: "./${imageName}"
tags: [${tagsStr}]
---

# ${title}

${result.description}

**Category**: ${result.category} | **Author**: u/${meta.author} | **Score**: ${meta.score} upvotes

[View original post on Reddit](${postUrl})
`;

  try {
    writeFileSync(mdxFile, content, "utf8");
    logger.info(`Wrote MDX -> ${mdxFile}`);
    return mdxFile;
  } catch (err) {
    logger.warning(`Failed to write MDX for ${imageName}: ${err}`);
    return null;
  }
}

export function gitCreateBranch(repoPath: string, branchName: string): boolean {
  try {
    execSync(`git checkout -b "${branchName}"`, {
      cwd: repoPath,
      stdio: ["ignore", "pipe", "pipe"],
    });
    logger.info(`Created and checked out branch: ${branchName}`);
    return true;
  } catch (err) {
    logger.error(`git checkout -b failed: ${err}`);
    return false;
  }
}

export function gitAdd(repoPath: string, paths: string[]): boolean {
  try {
    execSync(`git add ${paths.map((p) => `"${p}"`).join(" ")}`, {
      cwd: repoPath,
      stdio: ["ignore", "pipe", "pipe"],
    });
    return true;
  } catch (err) {
    logger.error(`git add failed: ${err}`);
    return false;
  }
}

export function gitCommit(repoPath: string, message: string): boolean {
  try {
    execSync(`git commit -m "${message.replace(/"/g, '\\"')}"`, {
      cwd: repoPath,
      stdio: ["ignore", "pipe", "pipe"],
    });
    logger.info(`Committed: ${message}`);
    return true;
  } catch (err: unknown) {
    const errorStr = String(err);
    if (errorStr.includes("nothing to commit")) {
      logger.debug("Nothing to commit");
      return true;
    }
    logger.error(`git commit failed: ${err}`);
    return false;
  }
}

export interface BatchItem {
  result: ClassificationResult;
  tmpPath: string;
}

export function saveAndCommitBatch(
  items: BatchItem[],
  repoPath: string,
  batchNum: number,
  subreddit: string,
  urlToMeta?: Record<string, PostMetadata>,
  locale: string = "en"
): string[] {
  const savedImages: string[] = [];
  const savedMdx: string[] = [];

  for (const { result, tmpPath } of items) {
    if (!result.is_meme) continue;
    const imageDest = saveMeme(tmpPath, result, repoPath);
    if (imageDest) {
      savedImages.push(imageDest);
      const meta = urlToMeta?.[result.url];
      if (meta) {
        const mdx = writeMdx(imageDest, result, meta, repoPath, locale);
        if (mdx) savedMdx.push(mdx);
      }
    }
  }

  if (savedImages.length === 0) return [];

  if (gitAdd(repoPath, [...savedImages, ...savedMdx])) {
    const categoryCounts: Record<string, number> = {};
    for (const { result } of items) {
      if (result.is_meme) {
        const cat = sanitizeSlug(result.category) || "other";
        categoryCounts[cat] = (categoryCounts[cat] || 0) + 1;
      }
    }

    const summary = Object.entries(categoryCounts)
      .sort(([a], [b]) => a.localeCompare(b))
      .map(([cat, n]) => `${cat}(${n})`)
      .join(", ");

    const msg = `Add ${savedImages.length} memes from r/${subreddit} batch ${batchNum} [${summary}]`;
    gitCommit(repoPath, msg);
  }

  return savedImages;
}
