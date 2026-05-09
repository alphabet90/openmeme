/**
 * Image downloader with SHA1 deduplication.
 * Ported from reddit.memes/src/downloader.py
 */

import { createHash } from "crypto";
import { writeFileSync, readFileSync, mkdirSync } from "fs";
import { join, extname } from "path";
import { CONFIG } from "./config.js";
import type { DownloadedImage } from "./models.js";
import PQueue from "p-queue";

const logger = {
  info: (...args: unknown[]) => console.log("[downloader]", ...args),
  warning: (...args: unknown[]) => console.warn("[downloader]", ...args),
  error: (...args: unknown[]) => console.error("[downloader]", ...args),
};

export function computeBufferSha1(buffer: Buffer): string {
  return createHash("sha1").update(buffer).digest("hex");
}

export function computeFileSha1(filePath: string): string {
  const data = readFileSync(filePath);
  return createHash("sha1").update(data).digest("hex");
}

async function downloadSingle(
  url: string,
  tmpDir: string,
  isProcessed?: (url: string) => boolean
): Promise<{ url: string; path: string } | null> {
  if (isProcessed?.(url)) {
    logger.info(`Skipping already processed URL: ${url}`);
    return null;
  }

  try {
    const resp = await fetch(url, {
      headers: { "User-Agent": CONFIG.REDDIT_USER_AGENT },
      signal: AbortSignal.timeout(CONFIG.API_TIMEOUT_MS),
    });

    if (!resp.ok) {
      logger.warning(`Failed to download ${url}: HTTP ${resp.status}`);
      return null;
    }

    const contentLength = resp.headers.get("content-length");
    if (contentLength && parseInt(contentLength) > CONFIG.MAX_IMAGE_SIZE_BYTES) {
      logger.warning(`Image too large (${contentLength} bytes): ${url}`);
      return null;
    }

    const contentType = resp.headers.get("content-type") || "";
    if (!contentType.startsWith("image/")) {
      logger.warning(`Not an image (${contentType}): ${url}`);
      return null;
    }

    const buffer = Buffer.from(await resp.arrayBuffer());
    if (buffer.length > CONFIG.MAX_IMAGE_SIZE_BYTES) {
      logger.warning(`Image too large (${buffer.length} bytes): ${url}`);
      return null;
    }

    const sha1 = computeBufferSha1(buffer);
    const parsed = new URL(url);
    const origExt = extname(parsed.pathname) || ".jpg";
    const filename = `${sha1}${origExt}`;
    const filePath = join(tmpDir, filename);

    writeFileSync(filePath, buffer);
    return { url, path: filePath };
  } catch (err) {
    logger.warning(`Download failed for ${url}: ${err}`);
    return null;
  }
}

export async function downloadBatch(
  urls: string[],
  tmpDir: string,
  options?: {
    isProcessed?: (url: string) => boolean;
    maxWorkers?: number;
  }
): Promise<DownloadedImage[]> {
  mkdirSync(tmpDir, { recursive: true });
  const maxWorkers = options?.maxWorkers || CONFIG.DOWNLOAD_WORKERS;

  const queue = new PQueue({ concurrency: maxWorkers });
  const results: DownloadedImage[] = [];

  const tasks = urls.map((url) =>
    queue.add(async () => {
      const result = await downloadSingle(url, tmpDir, options?.isProcessed);
      if (result) {
        const sha1 = computeFileSha1(result.path);
        results.push({ url: result.url, path: result.path, sha1 });
      }
    })
  );

  await Promise.all(tasks);
  return results;
}
