"use client";

import { useMutation, useQueryClient } from "@tanstack/react-query";
import { useSupabase } from "@/components/providers/supabase-provider";
import { nowMs } from "@/lib/utils/constants";
import { triggerPrefetch } from "@/lib/utils/prefetch-trigger";

/**
 * TanStack Query mutation — soft-deletes a daily log.
 *
 * CRITICAL PARITY RULES:
 * 1. Sets deleted_at AND last_modified_at so LWW resolves correctly with Android
 * 2. Sets is_synced = true (web writes go directly to Supabase)
 * 3. Never hard-deletes — let pg_cron handle tombstone purge after 15 days
 */
export function useDeleteLog() {
  const supabase = useSupabase();
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async (logId: string) => {
      const now = nowMs();

      // Guard: don't re-delete already-tombstoned rows.
      const { error } = await supabase
        .from("daily_logs")
        .update({
          deleted_at: now,
          last_modified_at: now,
          is_synced: true,
        })
        .eq("id", logId)
        .is("deleted_at", null);

      if (error) throw error;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["daily-logs"] });
      queryClient.invalidateQueries({ queryKey: ["recommendation-cache"] });
      triggerPrefetch(supabase);
    },
  });
}
