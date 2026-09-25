package dev.pukan.metroprague.domain.model

import java.time.Instant

/**
 * Mirrors Golemio's trip.id, route.short_name, trip.headsign,
 * departure_timestamp.scheduled, departure_timestamp.predicted, delay.seconds,
 * trip.is_at_stop, and trip.is_canceled fields.
 */
data class Departure(
    val tripId: String,
    val line: Line,
    val headsign: String,
    val scheduled: Instant,
    val predicted: Instant?,
    val delaySeconds: Int?,
    val isAtStop: Boolean,
    val isCanceled: Boolean,
) {
    val effectiveTime: Instant get() = predicted ?: scheduled
}

data class DepartureBoard(
    val stationId: String,
    val departures: List<Departure>,
    val infoTexts: List<String> = emptyList(),
)

fun DepartureBoard.forDirection(direction: Direction): List<Departure> {
    // Real data contains short-turn trips whose headsign is not a line terminus. The
    // position-based fallback for those trips belongs in this function.
    return departures.filter {
        it.line == direction.line && it.headsign == direction.terminusName
    }
}
