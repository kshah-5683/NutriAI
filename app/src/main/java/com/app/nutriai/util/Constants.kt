package com.app.nutriai.util

/**
 * App-wide constants.
 */
object Constants {
    // Database
    const val DATABASE_NAME = "nutriai_database"
    const val DATABASE_VERSION = 7

    // Sync
    // Reduced from 12h to 24h (Phase 8 Pre-work II): push-on-write + foreground pull handle
    // the happy path; the periodic job is now a safety-net backstop only.
    const val SYNC_INTERVAL_HOURS = 24L
    const val CACHE_EVICTION_DAYS = 90L

    /** WorkManager unique work name for the periodic background sync job. */
    const val SYNC_WORK_TAG = "nutriai_sync_work"

    // API
    const val GEMINI_BASE_URL = "https://generativelanguage.googleapis.com/"

    /** USDA FoodData Central — primary online nutrition source. */
    const val USDA_FDC_BASE_URL = "https://api.nal.usda.gov/"

    /** Nutrition values are stored and computed per this many grams (food-label convention). */
    const val PER_100G_BASE = 100.0

    // Local User (pre-auth placeholder — used for all local-only data)
    const val LOCAL_USER_ID = "local_user"

    // Catalogs — separate catalogs for recipes and ingredients
    const val RECIPE_CATALOG_ID = "local_user_recipes"
    const val RECIPE_CATALOG_NAME = "Recipes"
    const val INGREDIENT_CATALOG_ID = "local_user_ingredients"
    const val INGREDIENT_CATALOG_NAME = "Ingredients"

    // Phase 6 — Supabase Auth & Sync
    /** DataStore preferences file name for persisting the auth session. */
    const val AUTH_PREFS_NAME = "auth_prefs"

    /**
     * Supabase remote catalog ID prefix.
     * Local catalog IDs like "local_user_recipes" are not globally unique —
     * two users would collide in the Supabase PRIMARY KEY.  We prefix them
     * with the Supabase user UUID so every row is unique across users.
     *
     * Format:  "{supabaseUserId}_{localCatalogId}"
     * Example: "abc-uuid_local_user_recipes"
     */
    const val REMOTE_CATALOG_ID_SEPARATOR = "_"
}
