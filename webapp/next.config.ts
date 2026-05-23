import type { NextConfig } from "next";
import path from "path";

const nextConfig: NextConfig = {
  turbopack: {
    // Set root to the webapp directory to prevent Next.js from
    // picking up the parent Android project's lockfile as the workspace root.
    root: path.resolve(__dirname),
  },
  // Exclude Node.js-only packages from webpack bundling.
  // undici and https-proxy-agent use node: URI imports that webpack cannot resolve.
  // They are only used server-side (instrumentation.ts) and must remain as
  // Node.js externals rather than being bundled into the client/edge bundle.
  serverExternalPackages: ["undici", "https-proxy-agent"],
};

export default nextConfig;
