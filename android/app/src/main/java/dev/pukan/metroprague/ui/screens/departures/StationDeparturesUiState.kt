package dev.pukan.metroprague.ui.screens.departures

import dev.pukan.metroprague.ui.model.DepartureDisplay

data class StationDeparturesUiState(
    val isLoading: Boolean = true,
    val stationName: String = "",
    val lineLetter: String = "",
    val lineColorHex: Long = 0L,
    val terminusName: String = "",
    val departures: List<StationDepartureItemUiState> = emptyList(),
    val emptyState: DepartureDisplay? = null,
)

data class StationDepartureItemUiState(
    val tripId: String,
    val destinationName: String,
    val departure: DepartureDisplay,
)
