import { fileURLToPath } from "url";
import { dirname, resolve } from "path";

const __dirname = dirname(fileURLToPath(import.meta.url));

export const CONFIG = {
  SUBREDDIT: process.env.SUBREDDIT || "argentina",
  REPO_PATH: process.env.REPO_PATH ? resolve(process.env.REPO_PATH) : resolve(__dirname, "../../../.."),
  TMP_DIR: process.env.TMP_DIR ? resolve(process.env.TMP_DIR) : resolve(__dirname, "../../../../tmp"),
  BLOOM_FILTER_FILE: process.env.BLOOM_FILTER_FILE
    ? resolve(process.env.BLOOM_FILTER_FILE)
    : resolve(__dirname, "../../../../data/processed.bloom"),
  LEGACY_STATE_FILE: process.env.LEGACY_STATE_FILE
    ? resolve(process.env.LEGACY_STATE_FILE)
    : resolve(__dirname, "../../../../data/state.json"),
  BLOOM_CAPACITY: 200_000,
  BLOOM_ERROR_RATE: 1e-3,

  REQUEST_DELAY_MS: 1000,
  MAX_IMAGE_SIZE_BYTES: 10 * 1024 * 1024, // 10 MB
  MIN_COMMENT_UPVOTES: 0,

  SUPPORTED_EXTENSIONS: new Set([".jpg", ".jpeg", ".png"]),
  IMAGE_HOSTS: new Set([
    "i.redd.it",
    "i.imgur.com",
    "preview.redd.it",
    "external-preview.redd.it",
  ]),

  REDDIT_BASE_URL: "https://old.reddit.com",
  REDDIT_USER_AGENT: process.env.REDDIT_USER_AGENT || "OpenMemeScraper/1.0 (github.com/alphabet90/openmeme)",

  CLASSIFY_WORKERS: parseInt(process.env.CLASSIFY_WORKERS || "4", 10),
  DOWNLOAD_WORKERS: parseInt(process.env.DOWNLOAD_WORKERS || "8", 10),
  CLASSIFIER: process.env.CLASSIFIER || "claude", // "claude" | "codex"

  API_TIMEOUT_MS: 15000,
  RATE_LIMIT_BASE_WAIT_S: 360,
} as const;

export type Config = typeof CONFIG;
