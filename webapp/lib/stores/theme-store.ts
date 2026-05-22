"use client";

import { create } from "zustand";

export type Theme = "light" | "dark" | "system";

interface ThemeState {
  theme: Theme;
  setTheme: (theme: Theme) => void;
}

/**
 * Zustand store for theme preference.
 *
 * Persists to localStorage under "nutriai-theme".
 * Applies the `.dark` class on <html> based on the resolved theme.
 *
 * Values:
 *  - "light"  → always light
 *  - "dark"   → always dark
 *  - "system" → follows prefers-color-scheme
 */
export const useThemeStore = create<ThemeState>((set) => ({
  theme: "system",

  setTheme: (theme) => {
    set({ theme });
    try {
      localStorage.setItem("nutriai-theme", theme);
    } catch {
      // SSR or blocked localStorage — ignore
    }
    applyTheme(theme);
  },
}));

/**
 * Applies the `.dark` class on <html> based on the resolved theme.
 */
export function applyTheme(theme: Theme) {
  if (typeof document === "undefined") return;

  const isDark =
    theme === "dark" ||
    (theme === "system" &&
      window.matchMedia("(prefers-color-scheme: dark)").matches);

  document.documentElement.classList.toggle("dark", isDark);
}

/**
 * Initializes the theme store from localStorage.
 * Call once on app mount (e.g., in a useEffect or ThemeInitScript).
 */
export function initThemeFromStorage() {
  try {
    const stored = localStorage.getItem("nutriai-theme") as Theme | null;
    const theme = stored ?? "system";
    useThemeStore.setState({ theme });
    applyTheme(theme);
  } catch {
    // SSR or blocked localStorage — use system default
    applyTheme("system");
  }
}
