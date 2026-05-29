import type { NextConfig } from "next";
import createNextIntlPlugin from "next-intl/plugin";
import { initOpenNextCloudflareForDev } from "@opennextjs/cloudflare";

const withNextIntl = createNextIntlPlugin("./i18n/request.ts");

const nextConfig: NextConfig = {
  images: {
    unoptimized: true,
  },
  async headers() {
    return [
      {
        source: "/:path*{/}?",
        headers: [
          {
            key: "X-Accel-Buffering",
            value: "no",
          },
        ],
      },
    ];
  },
  async redirects() {
    return [
      {
        source: "/top",
        destination: "/memes/top",
        permanent: true,
      },
      {
        source: "/nuevos",
        destination: "/memes/nuevos",
        permanent: true,
      },
      {
        source: "/clasicos",
        destination: "/memes/clasicos",
        permanent: true,
      },
      {
        source: "/aleatorio",
        destination: "/memes/aleatorio",
        permanent: true,
      },
    ];
  },
};

export default withNextIntl(nextConfig);

if (process.env.NODE_ENV === 'development') {
  initOpenNextCloudflareForDev();
}
