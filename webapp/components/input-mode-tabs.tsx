"use client";

import { useLogFormStore, type InputMode } from "@/lib/stores/log-form-store";

const TABS: { key: InputMode; label: string; icon: string }[] = [
  { key: "ai", label: "AI Parse", icon: "✨" },
  { key: "manual", label: "Manual", icon: "✏️" },
  { key: "scan", label: "Scan", icon: "📸" },
];

/**
 * Tab switcher for the Log page — AI Parse / Manual / Scan.
 * Controlled by the Zustand log form store.
 */
export function InputModeTabs() {
  const inputMode = useLogFormStore((s) => s.inputMode);
  const setInputMode = useLogFormStore((s) => s.setInputMode);

  return (
    <div
      className="flex rounded-lg p-1"
      style={{ backgroundColor: "var(--bg-surface-variant)" }}
    >
      {TABS.map(({ key, label, icon }) => {
        const isActive = inputMode === key;
        return (
          <button
            key={key}
            onClick={() => setInputMode(key)}
            className="flex flex-1 items-center justify-center gap-1.5 rounded-md px-3 py-2 text-sm font-medium transition-colors"
            style={{
              backgroundColor: isActive ? "var(--bg-surface)" : "transparent",
              color: isActive
                ? "var(--color-primary)"
                : "var(--text-secondary)",
              boxShadow: isActive ? "0 1px 3px rgba(0,0,0,0.1)" : "none",
            }}
          >
            <span>{icon}</span>
            <span>{label}</span>
          </button>
        );
      })}
    </div>
  );
}
