package dev.pukan.metroprague.ui.screens.departures

import androidx.lifecycle.SavedStateHandle
import dev.pukan.metroprague.domain.model.Departure
import dev.pukan.metroprague.domain.model.DepartureBoard
import dev.pukan.metroprague.domain.model.Line
import dev.pukan.metroprague.domain.model.LinePosition
import dev.pukan.metroprague.domain.model.Station
import dev.pukan.metroprague.domain.repository.DepartureRepository
import dev.pukan.metroprague.domain.repository.StationRepository
import dev.pukan.metroprague.ui.model.DepartureDisplay
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class StationDeparturesViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val now = Instant.parse("2026-08-05T10:00:00Z")
    private val fixedClock = Clock.fixed(now, ZoneId.of("Europe/Prague"))

    private val stations = listOf(
        Station("nemocnice-motol", "Nemocnice Motol", listOf(LinePosition(Line.A, 0))),
        Station("muzeum", "Muzeum", listOf(LinePosition(Line.A, 9), LinePosition(Line.C, 9))),
        Station("depo-hostivar", "Depo Hostivař", listOf(LinePosition(Line.A, 16))),
        Station("letnany", "Letňany", listOf(LinePosition(Line.C, 0))),
        Station("haje", "Háje", listOf(LinePosition(Line.C, 19))),
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `exposes at most five upcoming departures for chosen station and direction`() = runTest {
        val departures = (1..7).map { index ->
            Departure(
                tripId = "trip-a-$index",
                line = Line.A,
                headsign = "Depo Hostivař",
                scheduled = now.plusSeconds(index * 120L),
                predicted = now.plusSeconds(index * 120L),
                delaySeconds = 0,
                isAtStop = false,
                isCanceled = false,
                directionTerminusStationId = "depo-hostivar",
            )
        } + Departure(
            tripId = "trip-c-1",
            line = Line.C,
            headsign = "Háje",
            scheduled = now.plusSeconds(60L),
            predicted = now.plusSeconds(60L),
            delaySeconds = 0,
            isAtStop = false,
            isCanceled = false,
            directionTerminusStationId = "haje",
        )

        val viewModel = viewModel(
            stationId = "muzeum",
            line = Line.A.name,
            terminusStationId = "depo-hostivar",
            board = DepartureBoard(stationId = "muzeum", departures = departures),
        )
        collectUiState(viewModel)

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals("Muzeum", state.stationName)
        assertEquals("A", state.lineLetter)
        assertEquals("Depo Hostivař", state.terminusName)
        assertNull(state.emptyState)
        assertEquals(5, state.departures.size)
        assertEquals(
            listOf("trip-a-1", "trip-a-2", "trip-a-3", "trip-a-4", "trip-a-5"),
            state.departures.map { it.tripId },
        )
        assertEquals(
            listOf(
                DepartureDisplay.InMinutes(2),
                DepartureDisplay.InMinutes(4),
                DepartureDisplay.InMinutes(6),
                DepartureDisplay.InMinutes(8),
                DepartureDisplay.InMinutes(10),
            ),
            state.departures.map { it.departure },
        )
    }

    @Test
    fun `empty board for direction exposes no service empty state`() = runTest {
        val viewModel = viewModel(
            stationId = "muzeum",
            line = Line.A.name,
            terminusStationId = "depo-hostivar",
            board = DepartureBoard(stationId = "muzeum", departures = emptyList()),
        )
        collectUiState(viewModel)

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals("Muzeum", state.stationName)
        assertEquals(emptyList<StationDepartureItemUiState>(), state.departures)
        assertEquals(DepartureDisplay.NoService, state.emptyState)
    }

    @Test
    fun `stale board exposes stale empty state`() = runTest {
        val viewModel = viewModel(
            stationId = "muzeum",
            line = Line.A.name,
            terminusStationId = "depo-hostivar",
            board = DepartureBoard(
                stationId = "muzeum",
                departures = emptyList(),
                isStale = true,
            ),
        )
        collectUiState(viewModel)

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(emptyList<StationDepartureItemUiState>(), state.departures)
        assertEquals(DepartureDisplay.Stale, state.emptyState)
    }

    @Test
    fun `unknown station exposes no service empty state`() = runTest {
        val viewModel = viewModel(
            stationId = "unknown",
            line = Line.A.name,
            terminusStationId = "depo-hostivar",
            board = DepartureBoard(stationId = "unknown", departures = emptyList()),
        )
        collectUiState(viewModel)

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(emptyList<StationDepartureItemUiState>(), state.departures)
        assertEquals(DepartureDisplay.NoService, state.emptyState)
    }

    private fun kotlinx.coroutines.test.TestScope.collectUiState(
        viewModel: StationDeparturesViewModel,
    ) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }
    }

    private fun viewModel(
        stationId: String,
        line: String,
        terminusStationId: String,
        board: DepartureBoard,
    ) = StationDeparturesViewModel(
        savedStateHandle = SavedStateHandle(
            mapOf(
                "stationId" to stationId,
                "line" to line,
                "terminusStationId" to terminusStationId,
            ),
        ),
        stationRepository = object : StationRepository {
            override fun getStations(): Flow<List<Station>> = flowOf(stations)
        },
        departureRepository = object : DepartureRepository {
            override fun getDepartureBoard(stationId: String): Flow<DepartureBoard> = flowOf(board)
        },
        clock = fixedClock,
    )
}
