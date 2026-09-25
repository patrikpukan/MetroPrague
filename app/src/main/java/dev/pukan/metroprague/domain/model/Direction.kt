package dev.pukan.metroprague.domain.model

data class Direction(
    val line: Line,
    val terminusStationId: String,
    val terminusName: String,
)
