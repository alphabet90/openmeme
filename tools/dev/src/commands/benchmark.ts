/**
 * openmeme-dev benchmark - Benchmark the scraper pipeline
 */

import { runPipeline } from "@openmeme/scraper";
import chalk from "chalk";

interface BenchmarkOptions {
  subreddit?: string;
  limit?: string;
  iterations?: string;
}

export async function benchmarkCommand(options: BenchmarkOptions): Promise<void> {
  const subreddit = options.subreddit || "memes";
  const limit = parseInt(options.limit || "50", 10);
  const iterations = parseInt(options.iterations || "3", 10);

  console.log(chalk.blue(`\n⏱️  Benchmarking scraper pipeline`));
  console.log(chalk.gray(`  Subreddit: r/${subreddit}`));
  console.log(chalk.gray(`  Limit: ${limit} posts`));
  console.log(chalk.gray(`  Iterations: ${iterations}\n`));

  const times: number[] = [];
  const totals: number[] = [];

  for (let i = 0; i < iterations; i++) {
    process.stdout.write(chalk.gray(`  Run ${i + 1}/${iterations}... `));
    const start = Date.now();
    try {
      const total = await runPipeline({
        subreddit,
        limit,
        dryRun: true,
        noBranch: true,
      });
      const elapsed = Date.now() - start;
      times.push(elapsed);
      totals.push(total);
      console.log(chalk.green(`${(elapsed / 1000).toFixed(1)}s, ${total} memes`));
    } catch (err) {
      const elapsed = Date.now() - start;
      times.push(elapsed);
      console.log(chalk.red(`failed (${(elapsed / 1000).toFixed(1)}s)`));
    }
  }

  const avg = times.reduce((a, b) => a + b, 0) / times.length;
  const min = Math.min(...times);
  const max = Math.max(...times);
  const avgMemes = totals.reduce((a, b) => a + b, 0) / totals.length;

  console.log(chalk.gray("\n  ──────────────────────────"));
  console.log(chalk.white(`  Avg time:   ${(avg / 1000).toFixed(1)}s`));
  console.log(chalk.white(`  Min time:   ${(min / 1000).toFixed(1)}s`));
  console.log(chalk.white(`  Max time:   ${(max / 1000).toFixed(1)}s`));
  console.log(chalk.white(`  Avg memes:  ${avgMemes.toFixed(0)}`));
  console.log(chalk.gray("  ──────────────────────────\n"));
}
