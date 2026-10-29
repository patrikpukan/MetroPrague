package dev.pukan.metroprague.data.repository

import dev.pukan.metroprague.data.golemio.GolemioDepartureBoardParser
import dev.pukan.metroprague.data.golemio.MetroSnapshotStore
import dev.pukan.metroprague.domain.model.DepartureBoard
import dev.pukan.metroprague.domain.repository.DepartureRepository
import java.time.Clock
import java.time.Duration
import java.time.Instant
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow

/** Reads only persisted snapshots. The ticker updates time labels; it never calls the API. */
internal class GolemioDepartureRepository @Inject constructor(
    private val store: MetroSnapshotStore,
    private val parser: GolemioDepartureBoardParser,
    private val clock: Clock,
) : DepartureRepository {
    override fun getDepartureBoard(stationId: String): Flow<DepartureBoard> = combine(
        store.data,
        flow {
            while (true) {
                emit(Instant.now(clock))
                delay(15_000)
            }
        },
    ) { snapshot, now ->
        val saved = snapshot.boards[stationId]
        val catalogue = snapshot.catalogue
        when {
            // Nothing saved yet: prompt for a reload rather than reporting an outage.
            catalogue == null -> DepartureBoard(stationId, emptyList(), isUnavailable = true, isStale = true)
            saved == null -> unavailable(stationId)
            else -> try {
                val fetchedAt = Instant.ofEpochMilli(saved.fetchedAtMillis)
                val stale = Duration.between(fetchedAt, now).seconds !in 0..299
                val board = parser.parse(stationId, saved.response, catalogue)
                board.copy(
                    departures = board.departures.filter {
                        !it.effectiveTime.isBefore(now) || it.isAtStop && !stale
                    },
                    fetchedAt = fetchedAt,
                    isStale = stale,
                )
            } catch (_: Exception) {
                unavailable(stationId)
            }
        }
    }.catch { emit(unavailable(stationId)) }

    private fun unavailable(stationId: String) = DepartureBoard(stationId, emptyList(), isUnavailable = true)
}
