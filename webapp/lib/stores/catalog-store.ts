"use client";

import { create } from "zustand";
import type { FoodItem } from "@/lib/types/domain";

/**
 * Catalog tabs — matches Android CatalogTab enum.
 */
export type CatalogTab = "recipes" | "ingredients";

interface CatalogStoreState {
  selectedTab: CatalogTab;
  searchQuery: string;
  editingFood: FoodItem | null;
  deletingFood: FoodItem | null;

  setSelectedTab: (tab: CatalogTab) => void;
  setSearchQuery: (query: string) => void;
  setEditingFood: (food: FoodItem | null) => void;
  setDeletingFood: (food: FoodItem | null) => void;
}

/**
 * Zustand store for Catalog page UI state.
 * Port of Android CatalogViewModel state (selectedTab, searchQuery, editSheet, pendingDelete).
 *
 * Switching tabs clears the search query — matches Android behavior.
 */
export const useCatalogStore = create<CatalogStoreState>((set) => ({
  selectedTab: "recipes",
  searchQuery: "",
  editingFood: null,
  deletingFood: null,

  setSelectedTab: (tab) => set({ selectedTab: tab, searchQuery: "" }),
  setSearchQuery: (query) => set({ searchQuery: query }),
  setEditingFood: (food) => set({ editingFood: food }),
  setDeletingFood: (food) => set({ deletingFood: food }),
}));
