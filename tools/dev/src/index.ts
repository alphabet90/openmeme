#!/usr/bin/env node
/**
 * tools/dev - OpenMeme Developer Utilities
 *
 * Provides developer-focused tools for maintaining the OpenMeme repository:
 *   - lint: Check MDX frontmatter consistency
 *   - fix: Auto-fix common issues
 *   - generate-prompts: Generate classifier prompts for new locales
 *   - benchmark: Benchmark the scraper pipeline
 *   - db-check: Check for database inconsistencies
 */

import { Command } from "commander";
import chalk from "chalk";

const program = new Command();

program
  .name("openmeme-dev")
  .description("Developer utilities for OpenMeme")
  .version("1.0.0");

program
  .command("lint")
  .description("Lint MDX frontmatter and image consistency")
  .option("-p, --path <path>", "Path to memes directory", "./memes")
  .option("--fix", "Auto-fix issues", false)
  .action(async (options) => {
    const { lintCommand } = await import("./commands/lint.js");
    await lintCommand(options);
  });

program
  .command("generate-prompt")
  .description("Generate a classifier prompt for a new locale")
  .argument("<locale>", "Locale code (e.g. es, pt-BR)")
  .option("-o, --output <path>", "Output file path")
  .option("--template <path>", "Template prompt file")
  .action(async (locale, options) => {
    const { generatePromptCommand } = await import("./commands/generate-prompt.js");
    await generatePromptCommand(locale, options);
  });

program
  .command("benchmark")
  .description("Benchmark the scraper pipeline")
  .option("-s, --subreddit <name>", "Subreddit to benchmark with", "memes")
  .option("-l, --limit <n>", "Number of posts", "50")
  .option("--iterations <n>", "Number of iterations", "3")
  .action(async (options) => {
    const { benchmarkCommand } = await import("./commands/benchmark.js");
    await benchmarkCommand(options);
  });

program
  .command("db-check")
  .description("Check repository for inconsistencies")
  .option("-p, --path <path>", "Repository path", ".")
  .option("--orphaned-images", "Check for images without MDX", false)
  .option("--orphaned-mdx", "Check for MDX without images", false)
  .action(async (options) => {
    const { dbCheckCommand } = await import("./commands/db-check.js");
    await dbCheckCommand(options);
  });

program
  .command("optimize-images")
  .description("Generate responsive image sizes with @squoosh/cli and update MDX references")
  .option("-p, --path <path>", "Path to memes directory", "./memes")
  .option("--threshold <kb>", "Only optimize images larger than this (KB)", "80")
  .option("-q, --quality <n>", "Encoder quality for jpg (1-100)", "75")
  .option("--sizes <list>", "Comma-separated widths to generate, aspect ratio preserved", "800,340")
  .option("--limit <n>", "Process at most N images (largest first)")
  .option("--batch-size <n>", "Images per squoosh invocation", "20")
  .option("--dry-run", "List candidates without modifying files", false)
  .action(async (options) => {
    const { optimizeImagesCommand } = await import("./commands/optimize-images.js");
    await optimizeImagesCommand(options);
  });

program
  .command("setup-hooks")
  .description("Setup git hooks for the repository")
  .option("--pre-commit", "Install pre-commit hook", true)
  .action(async (options) => {
    const { setupHooksCommand } = await import("./commands/setup-hooks.js");
    await setupHooksCommand(options);
  });

program.parse();
