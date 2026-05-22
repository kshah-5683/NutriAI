package com.app.nutriai.presentation.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.app.nutriai.presentation.screens.auth.AuthScreen
import com.app.nutriai.presentation.screens.catalog.CatalogScreen
import com.app.nutriai.presentation.screens.home.HomeScreen
import com.app.nutriai.presentation.screens.insights.InsightsScreen
import com.app.nutriai.presentation.screens.log.LogScreen

/**
 * Data class representing a bottom navigation item.
 */
data class BottomNavItem(
    val label: String,
    val icon: ImageVector,
    val route: String
)

/**
 * Main navigation host for the app. Sets up bottom navigation
 * and the NavHost with all screen destinations.
 *
 * Phase 3: ViewModels are injected via hiltViewModel() in each composable.
 * Bottom navigation bar hides on non-tab screens (e.g., Log screen).
 * Catalog redesign: Log screen accepts optional catalogType for direct add from Catalog tabs.
 * Phase 6: AuthScreen receives [onNavigateToHome] callback — on successful sign-in the
 *          Profile tab navigates the user to the Home dashboard.
 */
@Composable
fun NutriAiNavHost() {
    val navController = rememberNavController()

    val bottomNavItems = listOf(
        BottomNavItem(
            label = "Home",
            icon = Icons.Default.Home,
            route = Screen.Home.route
        ),
        BottomNavItem(
            label = "My Foods",
            icon = Icons.Default.Restaurant,
            route = Screen.Catalog.route
        ),
        BottomNavItem(
            label = "Insights",
            icon = Icons.Default.BarChart,
            route = Screen.Insights.route
        ),
        BottomNavItem(
            label = "Profile",
            icon = Icons.Default.Person,
            route = Screen.Auth.route
        )
    )

    // Routes where the bottom bar should be visible
    val bottomBarRoutes = setOf(
        Screen.Home.route,
        Screen.Catalog.route,
        Screen.Insights.route,
        Screen.Auth.route
    )

    Scaffold(
        bottomBar = {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentDestination = navBackStackEntry?.destination
            val showBottomBar = currentDestination?.route in bottomBarRoutes

            AnimatedVisibility(
                visible = showBottomBar,
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it })
            ) {
                NavigationBar {
                    bottomNavItems.forEach { item ->
                        NavigationBarItem(
                            icon = { Icon(item.icon, contentDescription = item.label) },
                            label = { Text(item.label) },
                            selected = currentDestination?.hierarchy?.any {
                                it.route == item.route
                            } == true,
                            onClick = {
                                navController.navigate(item.route) {
                                    // Pop up to the start destination to avoid building up
                                    // a large back stack of destinations
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    // Avoid multiple copies of the same destination
                                    launchSingleTop = true
                                    // Restore state when re-selecting a previously selected item
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Home.route) {
                HomeScreen(
                    onNavigateToLog = {
                        navController.navigate(Screen.Log.createRoute())
                    }
                )
            }
            composable(
                route = Screen.Log.route,
                arguments = listOf(
                    navArgument(Screen.Log.ARG_CATALOG_TYPE) {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    }
                )
            ) { backStackEntry ->
                val catalogType = backStackEntry.arguments?.getString(Screen.Log.ARG_CATALOG_TYPE)
                LogScreen(
                    catalogType = catalogType,
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }
            composable(Screen.Catalog.route) {
                CatalogScreen(
                    onNavigateToAddItem = { catalogType ->
                        navController.navigate(Screen.Log.createRoute(catalogType))
                    }
                )
            }
            composable(Screen.Insights.route) {
                InsightsScreen()
            }
            composable(Screen.Auth.route) {
                AuthScreen(
                    onNavigateToHome = {
                        navController.navigate(Screen.Home.route) {
                            // Pop up to start destination so back doesn't return to Auth
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    }
}
