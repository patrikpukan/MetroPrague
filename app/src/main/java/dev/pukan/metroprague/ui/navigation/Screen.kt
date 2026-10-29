package dev.pukan.metroprague.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.SavedStateHandle
import dev.pukan.metroprague.domain.model.Line
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

@Serializable
data class StationDeparturesRoute(
    val stationId: String,
    val line: String,
    val terminusStationId: String,
) {
    constructor(
        stationId: String,
        line: Line,
        terminusStationId: String,
    ) : this(
        stationId = stationId,
        line = line.name,
        terminusStationId = terminusStationId,
    )

    companion object {
        fun from(savedStateHandle: SavedStateHandle): StationDeparturesRoute =
            StationDeparturesRoute(
                stationId = checkNotNull(savedStateHandle.get<String>("stationId")),
                line = checkNotNull(savedStateHandle.get<String>("line")),
                terminusStationId = checkNotNull(savedStateHandle.get<String>("terminusStationId")),
            )
    }
}
