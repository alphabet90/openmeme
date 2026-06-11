/**
 * @openmeme/scraper - Reddit meme scraper pipeline
 *
 * Migrated from reddit.memes to the OpenMeme monorepo.
 * Provides a modular pipeline for scraping Reddit posts,
 * classifying images as memes, and saving them to the repository.
 */

// Core modules
export { CONFIG } from "./config.js";
export type { Config } from "./config.js";

export {
  PostTracker,
} from "./post-tracker.js";

export {
  BloomFilter,
  BloomFilterError,
} from "./bloom.js";

export {
  fetchPosts,
  fetchSinglePost,
  fetchCommentImages,
  extractPostImageUrls,
  resolveShareUrl,
} from "./scraper.js";

export {
  downloadBatch,
  computeFileSha1,
  computeBufferSha1,
} from "./downloader.js";

export {
  createClassifier,
  loadPrompt,
  ClaudeClassifier,
  CodexClassifier,
} from "./classifier.js";

export {
  saveMeme,
  writeMdx,
  saveAndCommitBatch,
  gitCreateBranch,
  gitAdd,
  gitCommit,
  sanitizeSlug,
} from "./saver.js";

export { runPipeline } from "./pipeline.js";

export {
  sha1File,
  sha1String,
  findImages,
  humanSize,
  sleep,
  chunks,
  sanitizeFilename,
  isValidImage,
  normalizeLocale,
} from "./utils.js";

// Validation
export {
  validateMemeFile,
  validateMeme,
  validateMemes,
  VALIDATION_RULES,
} from "./validator.js";

// Models
export type {
  PostMetadata,
  ClassificationResult,
  DownloadedImage,
  MemeItem,
  ScraperOptions,
} from "./models.js";
export { DEFAULT_OPTIONS } from "./models.js";

// Re-export types from classifier
export type { BaseClassifier } from "./classifier.js";
