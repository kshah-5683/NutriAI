"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { DashboardShell } from "@/components/dashboard-shell";
import { CatalogFoodCard } from "@/components/catalog-food-card";
import { EditFoodSheet } from "@/components/edit-food-sheet";
import { ConfirmDeleteFoodDialog } from "@/components/confirm-delete-food-dialog";
import { useCatalogItems } from "@/lib/hooks/use-catalog-items";
import { useCatalogStore, type CatalogTab } from "@/lib/stores/catalog-store";
import { useLogFormStore } from "@/lib/stores/log-form-store";
import { getIngredientCatalogId, getRecipeCatalogId } from "@/lib/utils/constants";
import { useSupabase } from "@/components/providers/supabase-provider";

const TABS: { key: CatalogTab; label: string; icon: string }[] = [
  { key: "recipes", label: "Recipes", icon: "🍲" },
  { key: "ingredients", label: "Ingredients", icon: "🥚" },
];

/**
 * Catalog page — Dual-tab (Recipes/Ingredients), search, edit, delete.
 * Port of Android's CatalogScreen + CatalogViewModel.
 * Wrapped in DashboardShell for consistent header + bottom navigation.
 */
export default function CatalogPage() {
  const supabase = useSupabase();
  const router = useRouter();
  const [userId, setUserId] = useState<string | null>(null);

  const selectedTab = useCatalogStore((s) => s.selectedTab);
  const searchQuery = useCatalogStore((s) => s.searchQuery);
  const editingFood = useCatalogStore((s) => s.editingFood);
  const deletingFood = useCatalogStore((s) => s.deletingFood);
  const setSelectedTab = useCatalogStore((s) => s.setSelectedTab);
  const setSearchQuery = useCatalogStore((s) => s.setSearchQuery);
  const setEditingFood = useCatalogStore((s) => s.setEditingFood);
  const setDeletingFood = useCatalogStore((s) => s.setDeletingFood);

  const setInputMode = useLogFormStore((s) => s.setInputMode);
  const toggleRecipeMode = useLogFormStore((s) => s.toggleRecipeMode);

  /** Pre-set the log form store before navigating so the correct mode is active on arrival. */
  const handleAddClick = () => {
    if (selectedTab === "recipes") {
      setInputMode("manual");
      toggleRecipeMode(true);
    } else {
      toggleRecipeMode(false);
    }
    router.push("/log");
  };

  useEffect(() => {
    supabase.auth.getUser().then(({ data }) => {
      setUserId(data.user?.id ?? null);
    });
  }, [supabase]);

  const catalogId = userId
    ? selectedTab === "recipes"
      ? getRecipeCatalogId(userId)
      : getIngredientCatalogId(userId)
    : null;

  const { data: items = [], isLoading } = useCatalogItems(catalogId, searchQuery);

  return (
    <DashboardShell>
      <div className="px-4 pt-4 pb-2">
        {/* Header */}
        <h1
          className="text-xl font-bold"
          style={{ color: "var(--text-primary)" }}
        >
          My Foods
        </h1>
        <p
          className="mt-0.5 text-sm"
          style={{ color: "var(--text-secondary)" }}
        >
          Your saved recipes and ingredients
        </p>

        {/* Tabs */}
        <div
          className="mt-3 flex rounded-md border"
          style={{ borderColor: "var(--border-variant)" }}
        >
          {TABS.map(({ key, label, icon }) => (
            <button
              key={key}
              onClick={() => setSelectedTab(key)}
              className="flex flex-1 items-center justify-center gap-1.5 py-2.5 text-sm font-medium transition-colors"
              style={{
                backgroundColor:
                  selectedTab === key ? "var(--color-primary-container)" : "transparent",
                color:
                  selectedTab === key ? "var(--color-primary)" : "var(--text-secondary)",
              }}
            >
              <span>{icon}</span>
              {label}
            </button>
          ))}
        </div>

        {/* Search */}
        <div className="relative mt-3">
          <svg
            className="absolute left-3 top-1/2 -translate-y-1/2"
            width="16"
            height="16"
            viewBox="0 0 24 24"
            fill="none"
          >
            <circle
              cx="11"
              cy="11"
              r="8"
              stroke="var(--text-secondary)"
              strokeWidth="2"
            />
            <path
              d="M21 21L16.65 16.65"
              stroke="var(--text-secondary)"
              strokeWidth="2"
              strokeLinecap="round"
            />
          </svg>
          <input
            type="text"
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            placeholder={`Search ${selectedTab}...`}
            className="w-full rounded-md border py-2.5 pl-9 pr-9 text-sm outline-none transition-colors focus:border-[var(--color-primary)]"
            style={{
              backgroundColor: "var(--bg-surface)",
              borderColor: "var(--border-variant)",
              color: "var(--text-primary)",
            }}
          />
          {searchQuery && (
            <button
              onClick={() => setSearchQuery("")}
              className="absolute right-3 top-1/2 -translate-y-1/2 text-xs"
              style={{ color: "var(--text-secondary)" }}
              aria-label="Clear search"
            >
              ✕
            </button>
          )}
        </div>
      </div>

      {/* Content */}
      <div className="flex-1 px-4 pb-20">
        {isLoading ? (
          <div className="flex items-center justify-center py-20">
            <div
              className="h-8 w-8 animate-spin rounded-full border-2 border-t-transparent"
              style={{ borderColor: "var(--color-primary)", borderTopColor: "transparent" }}
            />
          </div>
        ) : items.length === 0 ? (
          <EmptyState isSearching={!!searchQuery.trim()} tab={selectedTab} />
        ) : (
          <div className="mt-3 space-y-2">
            <p className="text-xs" style={{ color: "var(--text-secondary)" }}>
              {items.length} {selectedTab}
            </p>
            {items.map((food) => (
              <CatalogFoodCard
                key={food.id}
                food={food}
                isRecipe={selectedTab === "recipes"}
                onEdit={setEditingFood}
                onDelete={setDeletingFood}
              />
            ))}
          </div>
        )}
      </div>

      {/* FAB — add new item via log page */}
      <div className="fixed bottom-20 right-4 z-10">
        <button
          onClick={handleAddClick}
          className="flex h-14 w-14 items-center justify-center rounded-full shadow-lg transition-transform hover:scale-105 active:scale-95"
          style={{ backgroundColor: "var(--color-primary)", color: "#FFFFFF" }}
          aria-label={`Add ${selectedTab === "recipes" ? "recipe" : "ingredient"}`}
        >
          <svg width="28" height="28" viewBox="0 0 24 24" fill="none">
            <path
              d="M12 5V19M5 12H19"
              stroke="currentColor"
              strokeWidth="2.5"
              strokeLinecap="round"
              strokeLinejoin="round"
            />
          </svg>
        </button>
      </div>

      {/* Edit + Delete modals */}
      <EditFoodSheet food={editingFood} onClose={() => setEditingFood(null)} />
      <ConfirmDeleteFoodDialog food={deletingFood} onClose={() => setDeletingFood(null)} />
    </DashboardShell>
  );
}

function EmptyState({ isSearching, tab }: { isSearching: boolean; tab: CatalogTab }) {
  return (
    <div className="flex flex-col items-center justify-center px-4 py-16 text-center">
      <span className="mb-3 text-4xl">{tab === "recipes" ? "🍲" : "🥚"}</span>
      <h2
        className="text-base font-semibold"
        style={{ color: "var(--text-primary)" }}
      >
        {isSearching
          ? `No ${tab} found`
          : tab === "recipes"
            ? "No recipes yet"
            : "No ingredients yet"}
      </h2>
      <p
        className="mt-1 text-sm"
        style={{ color: "var(--text-secondary)", opacity: 0.7 }}
      >
        {isSearching
          ? "Try a different search term"
          : tab === "recipes"
            ? "Recipes you log will appear here. Tap + to add one."
            : "Ingredients you log will appear here. Tap + to add one."}
      </p>
    </div>
  );
}
