package dev.pukan.metroprague.data.repository

import dev.pukan.metroprague.data.golemio.GolemioBoardService
import dev.pukan.metroprague.data.golemio.GolemioCatalogueLoader
import dev.pukan.metroprague.data.golemio.GolemioDepartureBoardParser
import dev.pukan.metroprague.data.golemio.GolemioHttpException
import dev.pukan.metroprague.data.golemio.MetroDataSnapshot
import dev.pukan.metroprague.data.golemio.MetroSnapshotStore
import dev.pukan.metroprague.data.golemio.SavedDepartureBoard
import dev.pukan.metroprague.di.LiveDeparturesConfig
import dev.pukan.metroprague.domain.repository.MetroDataRepository
import dev.pukan.metroprague.domain.repository.MetroDataStatus
import dev.pukan.metroprague.domain.repository.MetroReloadError
import java.time.Clock
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.sync.Mutex

internal class GolemioMetroDataRepository @Inject constructor(
    private val store: MetroSnapshotStore,
    private val loader: GolemioCatalogueLoader,
    private val boards: GolemioBoardService,
    private val parser: GolemioDepartureBoardParser,
    private val clock: Clock,
    private val config: LiveDeparturesConfig,
) : MetroDataRepository {
    private val reloadMutex = Mutex()
    private val progress = MutableStateFlow(MetroDataStatus(canReload = config.useLiveDepartures))

    override val status = combine(store.data, progress) { saved, current ->
        current.copy(
            stationCount = saved.catalogue?.stations?.size ?: 0,
            departureCount = saved.departureCount,
            reloadedAtMillis = saved.reloadedAtMillis,
        )
    }

    override suspend fun reload() {
        if (!config.useLiveDepartures || !reloadMutex.tryLock()) return
        progress.value = MetroDataStatus(canReload = true, isReloading = true)
        try {
            val catalogue = loader.load()
            val savedBoards = linkedMapOf<String, SavedDepartureBoard>()
            var departureCount = 0
            progress.value = progress.value.copy(totalStations = catalogue.stations.size)
            catalogue.stations.forEachIndexed { index, station ->
                val ids = catalogue.platforms.filter { it.stationId == station.id }.map { it.id }
                require(ids.isNotEmpty())
                val response = boards.getDepartures(ids)
                val parsed = parser.parse(station.id, response, catalogue)
                require(parsed.departures.all { it.directionTerminusStationId != null }) { "Unmapped departure direction" }
                departureCount += parsed.departures.size
                savedBoards[station.id] = SavedDepartureBoard(response, clock.millis())
                progress.value = progress.value.copy(completedStations = index + 1)
            }
            // One atomic write: a failed/cancelled reload never replaces part of the previous data.
            store.replace(MetroDataSnapshot(catalogue, savedBoards, clock.millis(), departureCount))
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (exception: Exception) {
            val error = when ((exception as? GolemioHttpException)?.statusCode) {
                401, 403 -> MetroReloadError.Authentication
                429 -> MetroReloadError.RateLimited
                else -> MetroReloadError.NetworkOrData
            }
            progress.value = progress.value.copy(error = error)
        } finally {
            progress.value = progress.value.copy(isReloading = false)
            reloadMutex.unlock()
        }
    }
}
