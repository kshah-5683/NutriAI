"use client";

import { createContext, useContext, useState } from "react";
import type { SupabaseClient } from "@supabase/supabase-js";
import { createClient } from "@/lib/supabase/client";
import type { Database } from "@/lib/types/database";

type SupabaseContext = {
  supabase: SupabaseClient<Database>;
};

const Context = createContext<SupabaseContext | undefined>(undefined);

/**
 * Provides a Supabase browser client to all Client Components.
 * The client is created once and reused across re-renders.
 */
export function SupabaseProvider({ children }: { children: React.ReactNode }) {
  const [supabase] = useState(() => createClient());

  return (
    <Context.Provider value={{ supabase }}>{children}</Context.Provider>
  );
}

/**
 * Hook to access the Supabase browser client from any Client Component.
 * Must be used within <SupabaseProvider>.
 */
export function useSupabase() {
  const context = useContext(Context);
  if (!context) {
    throw new Error("useSupabase must be used within a <SupabaseProvider>");
  }
  return context.supabase;
}
