"use client";

import { useQuery } from "@tanstack/react-query";
import { useSupabase } from "@/components/providers/supabase-provider";
import type { FoodItem } from "@/lib/types/domain";

/**
 * Maps a Supabase food_items row (snake_case) to a FoodItem domain object (camelCase).
 */
function mapRow(row: Record<string, unknown>): FoodItem {
  return {
    id: row.id as string,
    catalogId: row.catalog_id as string,
    name: row.name as string,
    brand: (row.brand as string) ?? null,
    baseServingG: row.base_serving_g as number,
    baseCalories: row.base_calories as number,
    baseProtein: row.base_protein as number,
    baseCarbs: row.base_carbs as number,
    baseFat: row.base_fat as number,
    externalApiId: (row.external_api_id as string) ?? null,
    lastModifiedAt: row.last_modified_at as number,
    deletedAt: (row.deleted_at as number) ?? null,
  };
}

/**
 * TanStack Query hook — fetches food_items for a given catalog ID.
 *
 * Supports optional search query (server-side ILIKE).
 * RLS on food_items filters by auth.uid() through the catalogs FK.
 *
 * CRITICAL: Every query includes .is("deleted_at", null) to exclude tombstones.
 */
export function useCatalogItems(catalogId: string | null, searchQuery: string) {
  const supabase = useSupabase();

  return useQuery<FoodItem[]>({
    queryKey: ["catalog-items", catalogId, searchQuery],
    queryFn: async () => {
      if (!catalogId) return [];

      let query = supabase
        .from("food_items")
        .select("*")
        .eq("catalog_id", catalogId)
        .is("deleted_at", null)
        .order("name", { ascending: true });

      if (searchQuery.trim()) {
        query = query.ilike("name", `%${searchQuery.trim()}%`);
      }

      const { data, error } = await query;

      if (error) throw error;
      return (data ?? []).map(mapRow);
    },
    enabled: !!catalogId,
  });
}
