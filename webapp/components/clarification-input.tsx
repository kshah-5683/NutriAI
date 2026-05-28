"use client";

import { useState } from "react";

interface ClarificationInputProps {
  /** AI-generated hint explaining why clarification is needed */
  hint: string;
  /** Called when user clicks "Use generic" */
  onUseGeneric: () => void;
  /** Called when user submits brand or weight clarification */
  onSubmitClarification: (input: string) => void;
  /** True while a re-lookup is in progress after clarification */
  isLoading?: boolean;
}

/**
 * Inline clarification banner shown inside ParsedFoodCard when
 * needsClarification is true. Allows the user to either accept
 * generic nutrition data or provide a brand name / gram weight
 * for a more accurate lookup.
 */
export function ClarificationInput({
  hint,
  onUseGeneric,
  onSubmitClarification,
  isLoading,
}: ClarificationInputProps) {
  const [input, setInput] = useState("");

  const handleSubmit = () => {
    const trimmed = input.trim();
    if (trimmed) {
      onSubmitClarification(trimmed);
    }
  };

  return (
    <div
      className="mt-2 rounded-md border p-2.5"
      style={{
        backgroundColor: "var(--bg-warning, #FFF8E1)",
        borderColor: "var(--border-warning, #FFE082)",
      }}
    >
      {/* Hint text */}
      <div className="flex items-start gap-1.5 text-xs">
        <span className="mt-px shrink-0">⚠️</span>
        <span style={{ color: "var(--text-primary)" }}>{hint}</span>
      </div>

      {/* Input + buttons */}
      <div className="mt-2 flex items-center gap-2">
        <input
          type="text"
          value={input}
          onChange={(e) => setInput(e.target.value)}
          onKeyDown={(e) => {
            if (e.key === "Enter") handleSubmit();
          }}
          placeholder="Brand name or weight (e.g. 40g)"
          disabled={isLoading}
          className="flex-1 rounded-md border px-2 py-1.5 text-xs outline-none transition-colors disabled:opacity-50"
          style={{
            backgroundColor: "var(--bg-surface)",
            borderColor: "var(--border-variant)",
            color: "var(--text-primary)",
          }}
        />
      </div>

      <div className="mt-2 flex items-center gap-2">
        <button
          onClick={onUseGeneric}
          disabled={isLoading}
          className="rounded-md px-3 py-1 text-xs font-medium transition-colors disabled:opacity-50"
          style={{
            backgroundColor: "var(--bg-surface-variant)",
            color: "var(--text-secondary)",
          }}
        >
          Use generic
        </button>
        <button
          onClick={handleSubmit}
          disabled={isLoading || !input.trim()}
          className="rounded-md px-3 py-1 text-xs font-medium transition-colors disabled:opacity-50"
          style={{
            backgroundColor: "var(--color-primary)",
            color: "var(--color-on-primary, #fff)",
          }}
        >
          {isLoading ? "Looking up..." : "Update & Lookup"}
        </button>
      </div>
    </div>
  );
}

/**
 * Badge showing the quality of the nutrition match after lookup.
 */
export function MatchTypeBadge({
  matchType,
  brandNotFound,
}: {
  matchType: "branded" | "generic" | null;
  /** True when user specified a brand but FDC didn't have it */
  brandNotFound?: boolean;
}) {
  if (matchType === "branded") {
    return (
      <span
        className="inline-flex items-center gap-1 rounded-full px-2 py-0.5 text-xs font-medium"
        style={{
          backgroundColor: "var(--color-primary-container, #D4E8C8)",
          color: "var(--color-primary)",
        }}
      >
        🟢 Exact brand match
      </span>
    );
  }

  if (matchType === "generic") {
    return (
      <span
        className="inline-flex items-center gap-1 rounded-full px-2 py-0.5 text-xs font-medium"
        style={{
          backgroundColor: "var(--bg-warning, #FFF8E1)",
          color: "var(--text-warning, #F57F17)",
        }}
      >
        {brandNotFound ? "🟡 Brand not found, using generic" : "🟡 Generic estimate"}
      </span>
    );
  }

  return null;
}
