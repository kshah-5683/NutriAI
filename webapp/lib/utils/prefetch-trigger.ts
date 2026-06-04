import type { SupabaseClient } from "@supabase/supabase-js";
import { EDGE_FUNCTIONS } from "./constants";
import { startOfDayMs } from "./format";

/**
 * Fire-and-forget prefetch trigger.
 * Called in onSuccess of log/update/delete mutations to proactively
 * refresh the recommendation cache for the next meal slot.
 *
 * CRITICAL: Passes `dateTimestamp` (client-local midnight) and `currentHour`
 * so the Edge Function uses the client's timezone for cache keys and meal
 * progression — not the Deno server's UTC clock.
 *
 * Errors are silently swallowed — prefetch is best-effort and must
 * never block or fail the primary mutation flow.
 */
export function triggerPrefetch(supabase: SupabaseClient): void {
  supabase.functions
    .invoke(EDGE_FUNCTIONS.PREFETCH_RECOMMENDATIONS, {
      body: {
        dateTimestamp: startOfDayMs(new Date()),
        currentHour: new Date().getHours(),
      },
    })
    .catch(() => {});
}
