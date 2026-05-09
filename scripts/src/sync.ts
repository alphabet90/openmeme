#!/usr/bin/env node
/**
 * scripts/sync.ts - Reddit Sync Automation
 *
 * Automates fetching new memes from specified subreddits and converting
 * them to the OpenMeme repository format. Can be run manually or via
 * GitHub Actions / cron.
 *
 * Usage:
 *   npx tsx scripts/sync.ts [options]
 *   npx tsx scripts/sync.ts --subreddit argentina --limit 50
 *   npx tsx scripts/sync.ts --config sync.config.json
 *   npx tsx scripts/sync.ts --daemon --interval 3600  # run every hour
 *
 * Inspired by Open Design's approach to automated content pipelines.
 */

import { readFileSync, existsSync, writeFileSync } from "fs";
import { resolve } from "path";
import { Command } from "commander";
import { runPipeline } from "@openmeme/scraper";
import type { ScraperOptions } from "@openmeme/scraper";

const logger = {
  info: (...args: unknown[]) => console.log("[sync]", ...args),
  warning: (...args: unknown[]) => console.warn("[sync]", ...args),
  error: (...args: unknown[]) => console.error("[sync]", ...args),
  success: (...args: unknown[]) => console.log("[sync] ✅", ...args),
};

// ---- Types ----

interface SyncConfig {
  subreddits: Array<{
    name: string;
    limit?: number;
    sort?: "hot" | "new" | "top";
    timeframe?: "hour" | "day" | "week" | "month" | "year" | "all";
    minCommentUpvotes?: number;
    batchSize?: number;
    locale?: string;
    classifier?: string;
  }>;
  defaults?: Omit<ScraperOptions, "subreddit">;
}

const DEFAULT_CONFIG: SyncConfig = {
  subreddits: [
    { name: "argentina", limit: 100 },
    { name: "memes", limit: 50, sort: "hot" },
    { name: "dankmemes", limit: 50, sort: "hot" },
  ],
  defaults: {
    batchSize: 10,
    classifyWorkers: 4,
    classifier: "claude",
    locale: "en",
    dryRun: false,
  },
};

// ---- CLI ----

const program = new Command();
program
  .name("sync")
  .description("Sync memes from Reddit to the OpenMeme repository")
  .option("-c, --config <path>", "Path to sync config file")
  .option("-s, --subreddit <name>", "Single subreddit to sync")
  .option("-l, --limit <n>", "Max posts to scan", "100")
  .option("--sort <sort>", "Sort order (hot|new|top)", "hot")
  .option("--timeframe <tf>", "Timeframe (hour|day|week|month|year|all)", "day")
  .option("--batch-size <n>", "Images per commit batch", "10")
  .option("--classifier <name>", "Classifier backend", "claude")
  .option("--locale <locale>", "Prompt locale", "en")
  .option("--dry-run", "Run without saving", false)
  .option("--daemon", "Run continuously", false)
  .option("-i, --interval <seconds>", "Daemon interval in seconds", "3600")
  .option("--workers <n>", "Classifier workers", "4")
  .option("--init-config", "Create a sample config file", false)
  .option("-v, --verbose", "Verbose logging", false)
  .parse();

const options = program.opts();

// ---- Config Management ----

function loadConfig(path: string): SyncConfig {
  const fullPath = resolve(path);
  if (!existsSync(fullPath)) {
    logger.warning(`Config file not found: ${fullPath}, using defaults`);
    return DEFAULT_CONFIG;
  }
  try {
    const content = readFileSync(fullPath, "utf8");
    const parsed = JSON.parse(content) as SyncConfig;
    logger.info(`Loaded sync config from ${fullPath}`);
    return { ...DEFAULT_CONFIG, ...parsed };
  } catch (err) {
    logger.error(`Failed to parse config: ${err}`);
    return DEFAULT_CONFIG;
  }
}

function createSampleConfig(): void {
  const path = resolve("sync.config.json");
  if (existsSync(path)) {
    logger.warning(`Config file already exists: ${path}`);
    return;
  }
  writeFileSync(path, JSON.stringify(DEFAULT_CONFIG, null, 2), "utf8");
  logger.success(`Created sample config: ${path}`);
  console.log("\nEdit the config file and run:");
  console.log(`  npx tsx scripts/sync.ts --config ${path}`);
}

// ---- Sync Logic ----

async function syncSubreddit(config: SyncConfig["subreddits"][0]): Promise<number> {
  logger.info(`\n--- Syncing r/${config.name} ---`);

  const opts: Partial<ScraperOptions> = {
    subreddit: config.name,
    limit: config.limit || 100,
    sort: config.sort || "hot",
    timeframe: config.timeframe || "day",
    batchSize: config.batchSize || 10,
    minCommentUpvotes: config.minCommentUpvotes || 0,
    locale: config.locale || "en",
    classifier: config.classifier || "claude",
    dryRun: false,
  };

  try {
    const total = await runPipeline(opts);
    logger.success(`r/${config.name}: ${total} memes saved`);
    return total;
  } catch (err) {
    logger.error(`r/${config.name} sync failed: ${err}`);
    return 0;
  }
}

async function runSync(config: SyncConfig): Promise<number> {
  let grandTotal = 0;
  logger.info(`Starting sync for ${config.subreddits.length} subreddit(s)\n`);

  for (const subConfig of config.subreddits) {
    const count = await syncSubreddit(subConfig);
    grandTotal += count;
  }

  logger.success(`\nSync complete: ${grandTotal} total memes saved`);
  return grandTotal;
}

async function runDaemon(config: SyncConfig, intervalSeconds: number): Promise<void> {
  logger.info(`Starting daemon mode (interval: ${intervalSeconds}s)`);

  while (true) {
    const startTime = Date.now();
    try {
      await runSync(config);
    } catch (err) {
      logger.error(`Sync cycle failed: ${err}`);
    }

    const elapsed = (Date.now() - startTime) / 1000;
    const sleepSeconds = Math.max(0, intervalSeconds - elapsed);
    logger.info(`Next sync in ${sleepSeconds.toFixed(0)}s`);

    await new Promise((resolve) => setTimeout(resolve, sleepSeconds * 1000));
  }
}

// ---- Main ----

async function main() {
  logger.info("OpenMeme Sync - Reddit Meme Synchronization\n");

  if (options.initConfig) {
    createSampleConfig();
    process.exit(0);
  }

  let config: SyncConfig;

  if (options.config) {
    config = loadConfig(options.config);
  } else if (options.subreddit) {
    // Single subreddit mode
    config = {
      subreddits: [
        {
          name: options.subreddit,
          limit: parseInt(options.limit, 10),
          sort: options.sort,
          timeframe: options.timeframe,
          batchSize: parseInt(options.batchSize, 10),
          classifier: options.classifier,
          locale: options.locale,
        },
      ],
    };
  } else {
    // Try loading default config
    if (existsSync(resolve("sync.config.json"))) {
      config = loadConfig("sync.config.json");
    } else {
      logger.info("No config file found, using defaults");
      config = DEFAULT_CONFIG;
    }
  }

  if (config.subreddits.length === 0) {
    logger.error("No subreddits configured");
    process.exit(1);
  }

  if (options.daemon) {
    const interval = parseInt(options.interval, 10);
    await runDaemon(config, interval);
  } else {
    const total = await runSync(config);
    process.exit(total > 0 ? 0 : 0); // 0 even if no memes found (not an error)
  }
}

main().catch((err) => {
  logger.error("Sync failed:", err);
  process.exit(1);
});
