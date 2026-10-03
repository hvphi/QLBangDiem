import type { NextConfig } from "next";
import path from "node:path";

const nextConfig: NextConfig = {
  output: "standalone",
  poweredByHeader: false,
  transpilePackages: ["qlbangdiem-signing-bridge"],
  turbopack: {
    root: path.resolve(process.cwd(), ".."),
  },
  async rewrites() {
    const backend = process.env.API_PROXY_TARGET?.replace(/\/$/, "");
    return backend ? [{ source: "/api/:path*", destination: `${backend}/api/:path*` }] : [];
  },
};

export default nextConfig;
