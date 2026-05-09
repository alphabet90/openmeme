/**
 * Shared utilities for the scraper package.
 */

import { existsSync, statSync, readdirSync } from "fs";
import { join, extname } from "path";
import { createHash } from "crypto";
import { readFileSync } from "fs";

/**
 * Compute SHA1 hash of a file's contents.
 */
export function sha1File(filePath: string): string {
  const data = readFileSync(filePath);
  return createHash("sha1").update(data).digest("hex");
}

/**
 * Compute SHA1 hash of a string.
 */
export function sha1String(input: string): string {
  return createHash("sha1").update(input).digest("hex");
}

/**
 * Recursively find image files in a directory.
 */
export function findImages(dir: string): string[] {
  if (!existsSync(dir)) return [];
  const results: string[] = [];
  const exts = new Set([".jpg", ".jpeg", ".png", ".gif", ".webp"]);

  function walk(current: string) {
    for (const entry of readdirSync(current, { withFileTypes: true })) {
      const full = join(current, entry.name);
      if (entry.isDirectory()) {
        walk(full);
      } else if (entry.isFile() && exts.has(extname(entry.name).toLowerCase())) {
        results.push(full);
      }
    }
  }

  walk(dir);
  return results;
}

/**
 * Get file size in human-readable format.
 */
export function humanSize(bytes: number): string {
  const units = ["B", "KB", "MB", "GB"];
  let size = bytes;
  let unitIndex = 0;
  while (size >= 1024 && unitIndex < units.length - 1) {
    size /= 1024;
    unitIndex++;
  }
  return `${size.toFixed(1)} ${units[unitIndex]}`;
}

/**
 * Sleep for a given number of milliseconds.
 */
export function sleep(ms: number): Promise<void> {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

/**
 * Chunk an array into smaller arrays.
 */
export function* chunks<T>(arr: T[], n: number): Generator<T[]> {
  for (let i = 0; i < arr.length; i += n) {
    yield arr.slice(i, i + n);
  }
}

/**
 * Sanitize a string for use in a filename.
 */
export function sanitizeFilename(name: string): string {
  return name
    .toLowerCase()
    .replace(/[^a-z0-9\-]/g, "-")
    .replace(/-{2,}/g, "-")
    .replace(/^-|-$/g, "")
    .slice(0, 80) || "meme";
}

/**
 * Check if an image file is valid (exists and non-empty).
 */
export function isValidImage(filePath: string): boolean {
  if (!existsSync(filePath)) return false;
  try {
    const stat = statSync(filePath);
    return stat.isFile() && stat.size > 0;
  } catch {
    return false;
  }
}

/**
 * Parse locale string to normalized form.
 */
export function normalizeLocale(locale: string): string {
  return locale.replace("_", "-").toLowerCase();
}
