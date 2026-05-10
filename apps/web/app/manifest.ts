import type { MetadataRoute } from "next";
import { site } from "@/lib/site";
import { defaultLocale, localeLangMap } from "@/i18n/routing";

export default function manifest(): MetadataRoute.Manifest {
  return {
    name: `${site.name} — ${site.tagline}`,
    short_name: site.name,
    description: site.description,
    start_url: `/${defaultLocale}`,
    display: "standalone",
    background_color: "#0D0D0D",
    theme_color: "#0D0D0D",
    lang: localeLangMap[defaultLocale],
    icons: [
      {
        src: "/favicon.ico",
        sizes: "any",
        type: "image/x-icon",
      },
    ],
  };
}
