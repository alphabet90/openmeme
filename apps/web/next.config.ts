import type { NextConfig } from "next";
import createNextIntlPlugin from "next-intl/plugin";

const cdnHost = process.env.NEXT_PUBLIC_MEMES_CDN_URL?.replace(/^https?:\/\//, "");

const withNextIntl = createNextIntlPlugin("./i18n/request.ts");

const nextConfig: NextConfig = {
  images: {
    remotePatterns: cdnHost
      ? [{ protocol: "https", hostname: cdnHost, pathname: "/**" }]
      : [],
  },
};

export default withNextIntl(nextConfig);
