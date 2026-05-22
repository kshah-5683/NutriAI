package com.app.nutriai.presentation.navigation

/**
 * Sealed class defining all navigation destinations in the app.
 * Each screen has a unique route string used by the NavHost.
 *
 * Phase 3: Static routes for Home, Log, Catalog, Auth.
 * Catalog redesign: Log screen accepts optional catalogType argument
 * to route saves to the correct catalog (recipe vs ingredient).
 */
sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object Log : Screen("log?catalogType={catalogType}") {
        /** Base route without arguments — used for default navigation from Home FAB */
        const val BASE_ROUTE = "log"
        const val ARG_CATALOG_TYPE = "catalogType"
        /** Build a route with catalog type for direct add from Catalog screen */
        fun createRoute(catalogType: String? = null): String {
            return if (catalogType != null) "log?catalogType=$catalogType" else "log"
        }
    }
    data object Catalog : Screen("catalog")
    data object Auth : Screen("auth")
    data object Insights : Screen("insights")
}
