/**
 * openmeme-dev optimize-images - Generate responsive image sizes with @squoosh/cli
 *
 * Finds images above a size threshold and produces, next to each original:
 *   {stem}-original.{ext}  — the untouched original, renamed
 *   {stem}-{size}.jpg      — one mozjpeg variant per --sizes entry (e.g. -800, -340),
 *                            downscaled with aspect ratio preserved, never upscaled
 *
 * All variants are encoded with mozjpeg, so PNG sources are converted to JPEG
 * (transparency is flattened; the untouched -original keeps the source format).
 * The `image:` frontmatter field in every MDX of the folder (base + locale
 * variants) is rewritten to the largest generated variant; smaller variants
 * are available for responsive srcset use.
 *
 * Images narrower than the largest size still get a compress-only variant in
 * the largest bucket (e.g. a 700px image yields {stem}-800.jpg at 700px wide)
 * so the MDX always has an optimized file to point at.
 *
 * @squoosh/cli is archived and its WASM loader breaks on Node >= 18
 * (it feeds a filesystem path to global fetch). We disable the built-in
 * fetch via NODE_OPTIONS so Emscripten falls back to reading from disk.
 */

import { spawnSync } from "child_process";
import {
  copyFileSync,
  existsSync,
  mkdtempSync,
  readFileSync,
  readdirSync,
  renameSync,
  rmSync,
  statSync,
  writeFileSync,
} from "fs";
import { tmpdir } from "os";
import { basename, dirname, extname, join, relative } from "path";
import chalk from "chalk";

interface OptimizeImagesOptions {
  path?: string;
  threshold?: string;
  quality?: string;
  sizes?: string;
  limit?: string;
  batchSize?: string;
  dryRun?: boolean;
}

interface Candidate {
  file: string;
  size: number;
  width: number | null;
}

interface VariantPlan {
  size: number;
  resize: boolean;
}

interface Result {
  oldFile: string;
  oldSize: number;
  variants: Array<{ name: string; size: number; bytes: number }>;
  mdxUpdated: string[];
}

const SUPPORTED_EXTS = [".jpg", ".jpeg", ".png"];
const VARIANT_EXT = ".jpg"; // mozjpeg always emits JPEG

function humanSize(bytes: number): string {
  if (bytes >= 1024 ** 2) return `${(bytes / 1024 ** 2).toFixed(1)}MB`;
  if (bytes >= 1024) return `${(bytes / 1024).toFixed(1)}KB`;
  return `${bytes}B`;
}

/** Reads pixel width from PNG IHDR or JPEG SOF headers. */
function imageWidth(file: string): number | null {
  const buf = readFileSync(file);
  // PNG: 8-byte signature, IHDR width at byte 16
  if (buf.length > 24 && buf.readUInt32BE(0) === 0x89504e47) {
    return buf.readUInt32BE(16);
  }
  // JPEG: walk markers until a start-of-frame (C0-CF except C4/C8/CC)
  if (buf.length > 4 && buf[0] === 0xff && buf[1] === 0xd8) {
    let i = 2;
    while (i + 9 < buf.length) {
      if (buf[i] !== 0xff) {
        i++;
        continue;
      }
      const marker = buf[i + 1];
      if (marker === 0xff) {
        i++;
        continue;
      }
      // standalone markers without a length segment
      if (marker === 0x01 || (marker >= 0xd0 && marker <= 0xd8)) {
        i += 2;
        continue;
      }
      if (marker >= 0xc0 && marker <= 0xcf && ![0xc4, 0xc8, 0xcc].includes(marker)) {
        return buf.readUInt16BE(i + 7);
      }
      i += 2 + buf.readUInt16BE(i + 2);
    }
  }
  return null;
}

function findCandidates(dir: string, thresholdBytes: number, sizes: number[]): Candidate[] {
  // skip our own outputs so reruns are idempotent
  const generatedSuffix = new RegExp(`-(original|${sizes.join("|")})\\.[a-z]+$`, "i");
  const out: Candidate[] = [];
  for (const entry of readdirSync(dir, { withFileTypes: true })) {
    const fullPath = join(dir, entry.name);
    if (entry.isDirectory()) {
      out.push(...findCandidates(fullPath, thresholdBytes, sizes));
    } else if (entry.isFile()) {
      if (!SUPPORTED_EXTS.includes(extname(entry.name).toLowerCase())) continue;
      if (generatedSuffix.test(entry.name)) continue;
      const size = statSync(fullPath).size;
      if (size <= thresholdBytes) continue;
      out.push({ file: fullPath, size, width: imageWidth(fullPath) });
    }
  }
  return out;
}

/** Which variants to generate: resize for sizes below the image width, and a
 * compress-only variant in the largest bucket when the image is narrower. */
function planVariants(candidate: Candidate, sizes: number[]): VariantPlan[] {
  const maxSize = sizes[0];
  return sizes.flatMap((size): VariantPlan[] => {
    if (candidate.width !== null && candidate.width > size) return [{ size, resize: true }];
    if (size === maxSize) return [{ size, resize: false }];
    return [];
  });
}

/**
 * Runs one @squoosh/cli invocation. Inputs are copied to a staging dir under
 * collision-free names so memes from different category folders can share a
 * batch. Returns a map of input file -> output path (in tmp).
 */
function runSquoosh(files: string[], cliArgs: string[], workDir: string): Map<string, string> {
  const inDir = mkdtempSync(join(workDir, "in-"));
  const outDir = mkdtempSync(join(workDir, "out-"));
  const stagedBySource = new Map<string, string>();

  files.forEach((file, i) => {
    const staged = `${String(i).padStart(4, "0")}__${basename(file)}`;
    copyFileSync(file, join(inDir, staged));
    stagedBySource.set(file, staged);
  });

  const result = spawnSync(
    "npx",
    [
      "--yes",
      "@squoosh/cli",
      ...cliArgs,
      "-d",
      outDir,
      ...files.map((f) => join(inDir, stagedBySource.get(f)!)),
    ],
    {
      encoding: "utf8",
      env: {
        ...process.env,
        NODE_OPTIONS: `${process.env.NODE_OPTIONS ?? ""} --no-experimental-fetch`.trim(),
      },
    }
  );

  if (result.status !== 0) {
    throw new Error(
      `squoosh-cli failed (exit ${result.status}):\n${result.stderr || result.stdout}`
    );
  }

  const outputs = new Map<string, string>();
  for (const [source, staged] of stagedBySource) {
    const outPath = join(outDir, staged.replace(/\.[^.]+$/, VARIANT_EXT));
    if (existsSync(outPath)) outputs.set(source, outPath);
  }
  return outputs;
}

/** Encodes one variant (size × resize) for a batch of files with mozjpeg. */
function encodeVariant(
  files: string[],
  size: number,
  resize: boolean,
  quality: number,
  workDir: string
): Map<string, string> {
  const args = resize ? ["--resize", JSON.stringify({ width: size })] : [];
  args.push("--mozjpeg", JSON.stringify({ quality }));
  return runSquoosh(files, args, workDir);
}

/** Rewrites `image: "./old.jpg"` to the new filename in every MDX of the folder. */
function updateMdxReferences(dir: string, oldName: string, newName: string): string[] {
  const escaped = oldName.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
  const pattern = new RegExp(`(image:\\s*["'])(?:\\./)?${escaped}(["'])`, "g");
  const updated: string[] = [];

  for (const entry of readdirSync(dir)) {
    if (!entry.endsWith(".mdx")) continue;
    const mdxPath = join(dir, entry);
    const content = readFileSync(mdxPath, "utf8");
    const next = content.replace(pattern, `$1./${newName}$2`);
    if (next !== content) {
      writeFileSync(mdxPath, next, "utf8");
      updated.push(mdxPath);
    }
  }
  return updated;
}

export async function optimizeImagesCommand(options: OptimizeImagesOptions): Promise<void> {
  const memesPath = join(process.cwd(), options.path || "memes");
  const thresholdBytes = parseInt(options.threshold || "80", 10) * 1024;
  const quality = parseInt(options.quality || "75", 10);
  const batchSize = parseInt(options.batchSize || "20", 10);
  const limit = options.limit ? parseInt(options.limit, 10) : Infinity;
  const sizes = (options.sizes || "800,340")
    .split(",")
    .map((s) => parseInt(s.trim(), 10))
    .filter((n) => Number.isFinite(n) && n > 0)
    .sort((a, b) => b - a);

  if (sizes.length === 0) {
    console.error(chalk.red("No valid sizes given (expected e.g. --sizes 800,340)"));
    process.exit(1);
  }
  if (!existsSync(memesPath)) {
    console.log(chalk.yellow(`No memes directory found at ${memesPath}`));
    process.exit(1);
  }

  console.log(chalk.blue("\n🖼  Optimize Images (@squoosh/cli)\n"));

  let candidates = findCandidates(memesPath, thresholdBytes, sizes).sort(
    (a, b) => b.size - a.size
  );
  const totalFound = candidates.length;
  candidates = candidates.slice(0, limit);

  console.log(
    `  ${totalFound} image(s) above ${humanSize(thresholdBytes)}` +
      (candidates.length < totalFound ? `, processing first ${candidates.length}` : "") +
      `; generating mozjpeg sizes [${sizes.join(", ")}] + original\n`
  );

  if (candidates.length === 0) return;

  if (options.dryRun) {
    for (const c of candidates) {
      const plan = planVariants(c, sizes)
        .map((v) => `-${v.size}${v.resize ? "" : " (compress only)"}`)
        .join(", ");
      console.log(
        `  ${chalk.gray(relative(process.cwd(), c.file))} ${humanSize(c.size)} ` +
          `${c.width ? `${c.width}px` : "?px"} → -original, ${plan}`
      );
    }
    console.log(chalk.yellow("\n⚠ Dry run: no files were modified.\n"));
    return;
  }

  const workDir = mkdtempSync(join(tmpdir(), "openmeme-squoosh-"));
  const results: Result[] = [];
  const failed: string[] = [];
  // candidate file -> (variant size -> tmp output path)
  const encoded = new Map<string, Map<number, string>>();

  try {
    for (const size of sizes) {
      for (const resize of [true, false]) {
        const group = candidates.filter((c) =>
          planVariants(c, sizes).some((v) => v.size === size && v.resize === resize)
        );

        for (let i = 0; i < group.length; i += batchSize) {
          const batch = group.slice(i, i + batchSize);
          console.log(
            chalk.cyan(
              `  Encoding mozjpeg -${size}${resize ? "" : " (compress only)"} batch ` +
                `${Math.floor(i / batchSize) + 1} (${batch.length} file(s))...`
            )
          );

          try {
            const outputs = encodeVariant(
              batch.map((c) => c.file),
              size,
              resize,
              quality,
              workDir
            );
            for (const [source, tmpPath] of outputs) {
              if (!encoded.has(source)) encoded.set(source, new Map());
              encoded.get(source)!.set(size, tmpPath);
            }
          } catch (err) {
            console.error(chalk.red(`  ✖ ${err instanceof Error ? err.message : err}`));
            failed.push(...batch.map((c) => c.file));
          }
        }
      }
    }

    // Finalize: rename originals, move variants into place, update MDX
    for (const candidate of candidates) {
      if (failed.includes(candidate.file)) continue;
      const rel = relative(process.cwd(), candidate.file);
      const tmpVariants = encoded.get(candidate.file) ?? new Map<number, string>();
      const planned = planVariants(candidate, sizes);

      if (tmpVariants.size < planned.length) {
        console.error(chalk.red(`  ✖ ${rel}: missing variant output`));
        failed.push(candidate.file);
        continue;
      }

      const dir = dirname(candidate.file);
      const oldName = basename(candidate.file);
      const ext = extname(oldName).toLowerCase();
      const stem = oldName.replace(/\.[^.]+$/, "");

      renameSync(candidate.file, join(dir, `${stem}-original${ext}`));

      const variants: Result["variants"] = [];
      for (const [size, tmpPath] of [...tmpVariants.entries()].sort((a, b) => b[0] - a[0])) {
        const name = `${stem}-${size}${VARIANT_EXT}`;
        renameSync(tmpPath, join(dir, name));
        variants.push({ name, size, bytes: statSync(join(dir, name)).size });
      }

      const mdxUpdated = updateMdxReferences(dir, oldName, variants[0].name);

      const variantStr = variants
        .map((v) => `${chalk.bold(`-${v.size}`)} ${humanSize(v.bytes)}`)
        .join(", ");
      console.log(
        `  ${chalk.green("✔")} ${rel} (${humanSize(candidate.size)}) → ` +
          `-original, ${variantStr}` +
          (mdxUpdated.length
            ? chalk.gray(` [${mdxUpdated.length} mdx → ${variants[0].name}]`)
            : chalk.yellow(" [no mdx referenced it]"))
      );

      results.push({ oldFile: candidate.file, oldSize: candidate.size, variants, mdxUpdated });
    }
  } finally {
    rmSync(workDir, { recursive: true, force: true });
  }

  const totalOld = results.reduce((s, r) => s + r.oldSize, 0);
  const totalServed = results.reduce((s, r) => s + r.variants[0].bytes, 0);

  console.log(chalk.blue("\n📊 Summary\n"));
  console.log(`  Processed: ${chalk.green(results.length)}`);
  console.log(`  Variants written: ${results.reduce((s, r) => s + r.variants.length, 0)}`);
  console.log(`  Failed: ${failed.length > 0 ? chalk.red(failed.length) : chalk.gray(0)}`);
  if (results.length > 0) {
    const pct = ((1 - totalServed / totalOld) * 100).toFixed(1);
    console.log(
      `  Served size (largest variant): ${humanSize(totalOld)} → ${humanSize(totalServed)} ` +
        `(${chalk.green(`-${pct}%`)})`
    );
    console.log(
      chalk.gray("\n  Images were renamed — run `pnpm index` to rebuild the web indexes.")
    );
  }
  console.log();

  process.exit(failed.length > 0 ? 1 : 0);
}
