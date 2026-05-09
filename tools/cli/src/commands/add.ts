/**
 * openmeme add - Add a meme to the repository
 */

import { existsSync, copyFileSync, mkdirSync, writeFileSync } from "fs";
import { resolve, join, extname, basename } from "path";
import chalk from "chalk";
import ora from "ora";
import { sanitizeSlug } from "@openmeme/scraper";

interface AddOptions {
  title?: string;
  description?: string;
  category?: string;
  subreddit?: string;
  author?: string;
  tags?: string;
  url?: string;
  batch?: string;
  interactive?: boolean;
  yes?: boolean;
}

interface MemeFormData {
  title: string;
  description: string;
  category: string;
  subreddit: string;
  author: string;
  tags: string[];
  sourceUrl: string;
}

function yamlStr(s: string): string {
  return (s || "")
    .replace(/\\/g, "\\\\")
    .replace(/"/g, '\\"')
    .replace(/\n/g, "\\n")
    .replace(/\r/g, "\\r");
}

function generateMdx(data: MemeFormData, imageName: string): string {
  const slug = sanitizeSlug(data.title);
  const tagsStr = data.tags.map((t) => `"${yamlStr(t)}"`).join(", ");
  const now = new Date().toISOString();

  return `---
title: "${yamlStr(data.title)}"
description: "${yamlStr(data.description)}"
author: "${yamlStr(data.author)}"
subreddit: "${yamlStr(data.subreddit)}"
category: "${yamlStr(data.category)}"
slug: "${slug}"
score: 0
created_at: "${now}"
source_url: "${yamlStr(data.sourceUrl)}"
post_url: ""
image: "./${imageName}"
tags: [${tagsStr}]
---

# ${data.title}

${data.description}

**Category**: ${data.category} | **Author**: u/${data.author}

[View source](${data.sourceUrl || "#"})
`;
}

function askQuestion(prompt: string, defaultValue: string = ""): Promise<string> {
  const readline = require("readline");
  const rl = readline.createInterface({
    input: process.stdin,
    output: process.stdout,
  });
  return new Promise((resolve) => {
    rl.question(`${prompt}${defaultValue ? ` (${defaultValue})` : ""}: `, (answer: string) => {
      rl.close();
      resolve(answer.trim() || defaultValue);
    });
  });
}

async function interactiveForm(imagePath: string): Promise<MemeFormData> {
  console.log(chalk.blue("\n📋 Enter meme metadata:\n"));

  const defaultTitle = basename(imagePath, extname(imagePath)).replace(/[-_]/g, " ");
  const title = await askQuestion("Title", defaultTitle);
  const description = await askQuestion("Description", title);
  const category = await askQuestion("Category", "other");
  const subreddit = await askQuestion("Subreddit", "unknown");
  const author = await askQuestion("Author", "unknown");
  const tagsStr = await askQuestion("Tags (comma-separated)", category);
  const sourceUrl = await askQuestion("Source URL", "");

  return {
    title,
    description,
    category,
    subreddit,
    author,
    tags: tagsStr.split(",").map((t) => t.trim()).filter(Boolean),
    sourceUrl,
  };
}

async function addSingleImage(imagePath: string, opts: AddOptions): Promise<void> {
  const spinner = ora("Processing...").start();

  if (!existsSync(imagePath)) {
    spinner.fail(`Image not found: ${imagePath}`);
    return;
  }

  spinner.stop();

  let data: MemeFormData;

  if (opts.batch) {
    // Batch mode - use filename as defaults
    const name = basename(imagePath, extname(imagePath));
    data = {
      title: opts.title || name.replace(/[-_]/g, " "),
      description: opts.description || opts.title || name,
      category: opts.category || "other",
      subreddit: opts.subreddit || "unknown",
      author: opts.author || "unknown",
      tags: (opts.tags || opts.category || "")
        .split(",")
        .map((t: string) => t.trim())
        .filter(Boolean),
      sourceUrl: opts.url || "",
    };
  } else if (opts.interactive || (!opts.title && !opts.yes)) {
    data = await interactiveForm(imagePath);
  } else {
    data = {
      title: opts.title || "Untitled Meme",
      description: opts.description || opts.title || "No description",
      category: opts.category || "other",
      subreddit: opts.subreddit || "unknown",
      author: opts.author || "unknown",
      tags: (opts.tags || "")
        .split(",")
        .map((t: string) => t.trim())
        .filter(Boolean),
      sourceUrl: opts.url || "",
    };
  }

  // Preview
  if (!opts.yes) {
    console.log(chalk.gray("\n📄 Preview:"));
    console.log(chalk.gray("─".repeat(40)));
    console.log(`  Title:       ${data.title}`);
    console.log(`  Description: ${data.description.slice(0, 60)}${data.description.length > 60 ? "..." : ""}`);
    console.log(`  Category:    ${data.category}`);
    console.log(`  Tags:        ${data.tags.join(", ") || "none"}`);
    console.log(chalk.gray("─".repeat(40)));

    const answer = await askQuestion("Save? (y/N)", "y");
    if (answer.toLowerCase() !== "y") {
      console.log(chalk.yellow("Cancelled."));
      return;
    }
  }

  const spinner2 = ora("Saving meme...").start();

  try {
    const repoPath = resolve(".");
    const categorySlug = sanitizeSlug(data.category) || "other";
    const destDir = join(repoPath, "memes", categorySlug);
    mkdirSync(destDir, { recursive: true });

    const ext = extname(imagePath).toLowerCase() || ".jpg";
    const slug = sanitizeSlug(data.title);
    const imageName = `${slug}${ext}`;
    const imageDest = join(destDir, imageName);

    copyFileSync(imagePath, imageDest);

    const mdxContent = generateMdx(data, imageName);
    const mdxPath = imageDest.replace(/\.[^.]+$/, ".mdx");
    writeFileSync(mdxPath, mdxContent, "utf8");

    spinner2.succeed(`Saved meme: ${chalk.green(imageName)}`);
    console.log(`  Image: ${chalk.gray(imageDest)}`);
    console.log(`  MDX:   ${chalk.gray(mdxPath)}`);
  } catch (err) {
    spinner2.fail(`Failed: ${err}`);
  }
}

async function batchAdd(dirPath: string, opts: AddOptions): Promise<void> {
  const { readdirSync, statSync } = require("fs");
  const { join } = require("path");

  const fullPath = resolve(dirPath);
  if (!existsSync(fullPath)) {
    console.error(chalk.red(`Directory not found: ${fullPath}`));
    return;
  }

  const exts = [".jpg", ".jpeg", ".png", ".gif", ".webp"];
  const files: string[] = [];

  for (const entry of readdirSync(fullPath, { withFileTypes: true })) {
    if (entry.isFile() && exts.includes(extname(entry.name).toLowerCase())) {
      files.push(join(fullPath, entry.name));
    }
  }

  console.log(chalk.blue(`\n📁 Found ${files.length} image(s) in ${fullPath}\n`));

  for (let i = 0; i < files.length; i++) {
    console.log(chalk.gray(`[${i + 1}/${files.length}]`));
    await addSingleImage(files[i], { ...opts, batch: undefined });
  }

  console.log(chalk.green(`\n✅ Batch complete: ${files.length} meme(s) added`));
}

export async function addCommand(image: string | undefined, options: AddOptions): Promise<void> {
  if (options.batch) {
    await batchAdd(options.batch, options);
  } else if (image) {
    await addSingleImage(resolve(image), options);
  } else {
    options.interactive = true;
    console.log(chalk.blue("\n🖼️  Add a meme to OpenMeme\n"));
    const imagePath = await askQuestion("Image path");
    if (!imagePath) {
      console.log(chalk.yellow("No image provided."));
      return;
    }
    await addSingleImage(resolve(imagePath), options);
  }
}
