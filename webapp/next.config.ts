import type { NextConfig } from "next";
import path from "path";

const nextConfig: NextConfig = {
  turbopack: {
    // Set root to the webapp directory to prevent Next.js from
    // picking up the parent Android project's lockfile as the workspace root.
    root: path.resolve(__dirname),
  },
};

export default nextConfig;
