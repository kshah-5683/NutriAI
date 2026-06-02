"use client";

import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { useSupabase } from "@/components/providers/supabase-provider";
import type { UserProfile } from "@/lib/types/domain";

/** Default profile — all fields null/empty, recommendations disabled. */
const DEFAULT_PROFILE: UserProfile = {
  age: null,
  gender: null,
  weightKg: null,
  weightGoal: null,
  dietType: null,
  cuisinePreferences: [],
  allergies: [],
  recommendationsEnabled: false,
};

/**
 * Fetches the user's dietary profile from the user_preferences row.
 * Returns null-safe defaults when no profile has been set.
 *
 * RLS on user_preferences filters by auth.uid() automatically.
 */
export function useUserProfile() {
  const supabase = useSupabase();

  return useQuery<UserProfile>({
    queryKey: ["user-profile"],
    queryFn: async () => {
      const { data, error } = await supabase
        .from("user_preferences")
        .select(
          "age, gender, weight_kg, weight_goal, diet_type, cuisine_preferences, allergies, recommendations_enabled"
        )
        .maybeSingle();

      if (error) {
        console.warn("[useUserProfile] Query failed, using defaults:", error.message);
        return DEFAULT_PROFILE;
      }

      if (!data) {
        return DEFAULT_PROFILE;
      }

      return {
        age: data.age ?? null,
        gender: data.gender ?? null,
        weightKg: data.weight_kg ?? null,
        weightGoal: data.weight_goal ?? null,
        dietType: data.diet_type ?? null,
        cuisinePreferences: data.cuisine_preferences ?? [],
        allergies: data.allergies ?? [],
        recommendationsEnabled: data.recommendations_enabled ?? false,
      };
    },
    // Profile changes rarely — cache for 5 minutes
    staleTime: 5 * 60_000,
  });
}

/**
 * Mutation to update profile columns in user_preferences.
 * Only updates profile fields — does NOT touch macro goal columns.
 *
 * Uses upsert with onConflict: "user_id" so it creates the row if none
 * exists (new user who hasn't saved macro goals yet). Macro goal columns
 * use their Postgres DEFAULT values in that case.
 */
export function useUpdateProfile() {
  const supabase = useSupabase();
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async (profile: UserProfile) => {
      const {
        data: { user },
      } = await supabase.auth.getUser();
      if (!user) throw new Error("Not authenticated");

      const { error } = await supabase.from("user_preferences").upsert(
        {
          user_id: user.id,
          age: profile.age,
          gender: profile.gender,
          weight_kg: profile.weightKg,
          weight_goal: profile.weightGoal,
          diet_type: profile.dietType,
          cuisine_preferences: profile.cuisinePreferences,
          allergies: profile.allergies,
          recommendations_enabled: profile.recommendationsEnabled,
        },
        { onConflict: "user_id" }
      );

      if (error) throw error;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["user-profile"] });
      // Also invalidate recommendations so they refetch with new profile
      queryClient.invalidateQueries({ queryKey: ["recommendations"] });
    },
  });
}
