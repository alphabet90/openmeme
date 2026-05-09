/**
 * openmeme validate - Validate all memes in the repository
 */

import { validateMemes } from "@openmeme/scraper";
import chalk from "chalk";

interface ValidateOptions {
  path?: string;
  strict?: boolean;
  fix?: boolean;
}

export async function validateCommand(options: ValidateOptions): Promise<void> {
  console.log(chalk.blue("\n🔍 Validating memes...\n"));
  const ok = await validateMemes(options.path || "./memes");
  if (ok) {
    console.log(chalk.green("\n✅ All memes are valid!\n"));
  } else {
    console.log(chalk.red("\n❌ Some memes have validation errors.\n"));
    process.exit(1);
  }
}
