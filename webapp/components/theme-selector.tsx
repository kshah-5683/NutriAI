"use client";

import { useThemeStore, type Theme } from "@/lib/stores/theme-store";

const THEME_OPTIONS: { value: Theme; label: string; icon: string }[] = [
  { value: "light", label: "Light", icon: "☀️" },
  { value: "dark", label: "Dark", icon: "🌙" },
  { value: "system", label: "System", icon: "💻" },
];

/**
 * 3-segment theme selector — Light / Dark / System.
 * Same visual pattern as PeriodSelector for consistency.
 */
export function ThemeSelector() {
  const theme = useThemeStore((s) => s.theme);
  const setTheme = useThemeStore((s) => s.setTheme);

  return (
    <div
      className="flex rounded-lg p-1"
      style={{ backgroundColor: "var(--bg-surface-variant)" }}
    >
      {THEME_OPTIONS.map(({ value, label, icon }) => {
        const isActive = theme === value;
        return (
          <button
            key={value}
            onClick={() => setTheme(value)}
            className="flex flex-1 items-center justify-center gap-1.5 rounded-md px-3 py-2 text-xs font-medium transition-colors"
            style={{
              backgroundColor: isActive ? "var(--bg-surface)" : "transparent",
              color: isActive
                ? "var(--color-primary)"
                : "var(--text-secondary)",
              boxShadow: isActive
                ? "0 1px 3px rgba(0,0,0,0.08)"
                : "none",
            }}
          >
            <span>{icon}</span>
            {label}
          </button>
        );
      })}
    </div>
  );
}
