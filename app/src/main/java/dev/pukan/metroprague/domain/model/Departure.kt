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
    val directionTerminusStationId: String? = null,
) {
    val effectiveTime: Instant get() = predicted ?: scheduled
}

data class DepartureBoard(
    val stationId: String,
    val departures: List<Departure>,
    val infoTexts: List<String> = emptyList(),
    val isUnavailable: Boolean = false,
)

fun DepartureBoard.forDirection(direction: Direction): List<Departure> {
    return departures.filter {
        it.line == direction.line && if (it.directionTerminusStationId == null) {
            it.headsign == direction.terminusName
        } else {
            it.directionTerminusStationId == direction.terminusStationId
        }
    }
}
