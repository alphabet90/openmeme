#!/usr/bin/env node
/**
 * CLI entry point for the scraper.
 * Mirrors reddit.memes/main.py functionality.
 */

import { Command } from "commander";
import { runPipeline } from "./pipeline.js";
import { CONFIG } from "./config.js";
import type { ScraperOptions } from "./models.js";

const program = new Command();

program
  .name("openmeme-scraper")
  .description("Scrape Reddit, classify memes, and save them to the repo")
  .version("1.0.0");

program
  .command("scrape")
  .description("Run the meme scraper pipeline")
  .option("-s, --subreddit <sub>", "Subreddit to scrape", CONFIG.SUBREDDIT)
  .option("-l, --limit <n>", "Max posts to scan", "100")
  .option("-b, --batch-size <n>", "Images per git commit batch", "10")
  .option("-w, --classify-workers <n>", "Parallel classifier subprocesses", String(CONFIG.CLASSIFY_WORKERS))
  .option("-c, --classifier <name>", "Vision classifier backend (claude|codex)", CONFIG.CLASSIFIER)
  .option("--locale <locale>", "Prompt locale (e.g. en, es-AR)", "en")
  .option("--repo-path <path>", "Path to git repo", CONFIG.REPO_PATH)
  .option("--dry-run", "Classify without saving or committing", false)
  .option("--per-post", "Classify each post's images immediately", false)
  .option("--no-branch", "Skip auto branch creation", false)
  .option("--reset-bloom", "Delete Bloom filter and reprocess", false)
  .option("--from-file <path>", "Load posts from JSON file")
  .option("--post-url <url>", "Process a single Reddit post URL")
  .option("--min-comment-upvotes <n>", "Minimum upvotes for comment images", String(CONFIG.MIN_COMMENT_UPVOTES))
  .option("--sort <sort>", "Post sort order (hot|new|top)", "hot")
  .option("--timeframe <tf>", "Timeframe for top sort (hour|day|week|month|year|all)", "day")
  .option("--page <n>", "Page number to start from", "1")
  .option("--skip-content-dedup", "Skip SHA1 content-duplicate check", false)
  .action(async (options) => {
    const opts: Partial<ScraperOptions> = {
      subreddit: options.subreddit,
      limit: parseInt(options.limit, 10),
      batchSize: parseInt(options.batchSize, 10),
      classifyWorkers: parseInt(options.classifyWorkers, 10),
      classifier: options.classifier,
      locale: options.locale,
      dryRun: options.dryRun,
      perPost: options.perPost,
      noBranch: options.noBranch,
      resetBloom: options.resetBloom,
      fromFile: options.fromFile,
      postUrl: options.postUrl,
      minCommentUpvotes: parseInt(options.minCommentUpvotes, 10),
      sort: options.sort,
      timeframe: options.timeframe,
      page: parseInt(options.page, 10),
      skipContentDedup: options.skipContentDedup,
    };

    try {
      const total = await runPipeline(opts);
      console.log(`\nTotal memes saved: ${total}`);
      process.exit(0);
    } catch (err) {
      console.error("Pipeline failed:", err);
      process.exit(1);
    }
  });

program
  .command("validate")
  .description("Validate memes in the repository")
  .option("-p, --path <path>", "Path to memes directory", "./memes")
  .action(async (options) => {
    const { validateMemes } = await import("./validator.js");
    const ok = await validateMemes(options.path);
    process.exit(ok ? 0 : 1);
  });

program.parse();
