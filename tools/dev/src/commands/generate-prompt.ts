/**
 * openmeme-dev generate-prompt - Generate classifier prompt for a locale
 */

import { writeFileSync, existsSync, readFileSync, mkdirSync } from "fs";
import { resolve, dirname, join } from "path";
import chalk from "chalk";

interface PromptOptions {
  output?: string;
  template?: string;
}

const DEFAULT_TEMPLATE = `You are a meme classifier. Look at the image and determine if it is a meme.
If it IS a meme, respond with JSON:
{
  "is_meme": true,
  "category": "category_name",
  "filename_slug": "descriptive-slug",
  "title": "Meme Title",
  "description": "Brief description",
  "tags": ["tag1", "tag2"]
}

If NOT a meme:
{
  "is_meme": false
}

Categories: funny, wholesome, politics, gaming, tech, relatable, absurd, other`;

const LOCALE_INTRO: Record<string, string> = {
  es: "Eres un clasificador de memes. Mira la imagen y determina si es un meme.",
  "es-ar": "Sos un clasificador de memes. Mirá la imagen y determiná si es un meme.",
  pt: "Voce e um classificador de memes. Olhe a imagem e determine se e um meme.",
  fr: "Vous etes un classificateur de memes. Regardez l'image et determinez si c'est un meme.",
  de: "Du bist ein Meme-Klassifizierer. Schau dir das Bild an und bestimme, ob es ein Meme ist.",
  it: "Sei un classificatore di meme. Guarda l'immagine e determina se e un meme.",
  ja: "あなたはミーム分類器です。画像を見て、それがミームかどうかを判断してください。",
  ko: "당신은 밈 분류기입니다. 이미지를 보고 밈인지 판단하세요.",
  zh: "你是一个梗图分类器。请看图片并判断它是否是一个梗图。",
  ru: "Ты классификатор мемов. Посмотри на изображение и определи, является ли это мемом.",
};

function generateLocalizedPrompt(locale: string, template: string): string {
  const intro = LOCALE_INTRO[locale.toLowerCase()];
  if (!intro) {
    return template;
  }

  const lines = template.split("\n");
  const localized: string[] = [];
  let replacedIntro = false;

  for (const line of lines) {
    if (!replacedIntro && line.includes("You are a meme classifier")) {
      localized.push(intro);
      replacedIntro = true;
    } else if (
      line.includes("If it IS a meme") ||
      line.includes("If NOT a meme") ||
      line.includes("Categories:")
    ) {
      localized.push(line);
    } else if (line.trim() === "" || line.startsWith("{") || line.startsWith("}")) {
      localized.push(line);
    } else {
      localized.push(line);
    }
  }

  return localized.join("\n");
}

export async function generatePromptCommand(locale: string, options: PromptOptions): Promise<void> {
  console.log(chalk.blue(`\n📝 Generating prompt for locale: ${locale}\n`));

  let template = DEFAULT_TEMPLATE;
  if (options.template) {
    const templatePath = resolve(options.template);
    if (existsSync(templatePath)) {
      template = readFileSync(templatePath, "utf8");
      console.log(chalk.gray(`Using template: ${templatePath}`));
    } else {
      console.log(chalk.yellow(`Template not found: ${templatePath}`));
    }
  }

  const localized = generateLocalizedPrompt(locale, template);

  const outputPath = options.output
    ? resolve(options.output)
    : resolve(process.cwd(), "prompts", `prompt.${locale}.txt`);

  mkdirSync(dirname(outputPath), { recursive: true });
  writeFileSync(outputPath, localized, "utf8");

  console.log(chalk.green(`✅ Prompt saved: ${outputPath}`));
  console.log(chalk.gray(`\nPreview:\n${localized.slice(0, 300)}...\n`));
}
