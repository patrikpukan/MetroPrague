package dev.pukan.metroprague.ui.screens.departures

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.pukan.metroprague.domain.model.Line
import dev.pukan.metroprague.domain.model.directionsAt
import dev.pukan.metroprague.domain.model.forDirection
import dev.pukan.metroprague.domain.repository.DepartureRepository
import dev.pukan.metroprague.domain.repository.StationRepository
import dev.pukan.metroprague.ui.model.DepartureDisplay
import dev.pukan.metroprague.ui.model.toDisplay
import dev.pukan.metroprague.ui.navigation.StationDeparturesRoute
import java.time.Clock
import java.time.Instant
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class StationDeparturesViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    stationRepository: StationRepository,
    departureRepository: DepartureRepository,
    private val clock: Clock,
) : ViewModel() {

    private val route: StationDeparturesRoute = StationDeparturesRoute.from(savedStateHandle)
    private val selectedLine: Line? = Line.entries.firstOrNull { it.name == route.line }

    val uiState: StateFlow<StationDeparturesUiState> = combine(
        stationRepository.getStations(),
        departureRepository.getDepartureBoard(route.stationId),
    ) { stations, board ->
        val station = stations.firstOrNull { it.id == route.stationId }
        val direction = station?.let { stations.directionsAt(it) }?.firstOrNull {
            it.line == selectedLine && it.terminusStationId == route.terminusStationId
        }

        if (station == null || direction == null) {
            return@combine StationDeparturesUiState(
                isLoading = false,
                stationName = station?.name.orEmpty(),
                lineLetter = selectedLine?.name ?: route.line,
                lineColorHex = selectedLine?.colorHex ?: 0L,
                emptyState = DepartureDisplay.NoService,
            )
        }

        val matchingDepartures = board.forDirection(direction).take(MAX_DEPARTURES)
        val terminusName = matchingDepartures.firstOrNull()?.headsign ?: direction.terminusName

        when {
            board.isStale -> StationDeparturesUiState(
                isLoading = false,
                stationName = station.name,
                lineLetter = direction.line.name,
                lineColorHex = direction.line.colorHex,
                terminusName = terminusName,
                emptyState = DepartureDisplay.Stale,
            )
            board.isUnavailable -> StationDeparturesUiState(
                isLoading = false,
                stationName = station.name,
                lineLetter = direction.line.name,
                lineColorHex = direction.line.colorHex,
                terminusName = terminusName,
                emptyState = DepartureDisplay.Unavailable,
            )
            matchingDepartures.isEmpty() -> StationDeparturesUiState(
                isLoading = false,
                stationName = station.name,
                lineLetter = direction.line.name,
                lineColorHex = direction.line.colorHex,
                terminusName = terminusName,
                emptyState = DepartureDisplay.NoService,
            )
            else -> {
                val now = Instant.now(clock)
                StationDeparturesUiState(
                    isLoading = false,
                    stationName = station.name,
                    lineLetter = direction.line.name,
                    lineColorHex = direction.line.colorHex,
                    terminusName = terminusName,
                    departures = matchingDepartures.map { departure ->
                        StationDepartureItemUiState(
                            tripId = departure.tripId,
                            destinationName = departure.headsign,
                            departure = departure.toDisplay(now, clock.zone),
                        )
                    },
                )
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = StationDeparturesUiState(),
    )

    companion object {
        const val MAX_DEPARTURES = 5
    }
}
