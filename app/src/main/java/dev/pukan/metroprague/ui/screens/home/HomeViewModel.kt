package dev.pukan.metroprague.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.pukan.metroprague.domain.model.Direction
import dev.pukan.metroprague.domain.model.FavoriteKey
import dev.pukan.metroprague.domain.model.Station
import dev.pukan.metroprague.domain.model.directionsAt
import dev.pukan.metroprague.domain.model.forDirection
import dev.pukan.metroprague.domain.repository.DepartureRepository
import dev.pukan.metroprague.domain.repository.FavoritesRepository
import dev.pukan.metroprague.domain.repository.StationRepository
import dev.pukan.metroprague.ui.model.toDisplay
import java.time.Clock
import java.time.Instant
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val favoritesRepository: FavoritesRepository,
    stationRepository: StationRepository,
    private val departureRepository: DepartureRepository,
    private val clock: Clock,
) : ViewModel() {

    val uiState: StateFlow<HomeUiState> = combine(
        favoritesRepository.favorites,
        stationRepository.getStations(),
    ) { favorites, stations ->
        favorites.mapNotNull { key -> key.resolve(stations) }
    }.flatMapLatest { resolvedFavorites ->
        if (resolvedFavorites.isEmpty()) {
            flowOf(HomeUiState(isLoading = false, favorites = emptyList()))
        } else {
            val stationIds = resolvedFavorites.map { it.station.id }.distinct()
            combine(stationIds.map(departureRepository::getDepartureBoard)) { boards ->
                val boardsByStationId = boards.associateBy { it.stationId }
                HomeUiState(
                    isLoading = false,
                    favorites = resolvedFavorites.map { favorite ->
                        val board = boardsByStationId.getValue(favorite.station.id)
                        FavoriteCardUiState(
                            key = favorite.key,
                            stationName = favorite.station.name,
                            lineLetter = favorite.direction.line.name,
                            lineColorHex = favorite.direction.line.colorHex,
                            terminusName = favorite.direction.terminusName,
                            nextDeparture = board
                                .forDirection(favorite.direction)
                                .firstOrNull()
                                .toDisplay(Instant.now(clock), clock.zone),
                        )
                    },
                )
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HomeUiState(),
    )

    fun onRemoveFavorite(key: FavoriteKey) {
        viewModelScope.launch {
            favoritesRepository.remove(key)
        }
    }
}

private data class ResolvedFavorite(
    val key: FavoriteKey,
    val station: Station,
    val direction: Direction,
)

private fun FavoriteKey.resolve(stations: List<Station>): ResolvedFavorite? {
    val station = stations.firstOrNull { it.id == stationId } ?: return null
    val direction = stations.directionsAt(station).firstOrNull {
        it.line == line && it.terminusStationId == terminusStationId
    } ?: return null
    return ResolvedFavorite(key = this, station = station, direction = direction)
}
