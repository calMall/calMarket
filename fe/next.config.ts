import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  /* config options here */
  images: {
    remotePatterns: [
      {
        protocol: "https",
        hostname: "thumbnail.image.rakuten.co.jp",
        pathname: "/**",
      },
      {
        protocol: "https",
        hostname: "r.r10s.jp",
        pathname: "/**",
      },
    ],
  },
};

export default nextConfig;
