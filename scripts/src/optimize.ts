#!/usr/bin/env node
/**
 * scripts/optimize.ts - Image Optimization
 *
 * Compresses images and ensures the repository doesn't grow uncontrollably.
 * Inspired by Open Design's asset optimization approach.
 *
 * Features:
 *   - Compress images with quality-aware settings
 *   - Convert to optimal formats (WebP, AVIF)
 *   - Remove metadata to reduce size
 *   - Batch processing with progress tracking
 *   - Git-aware: only optimize uncommitted images
 *   - Size reporting and budget enforcement
 *
 * Usage:
 *   npx tsx scripts/optimize.ts [options]
 *   npx tsx scripts/optimize.ts --all           # optimize all memes
 *   npx tsx scripts/optimize.ts --staged        # optimize only staged images
 *   npx tsx scripts/optimize.ts --path ./memes  # optimize specific path
 *   npx tsx scripts/optimize.ts --budget 100MB  # enforce size budget
 *   npx tsx scripts/optimize.ts --dry-run       # show what would be done
 */

import { execSync } from "child_process";
import { existsSync, statSync, readdirSync, writeFileSync } from "fs";
import { extname, resolve, relative, join, basename, dirname } from "path";
import { Command } from "commander";
import { humanSize, findImages } from "@openmeme/scraper";

// sharp is loaded dynamically to handle optional dependency
let sharpModule: typeof import("sharp") | null = null;
async function getSharp(): Promise<typeof import("sharp")> {
  if (!sharpModule) {
    sharpModule = await import("sharp");
  }
  return sharpModule;
}

const logger = {
  info: (...args: unknown[]) => console.log("[optimize]", ...args),
  warning: (...args: unknown[]) => console.warn("[optimize]", ...args),
  error: (...args: unknown[]) => console.error("[optimize]", ...args),
  success: (...args: unknown[]) => console.log("[optimize] ✅", ...args),
};

// ---- CLI ----

const program = new Command();
program
  .name("optimize")
  .description("Optimize meme images for the repository")
  .option("-a, --all", "Optimize all meme images", false)
  .option("-s, --staged", "Optimize only staged images", false)
  .option("-p, --path <path>", "Path to memes directory", "./memes")
  .option("-q, --quality <n>", "JPEG/WebP quality (1-100)", "85")
  .option("--webp", "Convert to WebP format", false)
  .option("--avif", "Convert to AVIF format", false)
  .option("--max-width <px>", "Maximum width in pixels", "1200")
  .option("--max-height <px>", "Maximum height in pixels", "1200")
  .option("--strip-metadata", "Remove EXIF/metadata", true)
  .option("--budget <size>", "Repository size budget (e.g. 100MB, 1GB)")
  .option("--dry-run", "Show what would be done without modifying files", false)
  .option("--threshold <bytes>", "Only optimize files larger than this", "102400") // 100KB
  .option("-v, --verbose", "Verbose output", false)
  .option("--report", "Generate optimization report", false)
  .parse();

const options = program.opts();

// ---- Types ----

interface OptimizationResult {
  file: string;
  originalSize: number;
  optimizedSize: number;
  savingsBytes: number;
  savingsPercent: number;
  format: string;
  dimensions?: { width: number; height: number };
}

interface OptimizationReport {
  timestamp: string;
  totalFiles: number;
  totalOriginalBytes: number;
  totalOptimizedBytes: number;
  totalSavingsBytes: number;
  totalSavingsPercent: number;
  results: OptimizationResult[];
  skipped: string[];
}

// ---- File Discovery ----

function getStagedImageFiles(): string[] {
  try {
    const output = execSync("git diff --cached --name-only --diff-filter=ACMR", {
      encoding: "utf8",
    });
    return output
      .split("\n")
      .map((f) => f.trim())
      .filter((f) => f.length > 0)
      .filter((f) => {
        const ext = extname(f).toLowerCase();
        return [".jpg", ".jpeg", ".png", ".gif", ".webp"].includes(ext);
      })
      .map((f) => resolve(f));
  } catch {
    return [];
  }
}

function parseBudget(budgetStr: string): number {
  const match = budgetStr.match(/^(\d+(?:\.\d+)?)\s*(MB|GB|KB|B)?$/i);
  if (!match) throw new Error(`Invalid budget format: ${budgetStr}`);
  const value = parseFloat(match[1]);
  const unit = (match[2] || "MB").toUpperCase();
  const multipliers: Record<string, number> = { B: 1, KB: 1024, MB: 1024 ** 2, GB: 1024 ** 3 };
  return value * (multipliers[unit] || 1024 ** 2);
}

// ---- Optimization ----

async function optimizeImage(
  filePath: string,
  opts: {
    quality: number;
    maxWidth: number;
    maxHeight: number;
    stripMetadata: boolean;
    webp: boolean;
    avif: boolean;
    dryRun: boolean;
  }
): Promise<OptimizationResult | null> {
  const sharp = await getSharp();
  const originalSize = statSync(filePath).size;

  try {
    let pipeline = sharp(filePath, {
      animated: extname(filePath).toLowerCase() === ".gif",
    });

    // Get metadata
    const metadata = await pipeline.metadata();
    const width = metadata.width || 0;
    const height = metadata.height || 0;

    // Resize if needed
    if (width > opts.maxWidth || height > opts.maxHeight) {
      pipeline = pipeline.resize(opts.maxWidth, opts.maxHeight, {
        fit: "inside",
        withoutEnlargement: true,
      });
    }

    // Strip metadata
    if (opts.stripMetadata) {
      pipeline = pipeline.withMetadata({});
    }

    // Determine output format
    let outputFormat = extname(filePath).toLowerCase().replace(".", "");
    if (opts.avif) {
      pipeline = pipeline.avif({ quality: opts.quality });
      outputFormat = "avif";
    } else if (opts.webp) {
      pipeline = pipeline.webp({ quality: opts.quality });
      outputFormat = "webp";
    } else {
      const ext = extname(filePath).toLowerCase();
      if (ext === ".png") {
        pipeline = pipeline.png({ quality: opts.quality, compressionLevel: 9 });
      } else {
        pipeline = pipeline.jpeg({ quality: opts.quality, mozjpeg: true });
        outputFormat = "jpg";
      }
    }

    if (opts.dryRun) {
      // Estimate compression (rough heuristic)
      const estimatedRatio = opts.avif ? 0.3 : opts.webp ? 0.5 : 0.7;
      const estimatedSize = Math.round(originalSize * estimatedRatio);
      return {
        file: relative(process.cwd(), filePath),
        originalSize,
        optimizedSize: estimatedSize,
        savingsBytes: originalSize - estimatedSize,
        savingsPercent: ((originalSize - estimatedSize) / originalSize) * 100,
        format: outputFormat,
        dimensions: { width, height },
      };
    }

    // Process and save
    const buffer = await pipeline.toBuffer();
    const optimizedSize = buffer.length;

    // Update file
    const ext = extname(filePath);
    let outputPath = filePath;
    if ((opts.webp || opts.avif) && ext !== `.${outputFormat}`) {
      outputPath = filePath.replace(/\.[^.]+$/, `.${outputFormat}`);
    }

    writeFileSync(outputPath, buffer);

    // Remove old file if format changed
    if (outputPath !== filePath && existsSync(filePath)) {
      require("fs").unlinkSync(filePath);
    }

    return {
      file: relative(process.cwd(), outputPath),
      originalSize,
      optimizedSize,
      savingsBytes: originalSize - optimizedSize,
      savingsPercent: ((originalSize - optimizedSize) / originalSize) * 100,
      format: outputFormat,
      dimensions: { width, height },
    };
  } catch (err) {
    logger.error(`Failed to optimize ${filePath}: ${err}`);
    return null;
  }
}

// ---- Main ----

async function main() {
  logger.info("OpenMeme Optimize - Image Optimization\n");

  // Check sharp availability
  try {
    await getSharp();
  } catch {
    logger.error(
      "Sharp is not installed. Run: npm install sharp"
    );
    process.exit(1);
  }

  // Get files
  let files: string[];
  if (options.staged) {
    files = getStagedImageFiles();
    logger.info(`Found ${files.length} staged image(s)\n`);
  } else if (options.all) {
    files = findImages(resolve(options.path));
    logger.info(`Found ${files.length} image(s) in ${options.path}\n`);
  } else {
    // Default: try staged first, then all
    try {
      execSync("git rev-parse --git-dir", { stdio: "ignore" });
      files = getStagedImageFiles();
      if (files.length === 0) {
        files = findImages(resolve(options.path));
      }
    } catch {
      files = findImages(resolve(options.path));
    }
  }

  // Filter by threshold
  const threshold = parseInt(options.threshold, 10);
  const largeFiles = files.filter((f) => statSync(f).size > threshold);
  const skippedSmall = files.filter((f) => statSync(f).size <= threshold);

  if (largeFiles.length === 0) {
    logger.info("No images need optimization.");
    process.exit(0);
  }

  logger.info(
    `${largeFiles.length} image(s) to optimize (${skippedSmall.length} below threshold)\n`
  );

  // Check budget
  if (options.budget) {
    const budget = parseBudget(options.budget);
    const totalSize = files.reduce((sum, f) => sum + statSync(f).size, 0);
    if (totalSize > budget) {
      logger.warning(
        `Repository size (${humanSize(totalSize)}) exceeds budget (${humanSize(budget)})`
      );
      logger.info("Optimization will attempt to bring size under budget.\n");
    }
  }

  // Optimize
  const results: OptimizationResult[] = [];
  const dryRunPrefix = options.dryRun ? "[DRY-RUN] " : "";

  for (let i = 0; i < largeFiles.length; i++) {
    const file = largeFiles[i];
    const shortName = relative(process.cwd(), file);
    process.stdout.write(`${dryRunPrefix}[${i + 1}/${largeFiles.length}] ${shortName} ... `);

    const result = await optimizeImage(file, {
      quality: parseInt(options.quality, 10),
      maxWidth: parseInt(options.maxWidth, 10),
      maxHeight: parseInt(options.maxHeight, 10),
      stripMetadata: options.stripMetadata,
      webp: options.webp,
      avif: options.avif,
      dryRun: options.dryRun,
    });

    if (result) {
      const savingsStr = result.savingsBytes > 0
        ? `-${humanSize(result.savingsBytes)} (${result.savingsPercent.toFixed(1)}%)`
        : "no savings";
      console.log(savingsStr);
      results.push(result);
    } else {
      console.log("FAILED");
    }
  }

  // Summary
  const successful = results.filter((r) => r !== null);
  const totalOriginal = successful.reduce((s, r) => s + r.originalSize, 0);
  const totalOptimized = successful.reduce((s, r) => s + r.optimizedSize, 0);
  const totalSavings = totalOriginal - totalOptimized;
  const avgSavings = totalOriginal > 0 ? (totalSavings / totalOriginal) * 100 : 0;

  console.log("\n" + "=".repeat(50));
  logger.info(`Optimized: ${successful.length}/${largeFiles.length} files`);
  logger.info(`Original:  ${humanSize(totalOriginal)}`);
  logger.info(`Optimized: ${humanSize(totalOptimized)}`);
  logger.info(`Savings:   ${humanSize(totalSavings)} (${avgSavings.toFixed(1)}%)`);
  console.log("=".repeat(50));

  // Report
  if (options.report) {
    const report: OptimizationReport = {
      timestamp: new Date().toISOString(),
      totalFiles: successful.length,
      totalOriginalBytes: totalOriginal,
      totalOptimizedBytes: totalOptimized,
      totalSavingsBytes: totalSavings,
      totalSavingsPercent: avgSavings,
      results: successful,
      skipped: skippedSmall.map((f) => relative(process.cwd(), f)),
    };

    const reportPath = resolve("optimization-report.json");
    writeFileSync(reportPath, JSON.stringify(report, null, 2), "utf8");
    logger.success(`\nReport saved: ${reportPath}`);
  }

  if (options.dryRun) {
    console.log("\n⚠️ This was a dry run. No files were modified.");
    console.log("   Run without --dry-run to apply optimizations.");
  } else {
    logger.success("\nOptimization complete!");
  }
}

main().catch((err) => {
  logger.error("Optimization failed:", err);
  process.exit(1);
});
