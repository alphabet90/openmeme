/**
 * openmeme-dev setup-hooks - Setup git hooks for the repository
 */

import { writeFileSync, existsSync, mkdirSync, chmodSync } from "fs";
import { join, resolve } from "path";
import chalk from "chalk";

interface SetupHooksOptions {
  preCommit?: boolean;
}

const PRE_COMMIT_HOOK = `#!/bin/sh
# OpenMeme pre-commit hook
# Automatically validates memes before allowing commits

echo "[OpenMeme] Running pre-commit validation..."

# Check if the guard script exists
if [ -f "./scripts/dist/guard.js" ]; then
  node "./scripts/dist/guard.js" --staged --verbose
  exit $?
elif [ -f "./node_modules/.bin/tsx" ] && [ -f "./scripts/src/guard.ts" ]; then
  npx tsx "./scripts/src/guard.ts" --staged --verbose
  exit $?
else
  echo "[OpenMeme] Warning: Guard script not found. Skipping validation."
  echo "           Run 'pnpm build' in scripts/ to enable validation."
  exit 0
fi
`;

const PRE_PUSH_HOOK = `#!/bin/sh
# OpenMeme pre-push hook
# Runs optimization check before pushing

echo "[OpenMeme] Running pre-push checks..."

# Check repository size
REPO_SIZE=$(du -sb . 2>/dev/null | cut -f1)
MAX_SIZE=$((500 * 1024 * 1024))  # 500MB

if [ "$REPO_SIZE" -gt "$MAX_SIZE" ]; then
  echo "[OpenMeme] Warning: Repository size exceeds 500MB"
  echo "           Consider running: npx tsx scripts/src/optimize.ts --all"
fi

exit 0
`;

export async function setupHooksCommand(options: SetupHooksOptions): Promise<void> {
  const gitDir = resolve(".git");
  const hooksDir = join(gitDir, "hooks");

  if (!existsSync(gitDir)) {
    console.log(chalk.red("Not a git repository."));
    return;
  }

  mkdirSync(hooksDir, { recursive: true });

  console.log(chalk.blue("\n🔧 Setting up git hooks\n"));

  if (options.preCommit) {
    const preCommitPath = join(hooksDir, "pre-commit");
    writeFileSync(preCommitPath, PRE_COMMIT_HOOK, "utf8");
    chmodSync(preCommitPath, 0o755);
    console.log(chalk.green("  ✅ pre-commit hook installed"));
    console.log(chalk.gray(`     ${preCommitPath}`));
  }

  const prePushPath = join(hooksDir, "pre-push");
  writeFileSync(prePushPath, PRE_PUSH_HOOK, "utf8");
  chmodSync(prePushPath, 0o755);
  console.log(chalk.green("  ✅ pre-push hook installed"));
  console.log(chalk.gray(`     ${prePushPath}`));

  console.log(chalk.blue("\nGit hooks are now active. To bypass:"));
  console.log(chalk.gray("  git commit --no-verify"));
  console.log();
}
