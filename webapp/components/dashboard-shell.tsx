"use client";

import { useRouter } from "next/navigation";
import { BottomNav } from "@/components/bottom-nav";
import { ThemeToggleButton } from "@/components/theme-toggle-button";
import { createClient } from "@/lib/supabase/client";
import type { ReactNode } from "react";

/**
 * Dashboard shell — wraps authenticated pages with header + bottom nav.
 * Used by all dashboard pages (home, insights, catalog).
 */
export function DashboardShell({ children }: { children: ReactNode }) {
  const router = useRouter();

  async function handleSignOut() {
    const supabase = createClient();
    await supabase.auth.signOut();
    router.push("/auth/sign-in");
    router.refresh();
  }

  return (
    <div className="flex min-h-screen flex-col">
      {/* Top header bar */}
      <header
        className="flex items-center justify-between border-b px-4 py-3"
        style={{
          backgroundColor: "var(--bg-surface)",
          borderColor: "var(--border-variant)",
        }}
      >
        <div className="flex items-center gap-2">
          <div
            className="flex h-8 w-8 items-center justify-center rounded-lg text-lg"
            style={{ backgroundColor: "var(--bg-primary-container)" }}
          >
            🥗
          </div>
          <span className="text-base font-semibold text-primary dark:text-primary-light">NutriAI</span>
        </div>

        <div className="flex items-center gap-2">
          <ThemeToggleButton />
          <button
            onClick={handleSignOut}
            className="rounded-sm px-3 py-1.5 text-xs font-medium transition-colors hover:bg-sage-gray"
            style={{ color: "var(--text-secondary)" }}
          >
            Sign Out
          </button>
        </div>
      </header>

      {/* Page content */}
      <main className="flex-1 pb-16">{children}</main>

      {/* Bottom navigation */}
      <BottomNav />
    </div>
  );
}
