/**
 * Next.js instrumentation — runs once when the server starts.
 *
 * Configures undici's global ProxyAgent so that all server-side `fetch()` calls
 * (including Supabase auth, RLS queries, etc.) tunnel through the corporate
 * HTTP proxy when HTTPS_PROXY is set.
 *
 * Without this, Node 20's built-in fetch (undici) ignores HTTPS_PROXY and
 * connections to Supabase fail with ECONNRESET behind Walmart's proxy.
 */
export async function register() {
  const proxyUrl = process.env.HTTPS_PROXY || process.env.https_proxy;

  if (proxyUrl) {
    // Dynamic import so this only loads on the server (not bundled into client)
    const { ProxyAgent, setGlobalDispatcher } = await import("undici");
    setGlobalDispatcher(new ProxyAgent(proxyUrl));
    console.log(`[instrumentation] Global proxy set: ${proxyUrl}`);
  }
}
