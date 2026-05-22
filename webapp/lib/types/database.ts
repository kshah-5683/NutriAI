/**
 * Supabase generated database types.
 *
 * TODO: Replace this stub with generated types by running:
 *   npx supabase gen types typescript --project-id YOUR_PROJECT_ID > lib/types/database.ts
 *
 * Until then, this stub satisfies TypeScript imports so development can proceed.
 * All queries will be typed as `any` until the real types are generated.
 */

export type Json =
  | string
  | number
  | boolean
  | null
  | { [key: string]: Json | undefined }
  | Json[];

export type Database = {
  public: {
    Tables: {
      catalogs: {
        Row: {
          id: string;
          user_id: string;
          name: string;
          last_modified_at: number;
          deleted_at: number | null;
          updated_at: string;
        };
        Insert: {
          id: string;
          user_id: string;
          name: string;
          last_modified_at: number;
          deleted_at?: number | null;
          updated_at?: string;
        };
        Update: {
          id?: string;
          user_id?: string;
          name?: string;
          last_modified_at?: number;
          deleted_at?: number | null;
          updated_at?: string;
        };
      };
      food_items: {
        Row: {
          id: string;
          catalog_id: string;
          name: string;
          brand: string | null;
          base_serving_g: number;
          base_calories: number;
          base_protein: number;
          base_carbs: number;
          base_fat: number;
          external_api_id: string | null;
          /** Always true on web — web writes go directly to Supabase */
          is_synced: boolean;
          last_modified_at: number;
          deleted_at: number | null;
          updated_at: string;
        };
        Insert: {
          id: string;
          catalog_id: string;
          name: string;
          brand?: string | null;
          base_serving_g: number;
          base_calories: number;
          base_protein: number;
          base_carbs: number;
          base_fat: number;
          external_api_id?: string | null;
          is_synced?: boolean;
          last_modified_at: number;
          deleted_at?: number | null;
          updated_at?: string;
        };
        Update: {
          id?: string;
          catalog_id?: string;
          name?: string;
          brand?: string | null;
          base_serving_g?: number;
          base_calories?: number;
          base_protein?: number;
          base_carbs?: number;
          base_fat?: number;
          external_api_id?: string | null;
          is_synced?: boolean;
          last_modified_at?: number;
          deleted_at?: number | null;
          updated_at?: string;
        };
      };
      daily_logs: {
        Row: {
          id: string;
          user_id: string;
          food_item_id: string | null;
          food_name: string;
          date_timestamp: number;
          consumed_qty: number;
          consumed_unit: string;
          total_calories: number;
          total_protein: number;
          total_carbs: number;
          total_fat: number;
          /** Always true on web — web writes go directly to Supabase */
          is_synced: boolean;
          last_modified_at: number;
          deleted_at: number | null;
          updated_at: string;
        };
        Insert: {
          id: string;
          user_id: string;
          food_item_id?: string | null;
          food_name: string;
          date_timestamp: number;
          consumed_qty: number;
          consumed_unit: string;
          total_calories: number;
          total_protein: number;
          total_carbs: number;
          total_fat: number;
          is_synced?: boolean;
          last_modified_at: number;
          deleted_at?: number | null;
          updated_at?: string;
        };
        Update: {
          id?: string;
          user_id?: string;
          food_item_id?: string | null;
          food_name?: string;
          date_timestamp?: number;
          consumed_qty?: number;
          consumed_unit?: string;
          total_calories?: number;
          total_protein?: number;
          total_carbs?: number;
          total_fat?: number;
          is_synced?: boolean;
          last_modified_at?: number;
          deleted_at?: number | null;
          updated_at?: string;
        };
      };
      user_preferences: {
        Row: {
          /** user_id is the primary key — one row per user */
          user_id: string;
          calorie_goal: number;
          protein_goal: number;
          carbs_goal: number;
          fat_goal: number;
          updated_at: string;
        };
        Insert: {
          user_id: string;
          calorie_goal?: number;
          protein_goal?: number;
          carbs_goal?: number;
          fat_goal?: number;
          updated_at?: string;
        };
        Update: {
          user_id?: string;
          calorie_goal?: number;
          protein_goal?: number;
          carbs_goal?: number;
          fat_goal?: number;
          updated_at?: string;
        };
      };
      ifct_foods: {
        Row: {
          /** code is the primary key — e.g. "G001", "P001" */
          code: string;
          name: string;
          energy_kcal: number;
          protein_g: number;
          fat_g: number;
          carbs_g: number;
          fiber_g: number;
        };
        Insert: {
          code: string;
          name: string;
          energy_kcal?: number;
          protein_g?: number;
          fat_g?: number;
          carbs_g?: number;
          fiber_g?: number;
        };
        Update: {
          code?: string;
          name?: string;
          energy_kcal?: number;
          protein_g?: number;
          fat_g?: number;
          carbs_g?: number;
          fiber_g?: number;
        };
      };
    };
    Views: Record<string, never>;
    Functions: Record<string, never>;
    Enums: Record<string, never>;
  };
};
