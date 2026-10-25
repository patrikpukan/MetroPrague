package dev.pukan.metroprague.data.repository

import dev.pukan.metroprague.data.golemio.GolemioBoardService
import dev.pukan.metroprague.data.golemio.GolemioCatalogueLoader
import dev.pukan.metroprague.data.golemio.GolemioDepartureBoardParser
import dev.pukan.metroprague.data.golemio.GolemioHttpException
import dev.pukan.metroprague.data.golemio.MetroDataSnapshot
import dev.pukan.metroprague.data.golemio.MetroSnapshotStore
import dev.pukan.metroprague.data.golemio.SavedDepartureBoard
import dev.pukan.metroprague.di.ApplicationScope
import dev.pukan.metroprague.di.LiveDeparturesConfig
import dev.pukan.metroprague.domain.repository.MetroDataRepository
import dev.pukan.metroprague.domain.repository.MetroDataStatus
import dev.pukan.metroprague.domain.repository.MetroReloadError
import java.time.Clock
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex

internal class GolemioMetroDataRepository @Inject constructor(
    private val store: MetroSnapshotStore,
    private val loader: GolemioCatalogueLoader,
    private val boards: GolemioBoardService,
    private val parser: GolemioDepartureBoardParser,
    private val clock: Clock,
    private val config: LiveDeparturesConfig,
    @param:ApplicationScope private val scope: CoroutineScope,
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
        // Owned by the app scope so leaving Settings does not discard a reload in progress.
        // Cancelling the caller only stops waiting; the lock is released when the work itself ends.
        val job = scope.launch(Dispatchers.IO) { download() }
        job.invokeOnCompletion {
            progress.update { it.copy(isReloading = false) }
            reloadMutex.unlock()
        }
        job.join()
    }

    private suspend fun download() {
        try {
            val catalogue = loader.load()
            val savedBoards = linkedMapOf<String, SavedDepartureBoard>()
            var departureCount = 0
            progress.update { it.copy(totalStations = catalogue.stations.size) }
            catalogue.stations.forEachIndexed { index, station ->
                val ids = catalogue.platforms.filter { it.stationId == station.id }.map { it.id }
                require(ids.isNotEmpty())
                val response = boards.getDepartures(ids)
                // Departures without a mapped direction still match favorites by headsign.
                departureCount += parser.parse(station.id, response, catalogue).departures.size
                savedBoards[station.id] = SavedDepartureBoard(response, clock.millis())
                progress.update { it.copy(completedStations = index + 1) }
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
            progress.update { it.copy(error = error) }
        }
    }
}
