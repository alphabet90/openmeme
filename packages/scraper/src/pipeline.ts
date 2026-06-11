/**
 * Meme scraper pipeline.
 * Ported from reddit.memes/src/pipeline.py and main.py
 * Orchestrates scraping, downloading, classification, and saving.
 */

import { mkdirSync, rmSync } from "fs";
import { resolve } from "path";
import { CONFIG } from "./config.js";
import { PostTracker } from "./post-tracker.js";
import { createClassifier } from "./classifier.js";
import {
  fetchPosts,
  fetchSinglePost,
  fetchCommentImages,
  extractPostImageUrls,
  type RedditPost,
} from "./scraper.js";
import { downloadBatch, computeFileSha1 } from "./downloader.js";
import { saveAndCommitBatch, gitCreateBranch } from "./saver.js";
import { chunks, findImages, sha1File } from "./utils.js";
import type {
  ScraperOptions,
  PostMetadata,
  ClassificationResult,
  DownloadedImage,
} from "./models.js";

const logger = {
  info: (...args: unknown[]) => console.log("[pipeline]", ...args),
  warning: (...args: unknown[]) => console.warn("[pipeline]", ...args),
  error: (...args: unknown[]) => console.error("[pipeline]", ...args),
};

interface PipelineContext {
  tracker: PostTracker;
  repoPath: string;
  tmpDir: string;
}

// ---- Pipeline stages ----

function buildTracker(resetBloom: boolean = false): PostTracker {
  const tracker = new PostTracker();
  if (resetBloom) {
    logger.info("Resetting Bloom filter");
    // Bloom filter starts fresh
  }
  return tracker;
}

function indexExistingMemes(
  tracker: PostTracker,
  repoPath: string,
  force: boolean = false
): void {
  const meta = tracker.metadata;
  if (!force && meta["sha1_indexed"]) return;

  const memesDir = resolve(repoPath, "memes");
  if (!require("fs").existsSync(memesDir)) {
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    (tracker as any).bloom.metadata = { ...meta, sha1_indexed: true };
    tracker.flush();
    return;
  }

  let indexed = 0;
  for (const imgPath of findImages(memesDir)) {
    try {
      const sha1 = sha1File(imgPath);
      tracker.markContentProcessed(sha1);
      indexed++;
    } catch {
      // skip
    }
  }

  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  (tracker as any).bloom.metadata = { ...meta, sha1_indexed: true };
  tracker.flush();
  logger.info(`Content-indexed ${indexed} existing memes`);
}

function buildMeta(post: RedditPost, subreddit: string): PostMetadata {
  return {
    post_id: String(post["name"] || ""),
    title: String(post["title"] || ""),
    author: String(post["author"] || "[deleted]"),
    subreddit: String(post["subreddit"] || subreddit),
    score: (post["ups"] as number) || (post["score"] as number) || 0,
    created_utc: (post["created_utc"] as number) || 0,
    permalink: String(post["permalink"] || ""),
  };
}

// ---- URL extraction ----

interface UrlWithMeta {
  url: string;
  meta: PostMetadata;
}

async function extractUrls(
  posts: RedditPost[],
  tracker: PostTracker,
  subreddit: string,
  minCommentUpvotes: number
): Promise<UrlWithMeta[]> {
  const seen = new Set<string>();
  const results: UrlWithMeta[] = [];

  for (const post of posts) {
    const postId = String(post["name"] || "");
    if (tracker.isProcessed(postId)) continue;

    const meta = buildMeta(post, subreddit);

    // Post-level images
    const postUrls = extractPostImageUrls(post);
    for (const url of postUrls) {
      if (!seen.has(url) && !tracker.isProcessed(url)) {
        seen.add(url);
        results.push({ url, meta });
      }
    }

    // Comment images
    try {
      const commentImages = await fetchCommentImages(
        post,
        minCommentUpvotes
      );
      for (const { url, score } of commentImages) {
        if (!seen.has(url) && !tracker.isProcessed(url)) {
          seen.add(url);
          results.push({
            url,
            meta: { ...meta, score },
          });
        }
      }
    } catch (err) {
      logger.warning(`Error fetching comments for ${postId}: ${err}`);
    }

    tracker.markProcessed(postId);
  }

  return results;
}

// ---- Dedup download ----

interface DedupResult {
  clean: DownloadedImage[];
  failed: Set<string>;
}

async function dedupDownload(
  urls: string[],
  tracker: PostTracker,
  tmpDir: string,
  skipContentDedup: boolean = false
): Promise<DedupResult> {
  const downloaded = await downloadBatch(urls, tmpDir, {
    isProcessed: (url) => tracker.isProcessed(url),
  });

  const downloadedUrls = new Set(downloaded.map((d) => d.url));
  const clean: DownloadedImage[] = [];

  for (const item of downloaded) {
    if (!skipContentDedup && tracker.isContentProcessed(item.sha1)) {
      logger.info(`Skipping content duplicate (sha1=${item.sha1.slice(0, 8)}...): ${item.url}`);
      tracker.markProcessed(item.url);
      try {
        rmSync(item.path);
      } catch {
        // ignore
      }
    } else {
      clean.push(item);
    }
  }

  const failed = new Set(urls.filter((u) => !downloadedUrls.has(u)));
  return { clean, failed };
}

// ---- Classification ----

interface ClassifyResult {
  memes: Array<{ result: ClassificationResult; path: string; sha1: string }>;
  errorCount: number;
  shouldAbort: boolean;
}

async function classifyAndPartition(
  cleanDownloaded: DownloadedImage[],
  classifierName: string,
  classifyWorkers: number,
  tracker: PostTracker,
  locale: string
): Promise<ClassifyResult> {
  const classifier = createClassifier(classifierName);
  const items = cleanDownloaded.map((d) => ({ url: d.url, path: d.path }));

  const results = await classifier.classifyBatch(items, {
    maxWorkers: classifyWorkers,
    locale,
  });

  const urlToSha1 = new Map(cleanDownloaded.map((d) => [d.url, d.sha1]));
  const urlToPath = new Map(cleanDownloaded.map((d) => [d.url, d.path]));

  const memes: Array<{ result: ClassificationResult; path: string; sha1: string }> = [];
  let errorCount = 0;

  for (const result of results) {
    if (result.error) {
      errorCount++;
      logger.warning(`Skipping bloom indexing for ${result.url} (will retry): ${result.error}`);
    } else if (result.is_meme) {
      const path = urlToPath.get(result.url);
      const sha1 = urlToSha1.get(result.url);
      if (path && sha1) {
        memes.push({ result, path, sha1 });
      }
    } else {
      tracker.markProcessed(result.url);
    }
  }

  tracker.flush();

  const shouldAbort =
    errorCount > 0 &&
    errorCount === results.length &&
    results.some((r) => r.error === "claude_not_found");

  return { memes, errorCount, shouldAbort };
}

// ---- Commit ----

async function commitBatch(
  memeItems: Array<{ result: ClassificationResult; path: string; sha1: string }>,
  tracker: PostTracker,
  repoPath: string,
  batchNum: number,
  subreddit: string,
  urlToMeta: Map<string, PostMetadata>,
  locale: string
): Promise<number> {
  const items = memeItems.map(({ result, path }) => ({
    result,
    tmpPath: path,
  }));

  const urlToMetaRecord: Record<string, PostMetadata> = {};
  for (const [url, meta] of urlToMeta) {
    urlToMetaRecord[url] = meta;
  }

  const saved = saveAndCommitBatch(
    items,
    repoPath,
    batchNum,
    subreddit,
    urlToMetaRecord,
    locale
  );

  for (const { result, sha1 } of memeItems) {
    tracker.markProcessed(result.url);
    tracker.markContentProcessed(sha1);
  }
  tracker.flush();

  return saved.length;
}

// ---- Main pipeline ----

export async function runPipeline(options: Partial<ScraperOptions> = {}): Promise<number> {
  const opts = {
    subreddit: options.subreddit || CONFIG.SUBREDDIT,
    limit: options.limit || 100,
    batchSize: options.batchSize || 10,
    dryRun: options.dryRun || false,
    perPost: options.perPost || false,
    fromFile: options.fromFile || "",
    postUrl: options.postUrl || "",
    minCommentUpvotes: options.minCommentUpvotes ?? CONFIG.MIN_COMMENT_UPVOTES,
    sort: (options.sort as "hot" | "new" | "top") || "hot",
    timeframe: (options.timeframe as "hour" | "day" | "week" | "month" | "year" | "all") || "day",
    page: options.page || 1,
    classifyWorkers: options.classifyWorkers || CONFIG.CLASSIFY_WORKERS,
    classifier: options.classifier || CONFIG.CLASSIFIER,
    locale: options.locale || "en",
    skipContentDedup: options.skipContentDedup || false,
    noBranch: options.noBranch || false,
    resetBloom: options.resetBloom || false,
  };

  const repoPath = resolve(CONFIG.REPO_PATH);
  const tmpDir = resolve(CONFIG.TMP_DIR);
  mkdirSync(tmpDir, { recursive: true });

  // Setup
  const tracker = buildTracker(opts.resetBloom);
  indexExistingMemes(tracker, repoPath, opts.resetBloom);

  // Fetch posts
  logger.info(
    `Starting pipeline: r/${opts.subreddit} limit=${opts.limit} sort=${opts.sort} timeframe=${opts.timeframe} page=${opts.page} batch=${opts.batchSize} dryRun=${opts.dryRun}`
  );

  let posts: RedditPost[];
  if (opts.postUrl) {
    posts = await fetchSinglePost(opts.postUrl);
  } else {
    posts = await fetchPosts(
      opts.subreddit,
      opts.limit,
      opts.sort,
      opts.timeframe,
      opts.page
    );
  }

  // Create branch
  if (!opts.noBranch && !opts.dryRun) {
    const now = new Date();
    const branchName = `memes/${opts.subreddit}-${now.toISOString().slice(0, 10).replace(/-/g, "")}-${String(now.getHours()).padStart(2, "0")}${String(now.getMinutes()).padStart(2, "0")}${String(now.getSeconds()).padStart(2, "0")}`;
    if (!gitCreateBranch(repoPath, branchName)) {
      logger.error(`Aborting: could not create branch ${branchName}`);
      return 0;
    }
  }

  // Extract URLs
  const urlWithMeta = await extractUrls(
    posts,
    tracker,
    opts.subreddit,
    opts.minCommentUpvotes
  );

  logger.info(`Found ${urlWithMeta.length} new image URLs to process`);

  if (urlWithMeta.length === 0) {
    tracker.flush();
    logger.info("Nothing new to process.");
    return 0;
  }

  // Process in batches
  const urls = urlWithMeta.map((u) => u.url);
  const urlToMeta = new Map(urlWithMeta.map((u) => [u.url, u.meta]));
  let totalMemes = 0;
  let batchNum = 0;

  for (const batchUrls of chunks(urls, opts.batchSize)) {
    batchNum++;
    logger.info(`--- Batch ${batchNum}: ${batchUrls.length} URLs ---`);

    const { clean, failed } = await dedupDownload(
      batchUrls,
      tracker,
      tmpDir,
      opts.skipContentDedup
    );
    for (const url of failed) tracker.markProcessed(url);
    tracker.flush();

    if (clean.length === 0) continue;

    const { memes, errorCount, shouldAbort } = await classifyAndPartition(
      clean,
      opts.classifier,
      opts.classifyWorkers,
      tracker,
      opts.locale
    );

    if (shouldAbort) {
      logger.error(
        "'claude' CLI not found — aborting pipeline. Install Claude Code and ensure it is in PATH."
      );
      return totalMemes;
    }

    if (errorCount === clean.length) {
      logger.warning(
        `All ${errorCount} items in batch ${batchNum} failed classification — they will be retried next run.`
      );
    }

    if (memes.length > 0 && !opts.dryRun) {
      totalMemes += await commitBatch(
        memes,
        tracker,
        repoPath,
        batchNum,
        opts.subreddit,
        urlToMeta,
        opts.locale
      );
    } else if (memes.length > 0 && opts.dryRun) {
      for (const { result } of memes) {
        logger.info(`[DRY RUN] Would save: ${result.category}/${result.filename_slug} (${result.url})`);
      }
    }

    // Cleanup
    for (const { path } of clean) {
      try {
        rmSync(path);
      } catch {
        // ignore
      }
    }
  }

  logger.info(`Pipeline complete. Total memes saved: ${totalMemes}`);
  return totalMemes;
}
