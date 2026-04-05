package dev.pukan.metroprague.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import kotlinx.serialization.Serializable

val navigationItems = listOf(
    NavigationItem(HomeRoute, "Home", Icons.Default.Home),
    NavigationItem(SearchRoute, "Search", Icons.Default.Search),
    NavigationItem(SettingsRoute, "Settings", Icons.Default.Settings)
)

data class NavigationItem<T : Any>(
    val route: T,
    val title: String,
    val icon: ImageVector
)

@Serializable
data object HomeRoute

@Serializable
data object SearchRoute

@Serializable
data object SettingsRoute
