import { createBrowserClient } from "@supabase/ssr";
import type { Database } from "../types/database";

/**
 * Creates a Supabase browser client for use in Client Components.
 * Uses cookie-based session management via @supabase/ssr.
 */
export function createClient() {
  return createBrowserClient<Database>(
    process.env.NEXT_PUBLIC_SUPABASE_URL!,
    process.env.NEXT_PUBLIC_SUPABASE_ANON_KEY!
  );
}
