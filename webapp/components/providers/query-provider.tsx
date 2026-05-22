"use client";

import { useState } from "react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";

/**
 * TanStack Query provider — wraps the app in a QueryClientProvider.
 * Uses useState to create a single QueryClient instance per browser session
 * (prevents re-creation on re-renders in RSC environments).
 */
export function QueryProvider({ children }: { children: React.ReactNode }) {
  const [queryClient] = useState(
    () =>
      new QueryClient({
        defaultOptions: {
          queries: {
            // Daily logs are mildly cacheable — refetch after 1 minute
            staleTime: 60_000,
            // Retry once on failure (network glitches)
            retry: 1,
          },
        },
      })
  );

  return (
    <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
  );
}
