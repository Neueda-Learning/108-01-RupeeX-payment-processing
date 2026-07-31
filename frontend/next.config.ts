import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  // Produces a minimal, self-contained server build (.next/standalone)
  // used by the production Docker image.
  output: "standalone",
};

export default nextConfig;
