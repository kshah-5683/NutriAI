import { create } from "zustand";

/**
 * Zustand store for the selected date.
 * Used by DateNavigationHeader, use-daily-logs hook, and the dashboard page.
 *
 * HYDRATION SAFETY: This store initializes with `new Date()` which differs
 * between server and client. It must ONLY be consumed in 'use client' components.
 * Never import in Server Components.
 */

interface DateState {
  selectedDate: Date;
  goToNextDay: () => void;
  goToPrevDay: () => void;
  goToToday: () => void;
  setDate: (date: Date) => void;
}

export const useDateStore = create<DateState>((set) => ({
  selectedDate: new Date(),

  goToNextDay: () =>
    set((state) => {
      const next = new Date(state.selectedDate);
      next.setDate(next.getDate() + 1);
      return { selectedDate: next };
    }),

  goToPrevDay: () =>
    set((state) => {
      const prev = new Date(state.selectedDate);
      prev.setDate(prev.getDate() - 1);
      return { selectedDate: prev };
    }),

  goToToday: () => set({ selectedDate: new Date() }),

  setDate: (date: Date) => set({ selectedDate: date }),
}));
