#!/usr/bin/env node
/**
 * tools/cli - OpenMeme CLI
 *
 * Utility for adding memes from the terminal.
 * Provides an interactive and scriptable interface for meme management.
 *
 * Commands:
 *   openmeme add <image>     Add a meme interactively
 *   openmeme add --batch <dir>  Batch add memes from directory
 *   openmeme list             List memes in repository
 *   openmeme search <query>   Search memes by tag/title
 *   openmeme validate         Validate all memes
 *   openmeme stats            Show repository statistics
 *   openmeme import <url>     Import meme from URL
 */

import { Command } from "commander";
import chalk from "chalk";
import { addCommand } from "./commands/add.js";
import { listCommand } from "./commands/list.js";
import { searchCommand } from "./commands/search.js";
import { validateCommand } from "./commands/validate.js";
import { statsCommand } from "./commands/stats.js";
import { importCommand } from "./commands/import.js";

const program = new Command();

program
  .name("openmeme")
  .description("OpenMeme CLI - Manage your meme repository")
  .version("1.0.0");

program
  .command("add")
  .description("Add a meme to the repository")
  .argument("[image]", "Path to image file")
  .option("-t, --title <title>", "Meme title")
  .option("-d, --description <desc>", "Meme description")
  .option("-c, --category <cat>", "Meme category")
  .option("--subreddit <sub>", "Source subreddit")
  .option("--author <author>", "Original author")
  .option("--tags <tags>", "Comma-separated tags")
  .option("--url <url>", "Source URL")
  .option("--batch <dir>", "Batch add from directory")
  .option("--interactive", "Interactive mode (default if no args)", false)
  .option("-y, --yes", "Skip confirmation prompts", false)
  .action(addCommand);

program
  .command("list")
  .description("List memes in the repository")
  .option("-c, --category <cat>", "Filter by category")
  .option("-l, --limit <n>", "Limit results", "50")
  .option("--json", "Output as JSON", false)
  .action(listCommand);

program
  .command("search")
  .description("Search memes by tag, title, or description")
  .argument("<query>", "Search query")
  .option("-t, --tag", "Search in tags only", false)
  .option("--json", "Output as JSON", false)
  .action(searchCommand);

program
  .command("validate")
  .description("Validate all memes in the repository")
  .option("-p, --path <path>", "Path to memes directory", "./memes")
  .option("-s, --strict", "Treat warnings as errors", false)
  .option("--fix", "Auto-fix issues where possible", false)
  .action(validateCommand);

program
  .command("stats")
  .description("Show repository statistics")
  .option("-p, --path <path>", "Path to memes directory", "./memes")
  .option("--json", "Output as JSON", false)
  .action(statsCommand);

program
  .command("import")
  .description("Import a meme from a URL")
  .argument("<url>", "Image URL or Reddit post URL")
  .option("-t, --title <title>", "Override title")
  .option("-c, --category <cat>", "Override category")
  .option("--auto-classify", "Use AI to classify", false)
  .action(importCommand);

program.parse();
