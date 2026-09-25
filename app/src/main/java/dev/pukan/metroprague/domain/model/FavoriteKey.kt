package dev.pukan.metroprague.domain.model

data class FavoriteKey(
    val stationId: String,
    val line: Line,
    val terminusStationId: String,
)
