"use client";

import { useEffect } from "react";
import { initThemeFromStorage, applyTheme } from "@/lib/stores/theme-store";

/**
 * Client component that initializes the theme from localStorage on first mount,
 * and listens for system color-scheme changes when theme is "system".
 *
 * Placed in RootLayout so it runs once on app hydration.
 */
export function ThemeInitScript() {
  useEffect(() => {
    // Hydrate theme from localStorage → apply .dark class
    initThemeFromStorage();

    // Listen for system preference changes
    const mediaQuery = window.matchMedia("(prefers-color-scheme: dark)");
    const handleChange = () => {
      const stored = localStorage.getItem("nutriai-theme") ?? "system";
      if (stored === "system") {
        applyTheme("system");
      }
    };

    mediaQuery.addEventListener("change", handleChange);
    return () => mediaQuery.removeEventListener("change", handleChange);
  }, []);

  return null;
}
