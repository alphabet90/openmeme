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
  // Proxy PostHog through /ingest to avoid ad-blockers
  skipTrailingSlashRedirect: true,
  async rewrites() {
    return [
      {
        source: "/ingest/static/:path*",
        destination: "https://us-assets.i.posthog.com/static/:path*",
      },
      {
        source: "/ingest/:path*",
        destination: "https://us.i.posthog.com/:path*",
      },
    ];
  },
};

export default withNextIntl(nextConfig);
