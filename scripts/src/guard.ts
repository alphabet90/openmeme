#!/usr/bin/env node
/**
 * scripts/guard.ts - Validation guard for meme quality
 *
 * Inspired by Open Design's validation scripts, this "guardian" verifies
 * that each new meme added to the repository meets all quality criteria
 * before allowing a commit to proceed.
 *
 * Usage:
 *   npx tsx scripts/guard.ts [options]
 *   npx tsx scripts/guard.ts --staged        # validate only staged files
 *   npx tsx scripts/guard.ts --all           # validate all memes
 *   npx tsx scripts/guard.ts --path ./memes  # validate specific path
 *
 * Exit codes:
 *   0 - all memes pass validation
 *   1 - one or more memes failed validation
 */

import { execSync } from "child_process";
import { existsSync, statSync } from "fs";
import { extname, resolve, relative } from "path";
import { validateMemeFile, VALIDATION_RULES } from "@openmeme/scraper";
import { Command } from "commander";

const logger = {
  info: (...args: unknown[]) => console.log("[guard]", ...args),
  warning: (...args: unknown[]) => console.warn("[guard]", ...args),
  error: (...args: unknown[]) => console.error("[guard]", ...args),
  success: (...args: unknown[]) => console.log("[guard] ✅", ...args),
  fail: (...args: unknown[]) => console.error("[guard] ❌", ...args),
};

// ---- CLI ----

const program = new Command();
program
  .name("guard")
  .description("Validate memes before commit")
  .option("-s, --staged", "Validate only git staged files", false)
  .option("-a, --all", "Validate all memes in repository", false)
  .option("-p, --path <path>", "Path to memes directory", "./memes")
  .option("--strict", "Treat warnings as errors", false)
  .option("--fix", "Attempt to auto-fix issues", false)
  .option("-v, --verbose", "Show detailed output", false)
  .option("--rules", "List all validation rules", false)
  .parse();

const options = program.opts();

// ---- List Rules ----

if (options.rules) {
  console.log("\n📋 Validation Rules\n");
  for (const rule of VALIDATION_RULES) {
    const badge = rule.required ? "🔴 REQUIRED" : "🟡 OPTIONAL";
    console.log(`  ${rule.name}`);
    console.log(`     ${rule.description}`);
    console.log(`     ${badge}\n`);
  }
  process.exit(0);
}

// ---- Get files to validate ----

function getStagedMemeFiles(): string[] {
  try {
    const output = execSync("git diff --cached --name-only --diff-filter=ACMR", {
      encoding: "utf8",
      cwd: process.cwd(),
    });
    const files = output
      .split("\n")
      .map((f) => f.trim())
      .filter((f) => f.length > 0);

    return files.filter((f) => {
      const ext = extname(f).toLowerCase();
      return [".jpg", ".jpeg", ".png", ".gif", ".webp"].includes(ext);
    });
  } catch {
    logger.error("Not a git repository or no staged files");
    return [];
  }
}

function getAllMemeFiles(memesPath: string): string[] {
  const { readdirSync } = require("fs");
  const { join } = require("path");
  const results: string[] = [];
  const exts = [".jpg", ".jpeg", ".png", ".gif", ".webp"];

  if (!existsSync(memesPath)) {
    logger.error(`Memes directory not found: ${memesPath}`);
    return [];
  }

  function walk(dir: string) {
    for (const entry of readdirSync(dir, { withFileTypes: true })) {
      const full = join(dir, entry.name);
      if (entry.isDirectory()) {
        walk(full);
      } else if (entry.isFile() && exts.includes(extname(entry.name).toLowerCase())) {
        results.push(full);
      }
    }
  }

  walk(memesPath);
  return results;
}

// ---- Validate ----

interface ValidationReport {
  file: string;
  valid: boolean;
  errors: string[];
  warnings: string[];
  size: number;
}

function validateFile(filePath: string): ValidationReport {
  const fullPath = resolve(filePath);
  const stat = statSync(fullPath);
  const { valid, errors, warnings } = validateMemeFile(fullPath);

  return {
    file: relative(process.cwd(), fullPath),
    valid,
    errors,
    warnings,
    size: stat.size,
  };
}

function printReport(report: ValidationReport, verbose: boolean): void {
  if (report.valid && report.warnings.length === 0) {
    if (verbose) {
      logger.success(`${report.file} (${(report.size / 1024).toFixed(1)} KB)`);
    }
    return;
  }

  if (report.valid) {
    logger.warning(`${report.file} (${(report.size / 1024).toFixed(1)} KB)`);
    for (const w of report.warnings) {
      console.log(`  ⚠️  ${w}`);
    }
    return;
  }

  logger.fail(`${report.file} (${(report.size / 1024).toFixed(1)} KB)`);
  for (const e of report.errors) {
    console.log(`  🔴 ${e}`);
  }
  for (const w of report.warnings) {
    console.log(`  ⚠️  ${w}`);
  }
}

// ---- Main ----

async function main() {
  logger.info("OpenMeme Guard - Meme Quality Validator\n");

  let files: string[];
  if (options.staged) {
    files = getStagedMemeFiles();
    logger.info(`Found ${files.length} staged meme image(s)\n`);
  } else if (options.all) {
    files = getAllMemeFiles(options.path);
    logger.info(`Found ${files.length} meme image(s) in ${options.path}\n`);
  } else {
    // Default: staged files if in a git repo, else all
    try {
      execSync("git rev-parse --git-dir", { stdio: "ignore" });
      files = getStagedMemeFiles();
      if (files.length === 0) {
        files = getAllMemeFiles(options.path);
      }
    } catch {
      files = getAllMemeFiles(options.path);
    }
  }

  if (files.length === 0) {
    logger.info("No meme files to validate.");
    process.exit(0);
  }

  const reports: ValidationReport[] = [];
  for (const file of files) {
    const report = validateFile(file);
    reports.push(report);
    printReport(report, options.verbose);
  }

  const failed = reports.filter((r) => !r.valid);
  const warned = reports.filter((r) => r.valid && r.warnings.length > 0);
  const passed = reports.filter((r) => r.valid && r.warnings.length === 0);

  console.log("\n" + "=".repeat(50));
  logger.info(`Results: ${passed.length} passed, ${warned.length} warned, ${failed.length} failed`);
  console.log("=".repeat(50));

  if (failed.length > 0) {
    logger.fail(`\n${failed.length} meme(s) failed validation. Fix errors before committing.`);
    console.log("\nTip: Run with --verbose for detailed error messages.");
    console.log("     Run with --rules to see all validation rules.");
    process.exit(1);
  }

  if (warned.length > 0 && options.strict) {
    logger.fail(`\n${warned.length} meme(s) have warnings (strict mode).`);
    process.exit(1);
  }

  if (warned.length > 0) {
    logger.warning(`\n${warned.length} meme(s) have non-critical warnings.`);
  }

  logger.success("All memes pass validation. Safe to commit!");
  process.exit(0);
}

main().catch((err) => {
  logger.error("Guard failed:", err);
  process.exit(1);
});
