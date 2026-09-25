package dev.pukan.metroprague.ui.screens

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dev.pukan.metroprague.ui.navigation.BottomNavigationBar
import dev.pukan.metroprague.ui.navigation.HomeRoute
import dev.pukan.metroprague.ui.navigation.SearchRoute
import dev.pukan.metroprague.ui.navigation.SettingsRoute
import dev.pukan.metroprague.ui.screens.home.HomeScreenRoute
import dev.pukan.metroprague.ui.screens.search.SearchScreenRoute
import dev.pukan.metroprague.ui.screens.settings.SettingsScreenRoute

@Composable
fun MainScreen() {
    val navController = rememberNavController()

    Scaffold(
        bottomBar = { BottomNavigationBar(navController) }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = HomeRoute,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable<HomeRoute> {
                HomeScreenRoute(
                    onNavigateToSearch = {
                        navController.navigate(SearchRoute) {
                            launchSingleTop = true
                        }
                    },
                )
            }
            composable<SearchRoute> { SearchScreenRoute() }
            composable<SettingsRoute> { SettingsScreenRoute() }
        }
    }
}
