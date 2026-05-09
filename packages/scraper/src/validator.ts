/**
 * Meme validation utilities.
 * Used by scripts/guard.ts and the scraper CLI.
 */

import { existsSync, readFileSync, readdirSync, statSync } from "fs";
import { join, extname, dirname } from "path";
import { pathToFileURL } from "url";

const logger = {
  info: (...args: unknown[]) => console.log("[validator]", ...args),
  warning: (...args: unknown[]) => console.warn("[validator]", ...args),
  error: (...args: unknown[]) => console.error("[validator]", ...args),
};

export interface ValidationRule {
  name: string;
  description: string;
  required: boolean;
  validate: (meme: MemeData) => ValidationResult;
}

export interface ValidationResult {
  pass: boolean;
  message?: string;
}

export interface MemeData {
  imagePath: string;
  mdxPath?: string;
  title?: string;
  description?: string;
  author?: string;
  subreddit?: string;
  category?: string;
  slug?: string;
  score?: number;
  created_at?: string;
  source_url?: string;
  post_url?: string;
  image?: string;
  tags?: string[];
}

// ---- Validation Rules ----

export const VALIDATION_RULES: ValidationRule[] = [
  {
    name: "image_exists",
    description: "Image file must exist and be non-empty",
    required: true,
    validate: (meme) => {
      if (!meme.imagePath || !existsSync(meme.imagePath)) {
        return { pass: false, message: `Image file not found: ${meme.imagePath}` };
      }
      const stat = statSync(meme.imagePath);
      if (stat.size === 0) {
        return { pass: false, message: `Image file is empty: ${meme.imagePath}` };
      }
      return { pass: true };
    },
  },
  {
    name: "valid_image_extension",
    description: "Image must have a valid extension (.jpg, .jpeg, .png, .gif, .webp)",
    required: true,
    validate: (meme) => {
      const ext = extname(meme.imagePath).toLowerCase();
      const valid = [".jpg", ".jpeg", ".png", ".gif", ".webp"];
      if (!valid.includes(ext)) {
        return { pass: false, message: `Invalid image extension: ${ext}` };
      }
      return { pass: true };
    },
  },
  {
    name: "title_present",
    description: "Meme must have a title",
    required: true,
    validate: (meme) => {
      if (!meme.title || meme.title.trim().length === 0) {
        return { pass: false, message: "Title is required" };
      }
      if (meme.title.length > 200) {
        return { pass: false, message: `Title too long (${meme.title.length} chars, max 200)` };
      }
      return { pass: true };
    },
  },
  {
    name: "description_present",
    description: "Meme must have a description",
    required: true,
    validate: (meme) => {
      if (!meme.description || meme.description.trim().length === 0) {
        return { pass: false, message: "Description is required" };
      }
      return { pass: true };
    },
  },
  {
    name: "source_url_valid",
    description: "Source URL must be a valid URL",
    required: true,
    validate: (meme) => {
      if (!meme.source_url) {
        return { pass: false, message: "Source URL is required" };
      }
      try {
        new URL(meme.source_url);
        return { pass: true };
      } catch {
        return { pass: false, message: `Invalid source URL: ${meme.source_url}` };
      }
    },
  },
  {
    name: "category_present",
    description: "Meme must have a category",
    required: true,
    validate: (meme) => {
      if (!meme.category || meme.category.trim().length === 0) {
        return { pass: false, message: "Category is required" };
      }
      return { pass: true };
    },
  },
  {
    name: "tags_present",
    description: "Meme should have at least one tag",
    required: false,
    validate: (meme) => {
      if (!meme.tags || meme.tags.length === 0) {
        return { pass: false, message: "Warning: No tags provided" };
      }
      return { pass: true };
    },
  },
  {
    name: "slug_valid",
    description: "Slug must be URL-safe",
    required: true,
    validate: (meme) => {
      if (!meme.slug) {
        return { pass: false, message: "Slug is required" };
      }
      if (!/^[a-z0-9\-]+$/.test(meme.slug)) {
        return { pass: false, message: `Slug contains invalid characters: ${meme.slug}` };
      }
      return { pass: true };
    },
  },
  {
    name: "author_present",
    description: "Author must be specified",
    required: true,
    validate: (meme) => {
      if (!meme.author || meme.author.trim().length === 0) {
        return { pass: false, message: "Author is required" };
      }
      return { pass: true };
    },
  },
  {
    name: "subreddit_present",
    description: "Subreddit must be specified",
    required: true,
    validate: (meme) => {
      if (!meme.subreddit || meme.subreddit.trim().length === 0) {
        return { pass: false, message: "Subreddit is required" };
      }
      return { pass: true };
    },
  },
];

// ---- Parser ----

function parseMdx(mdxPath: string): MemeData | null {
  if (!existsSync(mdxPath)) return null;
  try {
    const content = readFileSync(mdxPath, "utf8");
    const frontmatter = content.match(/---\n([\s\S]*?)\n---/);
    if (!frontmatter) return null;

    const data: Record<string, unknown> = {};
    const lines = frontmatter[1].split("\n");
    for (const line of lines) {
      const match = line.match(/^([a-z_]+):\s*(.*)$/);
      if (match) {
        const [, key, value] = match;
        // Handle arrays
        if (value.startsWith("[") && value.endsWith("]")) {
          data[key] = value
            .slice(1, -1)
            .split(",")
            .map((s) => s.trim().replace(/^"|"$/g, ""))
            .filter(Boolean);
        } else if (value.match(/^\d+$/)) {
          data[key] = parseInt(value, 10);
        } else if (value.startsWith('"') && value.endsWith('"')) {
          data[key] = value.slice(1, -1);
        } else {
          data[key] = value;
        }
      }
    }

    return {
      imagePath: "",
      mdxPath,
      title: data.title as string,
      description: data.description as string,
      author: data.author as string,
      subreddit: data.subreddit as string,
      category: data.category as string,
      slug: data.slug as string,
      score: data.score as number,
      created_at: data.created_at as string,
      source_url: data.source_url as string,
      post_url: data.post_url as string,
      image: data.image as string,
      tags: data.tags as string[],
    };
  } catch (err) {
    logger.warning(`Failed to parse MDX: ${mdxPath}: ${err}`);
    return null;
  }
}

// ---- Public API ----

export function validateMeme(meme: MemeData): {
  valid: boolean;
  errors: string[];
  warnings: string[];
} {
  const errors: string[] = [];
  const warnings: string[] = [];

  for (const rule of VALIDATION_RULES) {
    const result = rule.validate(meme);
    if (!result.pass) {
      if (rule.required) {
        errors.push(`[${rule.name}] ${result.message}`);
      } else {
        warnings.push(`[${rule.name}] ${result.message}`);
      }
    }
  }

  return {
    valid: errors.length === 0,
    errors,
    warnings,
  };
}

export async function validateMemes(memesDir: string): Promise<boolean> {
  if (!existsSync(memesDir)) {
    logger.error(`Memes directory not found: ${memesDir}`);
    return false;
  }

  let total = 0;
  let passed = 0;
  let failed = 0;

  function walk(dir: string) {
    for (const entry of readdirSync(dir, { withFileTypes: true })) {
      const fullPath = join(dir, entry.name);
      if (entry.isDirectory()) {
        walk(fullPath);
      } else if (entry.isFile() && [".jpg", ".jpeg", ".png", ".gif", ".webp"].includes(extname(entry.name).toLowerCase())) {
        total++;
        const mdxPath = fullPath.replace(/\.[^.]+$/, ".mdx");

        const memeData: MemeData = { imagePath: fullPath };
        if (existsSync(mdxPath)) {
          const parsed = parseMdx(mdxPath);
          if (parsed) {
            Object.assign(memeData, parsed, { imagePath: fullPath, mdxPath });
          }
        }

        const { valid, errors, warnings } = validateMeme(memeData);
        if (valid) {
          passed++;
          if (warnings.length > 0) {
            logger.warning(`${entry.name}: ${warnings.join("; ")}`);
          }
        } else {
          failed++;
          logger.error(`${entry.name}:`);
          for (const err of errors) {
            logger.error(`  - ${err}`);
          }
        }
      }
    }
  }

  walk(memesDir);

  logger.info(`\nValidation complete: ${passed}/${total} passed, ${failed} failed`);
  return failed === 0;
}

export function validateMemeFile(imagePath: string): {
  valid: boolean;
  errors: string[];
  warnings: string[];
} {
  const mdxPath = imagePath.replace(/\.[^.]+$/, ".mdx");

  const memeData: MemeData = { imagePath };
  if (existsSync(mdxPath)) {
    const parsed = parseMdx(mdxPath);
    if (parsed) {
      Object.assign(memeData, parsed, { imagePath, mdxPath });
    }
  }

  return validateMeme(memeData);
}

export { VALIDATION_RULES };
