package dev.pukan.metroprague.ui.screens.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.pukan.metroprague.domain.model.Direction
import dev.pukan.metroprague.domain.model.FavoriteKey
import dev.pukan.metroprague.domain.model.Line
import dev.pukan.metroprague.domain.model.Station
import dev.pukan.metroprague.domain.model.directionsAt
import dev.pukan.metroprague.domain.model.forDirection
import dev.pukan.metroprague.domain.repository.DepartureRepository
import dev.pukan.metroprague.domain.repository.FavoritesRepository
import dev.pukan.metroprague.domain.repository.StationRepository
import dev.pukan.metroprague.ui.model.DepartureDisplay
import dev.pukan.metroprague.ui.model.toDisplay
import java.time.Clock
import java.time.Instant
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DirectionSheetUiState(
    val stationName: String,
    val directions: List<DirectionRowUiState>,
)

data class DirectionRowUiState(
    val direction: Direction,
    val lineLetter: String,
    val lineColorHex: Long,
    val nextDeparture: DepartureDisplay,
    val isFavorite: Boolean,
)

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val stationRepository: StationRepository,
    private val departureRepository: DepartureRepository,
    private val favoritesRepository: FavoritesRepository,
    private val clock: Clock,
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _selectedLine = MutableStateFlow<Line?>(null)
    val selectedLine: StateFlow<Line?> = _selectedLine

    private val _selectedStationId = MutableStateFlow<String?>(null)

    val filteredStations: StateFlow<List<Station>> = combine(
        stationRepository.getStations(),
        _searchQuery,
        _selectedLine,
    ) { stations, query, line ->
        stations.filter { station ->
            val matchesLine = line == null || station.lines.any { it.line == line }
            val matchesQuery = station.name.contains(query, ignoreCase = true)
            matchesLine && matchesQuery
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList(),
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    val sheetState: StateFlow<DirectionSheetUiState?> = _selectedStationId
        .flatMapLatest { stationId ->
            if (stationId == null) {
                flowOf(null)
            } else {
                combine(
                    stationRepository.getStations(),
                    departureRepository.getDepartureBoard(stationId),
                    favoritesRepository.favorites,
                ) { stations, board, favorites ->
                    val station = stations.firstOrNull { it.id == stationId }
                        ?: return@combine null
                    DirectionSheetUiState(
                        stationName = station.name,
                        directions = stations.directionsAt(station).map { direction ->
                            DirectionRowUiState(
                                direction = direction,
                                lineLetter = direction.line.name,
                                lineColorHex = direction.line.colorHex,
                                nextDeparture = board
                                    .forDirection(direction)
                                    .firstOrNull()
                                    .toDisplay(Instant.now(clock), clock.zone),
                                isFavorite = FavoriteKey(
                                    stationId = station.id,
                                    line = direction.line,
                                    terminusStationId = direction.terminusStationId,
                                ) in favorites,
                            )
                        },
                    )
                }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null,
        )

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun onLineFilterChange(line: Line?) {
        if (_selectedLine.value == line) {
            _selectedLine.value = null
        } else {
            _selectedLine.value = line
        }
    }

    fun onStationSelected(station: Station) {
        _selectedStationId.value = station.id
    }

    fun onSheetDismissed() {
        _selectedStationId.value = null
    }

    fun onToggleFavorite(direction: Direction) {
        val stationId = _selectedStationId.value ?: return
        val key = FavoriteKey(
            stationId = stationId,
            line = direction.line,
            terminusStationId = direction.terminusStationId,
        )
        val isDisplayedDirection = sheetState.value
            ?.directions
            ?.any { it.direction == direction }
            ?: false
        if (!isDisplayedDirection) return

        viewModelScope.launch {
            favoritesRepository.toggle(key)
        }
    }
}
