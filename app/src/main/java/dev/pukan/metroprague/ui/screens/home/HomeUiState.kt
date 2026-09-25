package dev.pukan.metroprague.ui.screens.home

import dev.pukan.metroprague.domain.model.FavoriteKey
import dev.pukan.metroprague.ui.model.DepartureDisplay

data class HomeUiState(
    val isLoading: Boolean = true,
    val favorites: List<FavoriteCardUiState> = emptyList(),
)

data class FavoriteCardUiState(
    val key: FavoriteKey,
    val stationName: String,
    val lineLetter: String,
    val lineColorHex: Long,
    val terminusName: String,
    val nextDeparture: DepartureDisplay,
)
