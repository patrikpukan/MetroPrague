package dev.pukan.metroprague.ui.screens.search

import dev.pukan.metroprague.domain.model.DepartureBoard
import dev.pukan.metroprague.domain.model.FavoriteKey
import dev.pukan.metroprague.domain.model.Line
import dev.pukan.metroprague.domain.model.LinePosition
import dev.pukan.metroprague.domain.model.Station
import dev.pukan.metroprague.domain.repository.DepartureRepository
import dev.pukan.metroprague.domain.repository.FavoritesRepository
import dev.pukan.metroprague.domain.repository.StationRepository
import dev.pukan.metroprague.ui.model.DepartureDisplay
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SearchViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val fixedClock = Clock.fixed(
        Instant.parse("2026-08-05T10:00:00Z"),
        ZoneId.of("Europe/Prague"),
    )
    private val nemocniceMotol = Station(
        id = "nemocnice-motol",
        name = "Nemocnice Motol",
        lines = listOf(LinePosition(Line.A, 0)),
    )
    private val depoHostivar = Station(
        id = "depo-hostivar",
        name = "Depo Hostivař",
        lines = listOf(LinePosition(Line.A, 16)),
    )
    private val letnany = Station(
        id = "letnany",
        name = "Letňany",
        lines = listOf(LinePosition(Line.C, 0)),
    )
    private val haje = Station(
        id = "haje",
        name = "Háje",
        lines = listOf(LinePosition(Line.C, 19)),
    )
    private val muzeum = Station(
        id = "muzeum",
        name = "Muzeum",
        lines = listOf(LinePosition(Line.A, 9), LinePosition(Line.C, 9)),
    )
    private val stations = listOf(nemocniceMotol, depoHostivar, letnany, haje, muzeum)

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `sheet is null initially`() = runTest {
        val viewModel = viewModel()
        collectSheetState(viewModel)

        assertNull(viewModel.sheetState.value)
    }

    @Test
    fun `selecting Muzeum exposes four directions and dismissing clears sheet`() = runTest {
        val viewModel = viewModel()
        collectSheetState(viewModel)

        viewModel.onStationSelected(muzeum)

        val sheetState = checkNotNull(viewModel.sheetState.value)
        assertEquals("Muzeum", sheetState.stationName)
        assertEquals(4, sheetState.directions.size)
        assertEquals(
            setOf("Nemocnice Motol", "Depo Hostivař", "Letňany", "Háje"),
            sheetState.directions.map { it.direction.terminusName }.toSet(),
        )

        viewModel.onSheetDismissed()

        assertNull(viewModel.sheetState.value)
    }

    @Test
    fun `empty board gives no service for every direction`() = runTest {
        val viewModel = viewModel()
        collectSheetState(viewModel)

        viewModel.onStationSelected(muzeum)

        assertEquals(
            List(4) { DepartureDisplay.NoService },
            checkNotNull(viewModel.sheetState.value).directions.map { it.nextDeparture },
        )
    }

    @Test
    fun `toggling direction adds and removes exact favorite and updates row state`() = runTest {
        val favoritesRepository = FakeFavoritesRepository()
        val viewModel = viewModel(favoritesRepository)
        collectSheetState(viewModel)
        viewModel.onStationSelected(muzeum)
        val direction = checkNotNull(viewModel.sheetState.value)
            .directions
            .first { it.direction.line == Line.A && it.direction.terminusStationId == "depo-hostivar" }
            .direction
        val expected = FavoriteKey(
            stationId = "muzeum",
            line = Line.A,
            terminusStationId = "depo-hostivar",
        )

        assertFalse(rowFor(viewModel, direction.terminusStationId).isFavorite)

        viewModel.onToggleFavorite(direction)

        assertEquals(listOf(expected), favoritesRepository.favorites.value)
        assertTrue(rowFor(viewModel, direction.terminusStationId).isFavorite)

        viewModel.onToggleFavorite(direction)

        assertEquals(emptyList<FavoriteKey>(), favoritesRepository.favorites.value)
        assertFalse(rowFor(viewModel, direction.terminusStationId).isFavorite)
    }

    private fun kotlinx.coroutines.test.TestScope.collectSheetState(viewModel: SearchViewModel) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.sheetState.collect {}
        }
    }

    private fun rowFor(viewModel: SearchViewModel, terminusStationId: String): DirectionRowUiState =
        checkNotNull(viewModel.sheetState.value).directions.first {
            it.direction.terminusStationId == terminusStationId
        }

    private fun viewModel(
        favoritesRepository: FavoritesRepository = FakeFavoritesRepository(),
    ) = SearchViewModel(
        stationRepository = object : StationRepository {
            override fun getStations(): Flow<List<Station>> = flowOf(stations)
        },
        departureRepository = object : DepartureRepository {
            override fun getDepartureBoard(stationId: String): Flow<DepartureBoard> = flowOf(
                DepartureBoard(stationId = stationId, departures = emptyList()),
            )
        },
        favoritesRepository = favoritesRepository,
        clock = fixedClock,
    )

    private class FakeFavoritesRepository : FavoritesRepository {
        override val favorites = MutableStateFlow<List<FavoriteKey>>(emptyList())

        override suspend fun add(key: FavoriteKey) {
            if (key !in favorites.value) {
                favorites.value += key
            }
        }

        override suspend fun remove(key: FavoriteKey) {
            favorites.value = favorites.value.filterNot { it == key }
        }
    }
}
