/**
 * openmeme import - Import a meme from a URL
 */

import { writeFileSync, mkdirSync } from "fs";
import { join } from "path";
import chalk from "chalk";
import ora from "ora";
import { fetchSinglePost } from "@openmeme/scraper";

interface ImportOptions {
  title?: string;
  category?: string;
  autoClassify?: boolean;
}

function yamlStr(s: string): string {
  return (s || "")
    .replace(/\\/g, "\\\\")
    .replace(/"/g, '\\"')
    .replace(/\n/g, "\\n")
    .replace(/\r/g, "\\r");
}

function sanitizeSlug(slug: string): string {
  return (slug || "meme")
    .toLowerCase()
    .replace(/[^a-z0-9\-]/g, "-")
    .replace(/-{2,}/g, "-")
    .replace(/^-|-$/g, "")
    .slice(0, 80);
}

export async function importCommand(url: string, options: ImportOptions): Promise<void> {
  console.log(chalk.blue(`\nImporting from: ${url}\n`));
  const spinner = ora("Fetching...").start();

  const isReddit = url.includes("reddit.com") || url.includes("redd.it");

  if (isReddit) {
    try {
      const posts = await fetchSinglePost(url);
      spinner.succeed(`Found ${posts.length} post(s)`);

      const post = posts[0];
      const title = options.title || post.title || "Imported Meme";
      const category = options.category || "other";
      const slug = sanitizeSlug(title);
      const imageUrl = post.url || "";

      if (!imageUrl) {
        console.log(chalk.yellow("No image found in post."));
        return;
      }

      spinner.start("Downloading image...");
      const resp = await fetch(imageUrl);
      if (!resp.ok) {
        spinner.fail(`Download failed: HTTP ${resp.status}`);
        return;
      }

      const buffer = Buffer.from(await resp.arrayBuffer());
      const ext = resp.headers.get("content-type")?.includes("png") ? ".png" : ".jpg";

      const repoPath = join(process.cwd(), "memes", category);
      mkdirSync(repoPath, { recursive: true });

      const imageName = `${slug}${ext}`;
      const imagePath = join(repoPath, imageName);
      writeFileSync(imagePath, buffer);
      spinner.succeed(`Image saved: ${imageName}`);

      const now = new Date().toISOString();
      const mdxContent = `---\ntitle: "${yamlStr(title)}"\ndescription: "Imported from Reddit"\nauthor: "${yamlStr(post.author || "unknown")}"\nsubreddit: "${yamlStr(post.subreddit || "unknown")}"\ncategory: "${yamlStr(category)}"\nslug: "${slug}"\nscore: ${post.score || 0}\ncreated_at: "${now}"\nsource_url: "${yamlStr(imageUrl)}"\npost_url: "https://reddit.com${post.permalink || ""}"\nimage: "./${imageName}"\ntags: ["${yamlStr(post.subreddit || "unknown")}", "${yamlStr(category)}"]\n---\n\n# ${title}\n\nImported from r/${post.subreddit || "unknown"}\n`;

      writeFileSync(imagePath.replace(/\.[^.]+$/, ".mdx"), mdxContent, "utf8");
      console.log(chalk.green("Meme imported!"));
    } catch (err) {
      spinner.fail(`Import failed: ${err}`);
    }
  } else {
    try {
      spinner.start("Downloading image...");
      const resp = await fetch(url);
      if (!resp.ok) {
        spinner.fail(`Download failed: HTTP ${resp.status}`);
        return;
      }
      const buffer = Buffer.from(await resp.arrayBuffer());
      const title = options.title || "Imported Meme";
      const category = options.category || "other";
      const slug = sanitizeSlug(title);
      const repoPath = join(process.cwd(), "memes", category);
      mkdirSync(repoPath, { recursive: true });
      const imageName = `${slug}.jpg`;
      const imagePath = join(repoPath, imageName);
      writeFileSync(imagePath, buffer);
      spinner.succeed(`Image saved: ${imageName}`);

      const mdxContent = `---\ntitle: "${yamlStr(title)}"\ndescription: "Imported from URL"\nauthor: "unknown"\nsubreddit: "unknown"\ncategory: "${yamlStr(category)}"\nslug: "${slug}"\nscore: 0\ncreated_at: "${new Date().toISOString()}"\nsource_url: "${yamlStr(url)}"\npost_url: ""\nimage: "./${imageName}"\ntags: ["${yamlStr(category)}"]\n---\n\n# ${title}\n\nImported from ${url}\n`;

      writeFileSync(imagePath.replace(/\.[^.]+$/, ".mdx"), mdxContent, "utf8");
      console.log(chalk.green("Image imported!"));
    } catch (err) {
      spinner.fail(`Import failed: ${err}`);
    }
  }
}
