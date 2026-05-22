"use client";

import { useDateStore } from "@/lib/stores/date-store";
import { formatHeaderDate, toLocalDateString } from "@/lib/utils/format";

/**
 * Date navigation header — prev/next arrows with formatted date.
 * Tapping the date text jumps to today.
 * Mirrors Android's DateNavigationHeader component.
 */
export function DateNavigationHeader() {
  const selectedDate = useDateStore((s) => s.selectedDate);
  const goToPrevDay = useDateStore((s) => s.goToPrevDay);
  const goToNextDay = useDateStore((s) => s.goToNextDay);
  const goToToday = useDateStore((s) => s.goToToday);

  const isToday =
    toLocalDateString(selectedDate) === toLocalDateString(new Date());

  return (
    <div className="flex items-center justify-between px-4 py-3">
      {/* Previous day */}
      <button
        onClick={goToPrevDay}
        className="flex h-10 w-10 items-center justify-center rounded-full transition-colors hover:bg-sage-gray"
        aria-label="Previous day"
      >
        <svg width="20" height="20" viewBox="0 0 20 20" fill="none">
          <path
            d="M12.5 15L7.5 10L12.5 5"
            stroke="currentColor"
            strokeWidth="2"
            strokeLinecap="round"
            strokeLinejoin="round"
          />
        </svg>
      </button>

      {/* Date display — tap to jump to today */}
      <button
        onClick={goToToday}
        className="flex flex-col items-center gap-0.5"
        aria-label="Go to today"
      >
        <span
          className="text-base font-semibold"
          style={{ color: "var(--text-primary)" }}
        >
          {formatHeaderDate(selectedDate)}
        </span>
        {isToday && (
          <span className="text-xs font-medium text-primary">Today</span>
        )}
      </button>

      {/* Next day */}
      <button
        onClick={goToNextDay}
        className="flex h-10 w-10 items-center justify-center rounded-full transition-colors hover:bg-sage-gray"
        aria-label="Next day"
      >
        <svg width="20" height="20" viewBox="0 0 20 20" fill="none">
          <path
            d="M7.5 5L12.5 10L7.5 15"
            stroke="currentColor"
            strokeWidth="2"
            strokeLinecap="round"
            strokeLinejoin="round"
          />
        </svg>
      </button>
    </div>
  );
}
