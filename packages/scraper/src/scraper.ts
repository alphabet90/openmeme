/**
 * Reddit scraper module.
 * Ported from reddit.memes/src/scraper.py
 */

import { CONFIG } from "./config.js";
import { extractImageUrls } from "./utils.js";

const logger = {
  info: (...args: unknown[]) => console.log("[scraper]", ...args),
  warning: (...args: unknown[]) => console.warn("[scraper]", ...args),
  error: (...args: unknown[]) => console.error("[scraper]", ...args),
  debug: (...args: unknown[]) => {
    if (process.env.DEBUG) console.log("[scraper:debug]", ...args);
  },
};

interface RedditPost {
  name: string;
  id: string;
  title: string;
  author: string;
  subreddit: string;
  score: number;
  ups?: number;
  created_utc: number;
  permalink: string;
  url?: string;
  media_metadata?: Record<string, unknown>;
  preview?: {
    images?: Array<{
      source?: { url?: string };
      resolutions?: Array<{ url?: string }>;
    }>;
  };
  is_gallery?: boolean;
  crosspost_parent_list?: Array<{ url?: string; permalink?: string }>;
}

interface RedditListing {
  kind: string;
  data: {
    children: Array<{ kind: string; data: RedditPost }>;
    after?: string | null;
  };
}

function sleep(ms: number): Promise<void> {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

async function _get(
  url: string,
  params?: Record<string, string | number | undefined>
): Promise<unknown> {
  await sleep(CONFIG.REQUEST_DELAY_MS);
  const queryParams = new URLSearchParams();
  if (params) {
    for (const [k, v] of Object.entries(params)) {
      if (v !== undefined && v !== null) queryParams.set(k, String(v));
    }
  }
  const fullUrl = queryParams.toString() ? `${url}?${queryParams}` : url;

  for (let attempt = 0; attempt < 3; attempt++) {
    try {
      const resp = await fetch(fullUrl, {
        headers: {
          "User-Agent": CONFIG.REDDIT_USER_AGENT,
          Accept: "application/json",
        },
        signal: AbortSignal.timeout(CONFIG.API_TIMEOUT_MS),
      });

      if (resp.status === 429) {
        const waitMs = CONFIG.RATE_LIMIT_BASE_WAIT_S * 1000 * 2 ** attempt;
        logger.warning(`Rate limited, waiting ${waitMs}ms`);
        await sleep(waitMs);
        continue;
      }
      if (resp.status === 403) {
        throw new Error(
          `Reddit returned 403 Forbidden for ${url}. ` +
            "Reddit may be blocking this environment. " +
            "Try running from a normal internet connection."
        );
      }
      if (resp.status >= 500) {
        await sleep(2000 * (attempt + 1));
        continue;
      }
      if (!resp.ok) {
        throw new Error(`HTTP ${resp.status}: ${resp.statusText}`);
      }
      return await resp.json();
    } catch (err) {
      if (err instanceof Error && err.name === "TimeoutError") {
        await sleep(2000 * (attempt + 1));
        continue;
      }
      if (attempt === 2) throw err;
    }
  }
  throw new Error(`Failed to fetch ${url} after 3 attempts`);
}

function _toOldReddit(url: string): string {
  const parsed = new URL(url);
  parsed.hostname = "old.reddit.com";
  parsed.protocol = "https:";
  return parsed.toString();
}

export async function resolveShareUrl(url: string): Promise<string> {
  const attemptUrl = _toOldReddit(url);
  try {
    const resp = await fetch(attemptUrl, {
      headers: { "User-Agent": CONFIG.REDDIT_USER_AGENT },
      redirect: "follow",
      signal: AbortSignal.timeout(CONFIG.API_TIMEOUT_MS),
    });
    const finalUrl = _toOldReddit(resp.url);
    if (finalUrl.includes("/comments/")) {
      logger.info(`Resolved share link to: ${finalUrl}`);
      return finalUrl;
    }
  } catch (err) {
    logger.warning(`Could not resolve ${attemptUrl}: ${err}`);
  }
  throw new Error(`Could not resolve share URL to a post URL: ${url}`);
}

export async function fetchSinglePost(url: string): Promise<RedditPost[]> {
  let resolvedUrl = url;
  if (new URL(url).pathname.includes("/s/")) {
    logger.info(`Resolving Reddit share link: ${url}`);
    resolvedUrl = await resolveShareUrl(url);
  }

  const parsed = new URL(resolvedUrl);
  if (!parsed.pathname.includes("/comments/")) {
    throw new Error(
      `URL does not look like a Reddit post (missing /comments/): ${url}`
    );
  }

  parsed.hostname = "old.reddit.com";
  parsed.protocol = "https:";
  parsed.search = "";
  parsed.hash = "";

  let canonical = parsed.toString();
  if (!canonical.endsWith(".json")) {
    canonical = canonical.replace(/\/$/, "") + "/.json";
  }

  logger.info(`Fetching single post: ${canonical}`);
  const data = (await _get(canonical)) as [RedditListing, RedditListing];

  const listing = Array.isArray(data) ? data[0] : data;
  const children = listing.data?.children || [];
  const posts = children
    .filter((c) => c.kind === "t3")
    .map((c) => c.data);

  if (posts.length === 0) {
    throw new Error(`No post data found at ${canonical}`);
  }

  logger.info(`Fetched post: ${posts[0].title?.slice(0, 80) || "untitled"}`);
  return posts;
}

function _buildFeedUrl(subreddit: string, sort: string): string {
  if (sort === "new") {
    return `${CONFIG.REDDIT_BASE_URL}/r/${subreddit}/new/.json`;
  }
  if (sort === "top") {
    return `${CONFIG.REDDIT_BASE_URL}/r/${subreddit}/top/.json`;
  }
  return `${CONFIG.REDDIT_BASE_URL}/r/${subreddit}/.json`;
}

async function _navigateToPage(
  url: string,
  sort: string,
  timeframe: string,
  targetPage: number
): Promise<{ after: string | null; count: number }> {
  if (targetPage === 1) return { after: null, count: 0 };

  let after: string | null = null;
  let count = 0;

  for (let p = 1; p < targetPage; p++) {
    const params: Record<string, string | number> = { limit: 25 };
    if (after) params.after = after;
    if (count) params.count = count;
    if (sort === "top") {
      params.sort = "top";
      params.t = timeframe;
    }

    logger.info(`Navigating to page ${p}/${targetPage - 1} (after=${after})`);
    const data = (await _get(url, params)) as RedditListing;
    const children = data.data?.children || [];

    if (children.length === 0) {
      throw new Error(
        `Subreddit has fewer than ${targetPage} pages; stopped at page ${p}`
      );
    }
    after = data.data?.after ?? null;
    if (!after) {
      throw new Error(
        `Subreddit has fewer than ${targetPage} pages; no cursor after page ${p}`
      );
    }
    count += children.length;
  }
  return { after, count };
}

export async function fetchPosts(
  subreddit: string,
  limit: number = 100,
  sort: "hot" | "new" | "top" = "hot",
  timeframe: "hour" | "day" | "week" | "month" | "year" | "all" = "day",
  page: number = 1
): Promise<RedditPost[]> {
  if (page < 1) throw new Error(`page must be >= 1, got ${page}`);

  const url = _buildFeedUrl(subreddit, sort);
  const posts: RedditPost[] = [];
  let after: string | null = null;
  let count = 0;

  if (page > 1) {
    try {
      const result = await _navigateToPage(url, sort, timeframe, page);
      after = result.after;
      count = result.count;
    } catch (err) {
      logger.warning(`Cannot reach page ${page}: ${err}`);
      return [];
    }
  }

  while (posts.length < limit) {
    const pageSize = Math.min(100, limit - posts.length);
    const params: Record<string, string | number> = { limit: pageSize };
    if (after) params.after = after;
    if (count) params.count = count;
    if (sort === "top") {
      params.sort = "top";
      params.t = timeframe;
    }

    logger.info(
      `Fetching r/${subreddit} [sort=${sort} page=${page}] (after=${after}, collected=${posts.length}/${limit})`
    );

    let data: RedditListing;
    try {
      data = (await _get(url, params)) as RedditListing;
    } catch (err) {
      logger.error(`Stopping pagination: ${err}`);
      break;
    }

    const children = data.data?.children || [];
    if (children.length === 0) break;

    for (const child of children) {
      if (child.kind !== "t3") continue;
      posts.push(child.data);
    }

    count += children.length;
    after = data.data?.after ?? null;
    if (!after) {
      if (posts.length < limit) {
        logger.info(`Feed exhausted after ${posts.length} posts (requested ${limit})`);
      }
      break;
    }
  }

  logger.info(`Fetched ${posts.length} posts from r/${subreddit}`);
  return posts;
}

// ---- comment images ----

function _flattenComments(children: Array<{ kind?: string; data?: RedditPost & { replies?: unknown } }>): RedditPost[] {
  const result: RedditPost[] = [];
  for (const child of children) {
    if (child.kind !== "t1") continue;
    const data = child.data as RedditPost & { replies?: unknown };
    if (data) result.push(data);
    const replies = data?.replies;
    if (replies && typeof replies === "object" && !Array.isArray(replies)) {
      const replyChildren = (replies as RedditListing).data?.children;
      if (replyChildren) {
        result.push(..._flattenComments(replyChildren as Array<{ kind?: string; data?: RedditPost & { replies?: unknown } }>));
      }
    }
  }
  return result;
}

export async function fetchCommentImages(
  post: RedditPost,
  minUpvotes: number
): Promise<Array<{ url: string; score: number }>> {
  const subreddit = post.subreddit;
  const postId = post.id;
  const url = `${CONFIG.REDDIT_BASE_URL}/r/${subreddit}/comments/${postId}/.json`;

  try {
    const data = (await _get(url)) as [RedditListing, RedditListing];
    const children = data[1]?.data?.children || [];
  } catch (err) {
    logger.warning(`Could not fetch comments for post ${postId}: ${err}`);
    return [];
  }

  // Re-fetch since we need children
  let children: Array<{ kind?: string; data?: RedditPost & { replies?: unknown } }> = [];
  try {
    const data = (await _get(url)) as [RedditListing, RedditListing];
    children = data[1]?.data?.children || [];
  } catch {
    return [];
  }

  const comments = _flattenComments(children);
  const seen = new Map<string, number>();

  for (const comment of comments) {
    const commentScore = comment.ups || 0;
    if (commentScore >= minUpvotes) {
      const urls = extractImageUrlsFromComment(comment);
      for (const imgUrl of urls) {
        if (!seen.has(imgUrl)) {
          seen.set(imgUrl, commentScore);
        }
      }
    }
  }

  const result = Array.from(seen.entries()).map(([url, score]) => ({ url, score }));
  logger.info(
    `Post ${postId}: ${result.length} comment image(s) found with >= ${minUpvotes} upvotes`
  );
  return result;
}

function extractImageUrlsFromComment(comment: RedditPost): string[] {
  const urls: string[] = [];
  const mediaMetadata = comment.media_metadata || {};
  for (const item of Object.values(mediaMetadata)) {
    const mediaItem = item as { status?: string; e?: string; s?: { u?: string } };
    if (mediaItem.status !== "valid" || mediaItem.e !== "Image") continue;
    const u = mediaItem.s?.u || "";
    if (!u) continue;
    const cleaned = _cleanUrl(u);
    const parsed = new URL(cleaned);
    if (!CONFIG.IMAGE_HOSTS.has(parsed.hostname)) {
      logger.debug(`Skipping image (host not whitelisted): ${parsed.hostname}`);
      continue;
    }
    const extMatch = parsed.pathname.match(/\.([^.]+)$/);
    const ext = extMatch ? `.${extMatch[1].toLowerCase()}` : "";
    if (!ext || !CONFIG.SUPPORTED_EXTENSIONS.has(ext)) {
      logger.debug(`Skipping image (unsupported extension ${ext}): ${parsed.pathname}`);
      continue;
    }
    urls.push(cleaned);
  }
  return [...new Set(urls)];
}

function _cleanUrl(url: string): string {
  const decoded = url.replace(/&amp;/g, "&");
  const parsed = new URL(decoded);
  if (parsed.hostname === "preview.redd.it" || parsed.hostname === "external-preview.redd.it") {
    parsed.hash = "";
    return parsed.toString();
  }
  parsed.search = "";
  parsed.hash = "";
  return parsed.toString();
}

export function extractPostImageUrls(post: RedditPost): string[] {
  const urls: string[] = [];

  // Direct image URL
  if (post.url) {
    const parsed = new URL(post.url);
    if (CONFIG.IMAGE_HOSTS.has(parsed.hostname)) {
      const extMatch = parsed.pathname.match(/\.([^.]+)$/);
      if (extMatch && CONFIG.SUPPORTED_EXTENSIONS.has(`.${extMatch[1].toLowerCase()}`)) {
        urls.push(post.url);
      }
    }
  }

  // Preview images
  if (post.preview?.images) {
    for (const img of post.preview.images) {
      if (img.source?.url) {
        urls.push(_cleanUrl(img.source.url));
      }
    }
  }

  // Gallery
  if (post.is_gallery && post.media_metadata) {
    for (const item of Object.values(post.media_metadata)) {
      const mediaItem = item as { s?: { u?: string } };
      if (mediaItem.s?.u) {
        urls.push(_cleanUrl(mediaItem.s.u));
      }
    }
  }

  // Crossposts
  if (post.crosspost_parent_list) {
    for (const parent of post.crosspost_parent_list) {
      if (parent.url) urls.push(parent.url);
    }
  }

  return [...new Set(urls)];
}
