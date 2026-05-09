/**
 * Bloom Filter implementation for tracking processed posts and image URLs.
 * Ported from Python to TypeScript for the OpenMeme monorepo.
 *
 * Uses a Counting Bloom Filter with 4-bit counters to support deletion.
 * Backed by a file on disk for persistence across runs.
 */

import { createHash } from "crypto";
import { readFileSync, writeFileSync, existsSync } from "fs";

export class BloomFilterError extends Error {
  constructor(message: string) {
    super(message);
    this.name = "BloomFilterError";
  }
}

interface BloomHeader {
  magic: string;
  version: number;
  capacity_bits: number;
  hash_count: number;
  item_count: number;
  metadata: Record<string, unknown>;
}

const MAGIC = "OBMB";
const VERSION = 1;

export class BloomFilter {
  private counters: Uint8Array; // 4-bit counters packed into bytes
  private capacityBits: number;
  private hashCount: number;
  private itemCount: number;
  private metadata: Record<string, unknown>;

  constructor(options?: {
    capacity?: number;
    errorRate?: number;
    capacityBits?: number;
    hashCount?: number;
  }) {
    if (options?.capacityBits && options?.hashCount) {
      this.capacityBits = options.capacityBits;
      this.hashCount = options.hashCount;
    } else {
      const capacity = options?.capacity || 200_000;
      const errorRate = options?.errorRate || 1e-3;
      this.capacityBits = Math.ceil(
        (-capacity * Math.log(errorRate)) / (Math.log(2) ** 2)
      );
      this.hashCount = Math.max(
        1,
        Math.round((this.capacityBits / capacity) * Math.log(2))
      );
    }
    // Each byte holds 2 x 4-bit counters
    this.counters = new Uint8Array(Math.ceil(this.capacityBits / 2));
    this.itemCount = 0;
    this.metadata = {};
  }

  get capacity_bits(): number {
    return this.capacityBits;
  }

  get hash_count(): number {
    return this.hashCount;
  }

  get metadata_dict(): Record<string, unknown> {
    return { ...this.metadata };
  }

  private _getIndices(key: string): number[] {
    const indices: number[] = [];
    const h1 = createHash("sha256").update(key).digest();
    const h2 = createHash("sha256").update(h1).digest();

    for (let i = 0; i < this.hashCount; i++) {
      const offset = (i * 4) % h1.length;
      const val = h1.readUInt32LE(offset) ^ h2.readUInt32LE(offset);
      indices.push(Math.abs(val) % this.capacityBits);
    }
    return indices;
  }

  private _getCounter(index: number): number {
    const byteIndex = Math.floor(index / 2);
    const isHighNibble = index % 2 === 1;
    const byte = this.counters[byteIndex];
    return isHighNibble ? (byte >> 4) & 0xf : byte & 0xf;
  }

  private _setCounter(index: number, value: number): void {
    const byteIndex = Math.floor(index / 2);
    const isHighNibble = index % 2 === 1;
    const clamped = Math.min(15, Math.max(0, value));
    if (isHighNibble) {
      this.counters[byteIndex] = (this.counters[byteIndex] & 0x0f) | (clamped << 4);
    } else {
      this.counters[byteIndex] = (this.counters[byteIndex] & 0xf0) | clamped;
    }
  }

  add(key: string): void {
    const indices = this._getIndices(key);
    for (const idx of indices) {
      const current = this._getCounter(idx);
      if (current < 15) {
        this._setCounter(idx, current + 1);
      }
    }
    this.itemCount++;
  }

  has(key: string): boolean {
    const indices = this._getIndices(key);
    for (const idx of indices) {
      if (this._getCounter(idx) === 0) {
        return false;
      }
    }
    return true;
  }

  delete(key: string): void {
    if (!this.has(key)) return;
    const indices = this._getIndices(key);
    for (const idx of indices) {
      const current = this._getCounter(idx);
      if (current > 0) {
        this._setCounter(idx, current - 1);
      }
    }
    this.itemCount = Math.max(0, this.itemCount - 1);
  }

  /** Serialize to a binary buffer */
  serialize(): Buffer {
    const metaStr = JSON.stringify(this.metadata);
    const metaLen = Buffer.byteLength(metaStr, "utf8");
    const headerSize = 24 + metaLen; // magic(4) + version(4) + capBits(4) + hashCount(4) + itemCount(4) + metaLen(4) + metaStr
    const totalSize = headerSize + this.counters.length;

    const buf = Buffer.allocUnsafe(totalSize);
    let offset = 0;

    buf.write(MAGIC, offset, 4, "ascii");
    offset += 4;
    buf.writeUInt32LE(VERSION, offset);
    offset += 4;
    buf.writeUInt32LE(this.capacityBits, offset);
    offset += 4;
    buf.writeUInt32LE(this.hashCount, offset);
    offset += 4;
    buf.writeUInt32LE(this.itemCount, offset);
    offset += 4;
    buf.writeUInt32LE(metaLen, offset);
    offset += 4;
    buf.write(metaStr, offset, metaLen, "utf8");
    offset += metaLen;

    Buffer.from(this.counters.buffer).copy(buf, offset);

    return buf;
  }

  /** Deserialize from a binary buffer */
  static deserialize(buffer: Buffer): BloomFilter {
    let offset = 0;

    const magic = buffer.toString("ascii", offset, offset + 4);
    offset += 4;
    if (magic !== MAGIC) {
      throw new BloomFilterError(`Invalid magic: ${magic}`);
    }

    const version = buffer.readUInt32LE(offset);
    offset += 4;
    if (version !== VERSION) {
      throw new BloomFilterError(`Unsupported version: ${version}`);
    }

    const capacityBits = buffer.readUInt32LE(offset);
    offset += 4;
    const hashCount = buffer.readUInt32LE(offset);
    offset += 4;
    const itemCount = buffer.readUInt32LE(offset);
    offset += 4;
    const metaLen = buffer.readUInt32LE(offset);
    offset += 4;

    const metadata = JSON.parse(buffer.toString("utf8", offset, offset + metaLen));
    offset += metaLen;

    const bloom = new BloomFilter({ capacityBits, hashCount });
    const counterData = buffer.slice(offset);
    bloom.counters = new Uint8Array(counterData);
    bloom.itemCount = itemCount;
    bloom.metadata = metadata;

    return bloom;
  }

  save(path: string): void {
    writeFileSync(path, this.serialize());
  }

  static load(path: string): BloomFilter {
    if (!existsSync(path)) {
      throw new BloomFilterError(`File not found: ${path}`);
    }
    const data = readFileSync(path);
    return BloomFilter.deserialize(data);
  }

  get length(): number {
    return this.itemCount;
  }
}
