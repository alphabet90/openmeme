const apiBaseUrl =
  process.env.NEXT_PUBLIC_MEMES_API_URL?.replace(/\/+$/, "") ||
  "https://api-production-681e.up.railway.app";

const cdnBaseUrl =
  process.env.NEXT_PUBLIC_MEMES_CDN_URL?.replace(/\/+$/, "") ||
  "https://cdn-openmeme.clientes-g4a.workers.dev";

export const site = {
  name: "OPENMEME",
  legalName: "OpenMeme",
  domain: "openmeme.com",
  url: "https://openmeme.com",
  tagline: "All the memes. In one place.",
  description:
    "The largest community meme repository. Thousands of memes in JPG/PNG, uploaded by the people for the people. Search, download and share the best memes.",
  twitter: "@openmeme",
  ogImage: "/og-default.png",
  apiBaseUrl,
  cdnBaseUrl,
} as const;

export type Site = typeof site;
