import { createServerClient } from "@supabase/ssr";
import { cookies } from "next/headers";
import type { Database } from "../types/database";

/**
 * Creates a Supabase server client for use in Server Components, Server Actions,
 * and Route Handlers. Reads/writes session from Next.js cookies.
 *
 * NOTE: Web always uses the real Supabase UUID — no "local_user" mapping needed.
 * The local_user → UUID translation is Android-only (offline mode).
 */
export async function createClient() {
  const cookieStore = await cookies();

  return createServerClient<Database>(
    process.env.NEXT_PUBLIC_SUPABASE_URL!,
    process.env.NEXT_PUBLIC_SUPABASE_ANON_KEY!,
    {
      cookies: {
        getAll() {
          return cookieStore.getAll();
        },
        setAll(cookiesToSet) {
          try {
            cookiesToSet.forEach(({ name, value, options }) =>
              cookieStore.set(name, value, options)
            );
          } catch {
            // setAll can be called from Server Components where cookies are read-only.
            // Middleware handles refreshing the session in that case.
          }
        },
      },
    }
  );
}
