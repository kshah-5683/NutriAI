"use client";

import { useState, useEffect } from "react";

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
        backgroundColor: "var(--bg-warning)",
        borderColor: "var(--border-warning)",
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

interface MultiClarificationInputProps {
  clarification: {
    id: string;
    question: string;
    options: string[];
  };
  activeIndex: number;
  totalQuestions: number;
  onAnswerSelected: (answer: string) => void;
  onBack?: () => void;
  isLoading?: boolean;
}

export function MultiClarificationInput({
  clarification,
  activeIndex,
  totalQuestions,
  onAnswerSelected,
  onBack,
  isLoading,
}: MultiClarificationInputProps) {
  const [customText, setCustomText] = useState("");

  // Reset custom text when activeIndex changes
  useEffect(() => {
    setCustomText("");
  }, [activeIndex]);

  const handleCustomSubmit = () => {
    const trimmed = customText.trim();
    if (trimmed) {
      onAnswerSelected(trimmed);
    }
  };

  return (
    <div
      className="mt-2 rounded-md border p-3 flex flex-col gap-2.5"
      style={{
        backgroundColor: "var(--bg-tertiary-container, rgba(0, 0, 0, 0.03))",
        borderColor: "var(--border-variant)",
      }}
    >
      {/* Header with back button */}
      <div className="flex items-center gap-2">
        {activeIndex > 0 && onBack && (
          <button
            onClick={onBack}
            disabled={isLoading}
            className="p-1 rounded hover:bg-black/5 dark:hover:bg-white/5 transition-colors disabled:opacity-50 text-sm font-bold"
            style={{ color: "var(--color-primary)" }}
          >
            ←
          </button>
        )}
        <span className="text-sm font-semibold" style={{ color: "var(--text-primary)" }}>
          {clarification.question}
        </span>
      </div>

      {/* Options list */}
      <div className="flex flex-col gap-1.5">
        {clarification.options.map((option, i) => (
          <button
            key={i}
            onClick={() => onAnswerSelected(option)}
            disabled={isLoading}
            className="w-full text-left rounded-md border px-3 py-2 text-sm font-medium transition-colors hover:bg-black/5 dark:hover:bg-white/5 disabled:opacity-50"
            style={{
              borderColor: "var(--border-variant)",
              color: "var(--color-primary)",
              backgroundColor: "var(--bg-surface)",
            }}
          >
            {option}
          </button>
        ))}
      </div>

      {/* "Something else..." custom text field */}
      <div className="flex flex-col gap-1.5">
        <input
          type="text"
          value={customText}
          onChange={(e) => setCustomText(e.target.value)}
          onKeyDown={(e) => {
            if (e.key === "Enter") handleCustomSubmit();
          }}
          placeholder="Something else... (type custom answer)"
          disabled={isLoading}
          className="w-full rounded-md border px-3 py-2 text-sm outline-none transition-colors disabled:opacity-50"
          style={{
            backgroundColor: "var(--bg-surface)",
            borderColor: "var(--border-variant)",
            color: "var(--text-primary)",
          }}
        />

        {customText.trim() && (
          <button
            onClick={handleCustomSubmit}
            disabled={isLoading}
            className="self-end rounded-md px-3 py-1.5 text-xs font-semibold transition-colors disabled:opacity-50"
            style={{
              backgroundColor: "var(--color-primary)",
              color: "var(--color-on-primary, #fff)",
            }}
          >
            {isLoading ? "Submitting..." : "Submit"}
          </button>
        )}
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
          backgroundColor: "var(--bg-branded)",
          color: "var(--text-branded)",
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
          backgroundColor: "var(--bg-warning)",
          color: "var(--text-warning)",
        }}
      >
        {brandNotFound ? "🟡 Brand not found, using generic" : "🟡 Generic estimate"}
      </span>
    );
  }

  return null;
}
