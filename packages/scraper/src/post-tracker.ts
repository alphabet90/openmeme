/**
 * Bloom-filter-backed tracker for processed Reddit post IDs and image URLs.
 * Ported from reddit.memes/src/post_tracker.py
 */

import { readFileSync, existsSync } from "fs";
import { BloomFilter, BloomFilterError } from "./bloom.js";
import { CONFIG } from "./config.js";
import { mkdirSync } from "fs";
import { dirname } from "path";

const logger = {
  info: (...args: unknown[]) => console.log("[tracker]", ...args),
  warning: (...args: unknown[]) => console.warn("[tracker]", ...args),
};

export class PostTracker {
  private path: string;
  private capacity: number;
  private errorRate: number;
  private bloom: BloomFilter;

  constructor(
    path: string = CONFIG.BLOOM_FILTER_FILE,
    capacity: number = CONFIG.BLOOM_CAPACITY,
    errorRate: number = CONFIG.BLOOM_ERROR_RATE
  ) {
    this.path = path;
    this.capacity = capacity;
    this.errorRate = errorRate;
    this.bloom = this._loadOrCreate();
  }

  private _loadOrCreate(): BloomFilter {
    if (existsSync(this.path)) {
      try {
        const bloom = BloomFilter.load(this.path);
        logger.info(
          `Loaded Bloom filter from ${this.path} (${bloom.length} items, m=${bloom.capacity_bits} bits, k=${bloom.hash_count})`
        );
        return bloom;
      } catch (err) {
        const e = err instanceof Error ? err.message : String(err);
        logger.warning(`Bloom filter at ${this.path} is corrupt (${e}) — starting fresh`);
      }
    }
    return new BloomFilter({ capacity: this.capacity, errorRate: this.errorRate });
  }

  // ---- membership ----

  isProcessed(key: string): boolean {
    return this.bloom.has(key);
  }

  markProcessed(key: string): void {
    this.bloom.add(key);
  }

  get length(): number {
    return this.bloom.length;
  }

  // ---- persistence ----

  flush(): void {
    const dir = dirname(this.path);
    mkdirSync(dir, { recursive: true });
    this.bloom.save(this.path);
  }

  // ---- content hashing ----

  private static SHA1_PREFIX = "sha1:";

  isContentProcessed(sha1Hex: string): boolean {
    return this.isProcessed(`${PostTracker.SHA1_PREFIX}${sha1Hex}`);
  }

  markContentProcessed(sha1Hex: string): void {
    this.markProcessed(`${PostTracker.SHA1_PREFIX}${sha1Hex}`);
  }

  get metadata(): Record<string, unknown> {
    return this.bloom.metadata_dict;
  }

  // ---- migration ----

  migrateFromStateJson(stateFile: string): number {
    if (!existsSync(stateFile)) return 0;
    let data: { processed_post_ids?: Record<string, unknown>; processed_urls?: Record<string, unknown> };
    try {
      data = JSON.parse(readFileSync(stateFile, "utf8"));
    } catch (err) {
      const e = err instanceof Error ? err.message : String(err);
      logger.warning(`Could not read legacy state file ${stateFile}: ${e}`);
      return 0;
    }

    let imported = 0;
    for (const postId of Object.keys(data.processed_post_ids || {})) {
      this.bloom.add(postId);
      imported++;
    }
    for (const url of Object.keys(data.processed_urls || {})) {
      this.bloom.add(url);
      imported++;
    }

    logger.info(`Migrated ${imported} items from ${stateFile} into Bloom filter`);
    return imported;
  }
}
