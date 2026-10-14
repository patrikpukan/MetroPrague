package dev.pukan.metroprague.data.repository

import dev.pukan.metroprague.data.golemio.MetroSnapshotStore
import dev.pukan.metroprague.domain.model.Station
import dev.pukan.metroprague.domain.repository.StationRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

internal class GolemioStationRepository @Inject constructor(
    private val store: MetroSnapshotStore,
) : StationRepository {
    override fun getStations(): Flow<List<Station>> = store.data
        .map { it.catalogue?.domainStations().orEmpty() }
        .distinctUntilChanged()
}
