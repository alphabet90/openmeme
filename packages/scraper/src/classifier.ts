/**
 * Pluggable classifier interface for meme detection.
 * Ported from reddit.memes/src/classifier.py and classifiers/
 */

import { spawn } from "child_process";
import { readFileSync } from "fs";
import { resolve, dirname } from "path";
import { fileURLToPath } from "url";
import { CONFIG } from "./config.js";
import type { ClassificationResult } from "./models.js";

const logger = {
  info: (...args: unknown[]) => console.log("[classifier]", ...args),
  warning: (...args: unknown[]) => console.warn("[classifier]", ...args),
  error: (...args: unknown[]) => console.error("[classifier]", ...args),
};

export interface BaseClassifier {
  classifyBatch(
    items: Array<{ url: string; path: string }>,
    options?: { maxWorkers?: number; locale?: string }
  ): Promise<ClassificationResult[]>;
}

// ---- Prompt Loader ----

const __dirname = dirname(fileURLToPath(import.meta.url));

function normalizeLocale(locale: string): string {
  return locale.replace("_", "-").toLowerCase();
}

export function loadPrompt(locale: string = "en"): string {
  const normalized = normalizeLocale(locale);
  const candidates = [
    resolve(__dirname, "../../prompts", `prompt.${normalized}.txt`),
    resolve(__dirname, "../../prompts", `prompt.${normalized.split("-")[0]}.txt`),
    resolve(__dirname, "../../prompts", "prompt.en.txt"),
  ];

  for (const path of candidates) {
    try {
      const content = readFileSync(path, "utf8");
      logger.info(`Loaded prompt for locale '${locale}' from ${path}`);
      return content;
    } catch {
      // try next
    }
  }

  // Fallback default prompt
  return `You are a meme classifier. Look at the image and determine if it is a meme.
If it IS a meme, respond with a JSON object:
{
  "is_meme": true,
  "category": "category_name",
  "filename_slug": "descriptive-slug",
  "title": "Meme Title",
  "description": "Brief description of the meme",
  "tags": ["tag1", "tag2"]
}

If it is NOT a meme, respond with:
{
  "is_meme": false
}

Categories: funny, wholesome, politics, gaming, tech, relatable, absurd, argentina, other`;
}

// ---- Claude Classifier ----

export class ClaudeClassifier implements BaseClassifier {
  async classifyBatch(
    items: Array<{ url: string; path: string }>,
    options?: { maxWorkers?: number; locale?: string }
  ): Promise<ClassificationResult[]> {
    const locale = options?.locale || "en";
    const prompt = loadPrompt(locale);
    const maxWorkers = options?.maxWorkers || CONFIG.CLASSIFY_WORKERS;

    // Process in parallel with worker limit
    const queue: Promise<ClassificationResult>[] = [];
    const running: Promise<unknown>[] = [];

    for (const item of items) {
      const task = this._classifyOne(item, prompt);
      queue.push(task);

      running.push(task);
      if (running.length >= maxWorkers) {
        await Promise.race(running);
        running.splice(
          0,
          running.length,
          ...running.filter((p) => {
            // Check if still pending
            const { status } = Object(p);
            return status === "pending";
          })
        );
      }
    }

    return Promise.all(queue);
  }

  private async _classifyOne(
    item: { url: string; path: string },
    prompt: string
  ): Promise<ClassificationResult> {
    try {
      const result = await this._callClaude(item.path, prompt);
      return { ...result, url: item.url };
    } catch (err) {
      const error = err instanceof Error ? err.message : String(err);
      if (error.includes("command not found") || error.includes("ENOENT")) {
        return {
          url: item.url,
          is_meme: false,
          category: "",
          filename_slug: "",
          title: "",
          description: "",
          tags: [],
          error: "claude_not_found",
        };
      }
      return {
        url: item.url,
        is_meme: false,
        category: "",
        filename_slug: "",
        title: "",
        description: "",
        tags: [],
        error,
      };
    }
  }

  private _callClaude(imagePath: string, prompt: string): Promise<Omit<ClassificationResult, "url">> {
    return new Promise((resolve, reject) => {
      const args = [
        "-p",
        prompt,
        "--output-format",
        "json",
        "--",
        imagePath,
      ];

      const child = spawn("claude", args, { timeout: 120_000 });
      let stdout = "";
      let stderr = "";

      child.stdout.on("data", (data: Buffer) => {
        stdout += data.toString();
      });

      child.stderr.on("data", (data: Buffer) => {
        stderr += data.toString();
      });

      child.on("close", (code) => {
        if (code !== 0) {
          reject(new Error(`claude exited with ${code}: ${stderr}`));
          return;
        }
        try {
          const parsed = JSON.parse(stdout);
          resolve({
            is_meme: !!parsed.is_meme,
            category: String(parsed.category || ""),
            filename_slug: String(parsed.filename_slug || ""),
            title: String(parsed.title || ""),
            description: String(parsed.description || ""),
            tags: Array.isArray(parsed.tags) ? parsed.tags.map(String) : [],
          });
        } catch {
          reject(new Error(`Invalid JSON from claude: ${stdout.slice(0, 200)}`));
        }
      });

      child.on("error", (err: Error) => {
        reject(err);
      });
    });
  }
}

// ---- Codex Classifier ----

export class CodexClassifier implements BaseClassifier {
  async classifyBatch(
    items: Array<{ url: string; path: string }>,
    options?: { maxWorkers?: number; locale?: string }
  ): Promise<ClassificationResult[]> {
    const locale = options?.locale || "en";
    const prompt = loadPrompt(locale);

    const results: ClassificationResult[] = [];
    for (const item of items) {
      try {
        const result = await this._callCodex(item.path, prompt);
        results.push({ ...result, url: item.url });
      } catch (err) {
        const error = err instanceof Error ? err.message : String(err);
        results.push({
          url: item.url,
          is_meme: false,
          category: "",
          filename_slug: "",
          title: "",
          description: "",
          tags: [],
          error,
        });
      }
    }
    return results;
  }

  private _callCodex(imagePath: string, prompt: string): Promise<Omit<ClassificationResult, "url">> {
    return new Promise((resolve, reject) => {
      const args = ["-i", imagePath, "-q", prompt, "--json"];

      const child = spawn("codex", args, { timeout: 120_000 });
      let stdout = "";
      let stderr = "";

      child.stdout.on("data", (data: Buffer) => {
        stdout += data.toString();
      });

      child.stderr.on("data", (data: Buffer) => {
        stderr += data.toString();
      });

      child.on("close", (code) => {
        if (code !== 0) {
          reject(new Error(`codex exited with ${code}: ${stderr}`));
          return;
        }
        try {
          const parsed = JSON.parse(stdout);
          resolve({
            is_meme: !!parsed.is_meme,
            category: String(parsed.category || ""),
            filename_slug: String(parsed.filename_slug || ""),
            title: String(parsed.title || ""),
            description: String(parsed.description || ""),
            tags: Array.isArray(parsed.tags) ? parsed.tags.map(String) : [],
          });
        } catch {
          reject(new Error(`Invalid JSON from codex: ${stdout.slice(0, 200)}`));
        }
      });

      child.on("error", (err: Error) => {
        reject(err);
      });
    });
  }
}

export function createClassifier(name?: string): BaseClassifier {
  const classifierName = name || CONFIG.CLASSIFIER;
  switch (classifierName) {
    case "codex":
      return new CodexClassifier();
    case "claude":
    default:
      return new ClaudeClassifier();
  }
}
