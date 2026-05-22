"use client";

import { useThemeStore } from "@/lib/stores/theme-store";

/**
 * Quick-toggle button for the dashboard header.
 * Cycles: light → dark → system → light.
 * Shows ☀️ (light), 🌙 (dark), or 💻 (system) based on current theme.
 */
export function ThemeToggleButton() {
  const theme = useThemeStore((s) => s.theme);
  const setTheme = useThemeStore((s) => s.setTheme);

  const icon = theme === "dark" ? "🌙" : theme === "light" ? "☀️" : "💻";
  const nextTheme = theme === "light" ? "dark" : theme === "dark" ? "system" : "light";
  const label =
    theme === "light"
      ? "Switch to dark mode"
      : theme === "dark"
        ? "Switch to system theme"
        : "Switch to light mode";

  return (
    <button
      onClick={() => setTheme(nextTheme)}
      className="flex h-8 w-8 items-center justify-center rounded-md text-base transition-colors hover:bg-sage-gray"
      aria-label={label}
      title={label}
    >
      {icon}
    </button>
  );
}
