import type { NextConfig } from "next";
import createNextIntlPlugin from "next-intl/plugin";

const withNextIntl = createNextIntlPlugin("./i18n/request.ts");

const nextConfig: NextConfig = {
  images: {
    unoptimized: true,
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

import("@opennextjs/cloudflare")
  .then(({ initOpenNextCloudflareForDev }) => initOpenNextCloudflareForDev())
  .catch(() => {});
