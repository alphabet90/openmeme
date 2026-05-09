/**
 * Core domain models for the meme scraper pipeline.
 * Mirrors the Python dataclasses from reddit.memes/src/models.py
 */

export interface PostMetadata {
  post_id: string;
  title: string;
  author: string;
  subreddit: string;
  score: number;
  created_utc: number;
  permalink: string;
}

export interface ClassificationResult {
  url: string;
  is_meme: boolean;
  category: string;
  filename_slug: string;
  title: string;
  description: string;
  tags: string[];
  error?: string;
}

export interface DownloadedImage {
  url: string;
  path: string;
  sha1: string;
}

export interface MemeItem {
  result: ClassificationResult;
  imagePath: string;
  sha1: string;
}

export interface ScraperOptions {
  subreddit?: string;
  limit?: number;
  batchSize?: number;
  dryRun?: boolean;
  perPost?: boolean;
  fromFile?: string;
  postUrl?: string;
  minCommentUpvotes?: number;
  sort?: "hot" | "new" | "top";
  timeframe?: "hour" | "day" | "week" | "month" | "year" | "all";
  page?: number;
  classifyWorkers?: number;
  classifier?: string;
  locale?: string;
  skipContentDedup?: boolean;
  noBranch?: boolean;
  resetBloom?: boolean;
}

export const DEFAULT_OPTIONS: Required<ScraperOptions> = {
  subreddit: "argentina",
  limit: 100,
  batchSize: 10,
  dryRun: false,
  perPost: false,
  fromFile: "",
  postUrl: "",
  minCommentUpvotes: 0,
  sort: "hot",
  timeframe: "day",
  page: 1,
  classifyWorkers: 4,
  classifier: "claude",
  locale: "en",
  skipContentDedup: false,
  noBranch: false,
  resetBloom: false,
};
