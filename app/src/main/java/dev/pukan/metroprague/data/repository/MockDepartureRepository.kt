package dev.pukan.metroprague.data.repository

import dev.pukan.metroprague.domain.model.Departure
import dev.pukan.metroprague.domain.model.DepartureBoard
import dev.pukan.metroprague.domain.model.Direction
import dev.pukan.metroprague.domain.model.Station
import dev.pukan.metroprague.domain.model.directionsAt
import dev.pukan.metroprague.domain.repository.DepartureRepository
import dev.pukan.metroprague.domain.repository.StationRepository
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.absoluteValue
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import javax.inject.Inject

class MockDepartureRepository @Inject constructor(
    private val stationRepository: StationRepository,
    private val clock: Clock,
) : DepartureRepository {

    override fun getDepartureBoard(stationId: String): Flow<DepartureBoard> = flow {
        val allStations = stationRepository.getStations().first()
        val station = allStations.firstOrNull { it.id == stationId }

        while (currentCoroutineContext().isActive) {
            val board = station?.let {
                buildDepartureBoard(
                    now = Instant.now(clock),
                    station = it,
                    allStations = allStations,
                    zoneId = clock.zone,
                )
            } ?: DepartureBoard(stationId = stationId, departures = emptyList())

            emit(board)
            delay(REFRESH_INTERVAL_MILLIS)
        }
    }
}

internal fun buildDepartureBoard(
    now: Instant,
    station: Station,
    allStations: List<Station>,
    zoneId: ZoneId,
): DepartureBoard {
    val localNow = now.atZone(zoneId)
    val secondsSinceMidnight = localNow.toLocalTime().toSecondOfDay()
    val headwaySeconds = headwaySecondsAt(secondsSinceMidnight)
        ?: return DepartureBoard(station.id, emptyList())

    val departures = allStations.directionsAt(station)
        .flatMap { direction ->
            buildDirectionDepartures(
                now = now,
                localDate = localNow.toLocalDate(),
                station = station,
                direction = direction,
                headwaySeconds = headwaySeconds,
                secondsSinceMidnight = secondsSinceMidnight,
                zoneId = zoneId,
            )
        }
        .sortedBy { it.effectiveTime }

    return DepartureBoard(station.id, departures)
}

private fun buildDirectionDepartures(
    now: Instant,
    localDate: LocalDate,
    station: Station,
    direction: Direction,
    headwaySeconds: Int,
    secondsSinceMidnight: Int,
    zoneId: ZoneId,
): List<Departure> {
    val offsetSeconds = (station.id + direction.terminusStationId)
        .hashCode()
        .absoluteValue % headwaySeconds
    val secondsToFirstDeparture = Math.floorMod(
        offsetSeconds - secondsSinceMidnight,
        headwaySeconds,
    ).let { if (it == 0) headwaySeconds else it }
    val firstDepartureSeconds = secondsSinceMidnight + secondsToFirstDeparture

    return (1..DEPARTURES_PER_DIRECTION).mapNotNull { index ->
        val scheduledSeconds = firstDepartureSeconds.toLong() +
            (index - 1L) * headwaySeconds
        val localScheduled = localDate
            .atStartOfDay()
            .plusSeconds(scheduledSeconds)

        if (isNoServiceTime(localScheduled.toLocalTime().toSecondOfDay())) {
            return@mapNotNull null
        }

        val scheduled = localScheduled.atZone(zoneId).toInstant()
        val tripNumber = scheduledSeconds / headwaySeconds.toLong() + 1
        val delaySeconds = if (tripNumber % DELAY_TRIP_INTERVAL == 0L) {
            DELAY_SECONDS
        } else {
            0
        }
        val predicted = scheduled.plusSeconds(delaySeconds.toLong())
        val secondsUntilDeparture = Duration.between(now, predicted).seconds

        Departure(
            tripId = "${station.id}-${direction.terminusStationId}-$index",
            line = direction.line,
            headsign = direction.terminusName,
            scheduled = scheduled,
            predicted = predicted,
            delaySeconds = delaySeconds,
            isAtStop = secondsUntilDeparture in 0..AT_STOP_THRESHOLD_SECONDS,
            isCanceled = false,
        )
    }
}

private fun headwaySecondsAt(secondsSinceMidnight: Int): Int? = when (secondsSinceMidnight) {
    in NO_SERVICE_START_SECONDS until SERVICE_START_SECONDS -> null
    in MORNING_PEAK_START_SECONDS until MORNING_PEAK_END_SECONDS,
    in AFTERNOON_PEAK_START_SECONDS until AFTERNOON_PEAK_END_SECONDS,
    -> PEAK_HEADWAY_SECONDS
    in OFF_PEAK_START_SECONDS until AFTERNOON_PEAK_START_SECONDS,
    in EVENING_OFF_PEAK_START_SECONDS until EVENING_OFF_PEAK_END_SECONDS,
    -> OFF_PEAK_HEADWAY_SECONDS
    else -> NIGHT_HEADWAY_SECONDS
}

private fun isNoServiceTime(secondsSinceMidnight: Int): Boolean =
    secondsSinceMidnight in NO_SERVICE_START_SECONDS until SERVICE_START_SECONDS

private const val REFRESH_INTERVAL_MILLIS = 15_000L
private const val DEPARTURES_PER_DIRECTION = 3
private const val DELAY_TRIP_INTERVAL = 5
private const val DELAY_SECONDS = 60
private const val AT_STOP_THRESHOLD_SECONDS = 30L

private const val NO_SERVICE_START_SECONDS = 30 * 60
private const val SERVICE_START_SECONDS = 4 * 60 * 60 + 30 * 60
private const val MORNING_PEAK_START_SECONDS = 7 * 60 * 60
private const val MORNING_PEAK_END_SECONDS = 9 * 60 * 60
private const val OFF_PEAK_START_SECONDS = MORNING_PEAK_END_SECONDS
private const val AFTERNOON_PEAK_START_SECONDS = 15 * 60 * 60
private const val AFTERNOON_PEAK_END_SECONDS = 19 * 60 * 60
private const val EVENING_OFF_PEAK_START_SECONDS = AFTERNOON_PEAK_END_SECONDS
private const val EVENING_OFF_PEAK_END_SECONDS = 20 * 60 * 60

private const val PEAK_HEADWAY_SECONDS = 2 * 60
private const val OFF_PEAK_HEADWAY_SECONDS = 5 * 60
private const val NIGHT_HEADWAY_SECONDS = 8 * 60
