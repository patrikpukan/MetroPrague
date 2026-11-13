package dev.pukan.metroprague.ui.screens.home

import dev.pukan.metroprague.domain.model.Departure
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
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val now = Instant.parse("2026-08-05T10:00:00Z")
    private val fixedClock = Clock.fixed(now, ZoneId.of("Europe/Prague"))
    private val stations = listOf(
        Station("nemocnice-motol", "Nemocnice Motol", listOf(LinePosition(Line.A, 0))),
        Station("muzeum", "Muzeum", listOf(LinePosition(Line.A, 9))),
        Station("depo-hostivar", "Depo Hostivař", listOf(LinePosition(Line.A, 16))),
        Station("zlicin", "Zličín", listOf(LinePosition(Line.B, 0))),
        Station("florenc", "Florenc", listOf(LinePosition(Line.B, 14))),
        Station("cerny-most", "Černý Most", listOf(LinePosition(Line.B, 23))),
    )
    private val muzeumFavorite = FavoriteKey("muzeum", Line.A, "depo-hostivar")
    private val florencFavorite = FavoriteKey("florenc", Line.B, "zlicin")

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `no favorites finishes loading with empty list`() = runTest {
        val viewModel = viewModel(FakeFavoritesRepository())
        collectUiState(viewModel)

        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals(emptyList<FavoriteCardUiState>(), viewModel.uiState.value.favorites)
    }

    @Test
    fun `two favorites resolve names and preserve stored order`() = runTest {
        val favorites = FakeFavoritesRepository(listOf(florencFavorite, muzeumFavorite))
        val viewModel = viewModel(
            favoritesRepository = favorites,
            boards = mapOf(
                "florenc" to board("florenc", Line.B, "Zličín"),
                "muzeum" to board("muzeum", Line.A, "Depo Hostivař"),
            ),
        )
        collectUiState(viewModel)

        assertEquals(listOf("Florenc", "Muzeum"), viewModel.uiState.value.favorites.map { it.stationName })
        assertEquals(listOf("Zličín", "Depo Hostivař"), viewModel.uiState.value.favorites.map { it.terminusName })
        assertEquals(
            listOf(DepartureDisplay.InMinutes(3), DepartureDisplay.InMinutes(3)),
            viewModel.uiState.value.favorites.map { it.nextDeparture },
        )
    }

    @Test
    fun `removing favorite removes its card`() = runTest {
        val favorites = FakeFavoritesRepository(listOf(muzeumFavorite))
        val viewModel = viewModel(
            favoritesRepository = favorites,
            boards = mapOf("muzeum" to board("muzeum", Line.A, "Depo Hostivař")),
        )
        collectUiState(viewModel)

        viewModel.onRemoveFavorite(muzeumFavorite)

        assertEquals(emptyList<FavoriteCardUiState>(), viewModel.uiState.value.favorites)
    }

    @Test
    fun `unknown station favorite is dropped while valid sibling survives`() = runTest {
        val unknown = FavoriteKey("unknown", Line.A, "depo-hostivar")
        val viewModel = viewModel(
            favoritesRepository = FakeFavoritesRepository(listOf(unknown, muzeumFavorite)),
            boards = mapOf("muzeum" to board("muzeum", Line.A, "Depo Hostivař")),
        )
        collectUiState(viewModel)

        assertEquals(listOf(muzeumFavorite), viewModel.uiState.value.favorites.map { it.key })
    }

    @Test
    fun `board without departures displays no service`() = runTest {
        val viewModel = viewModel(
            favoritesRepository = FakeFavoritesRepository(listOf(muzeumFavorite)),
            boards = mapOf("muzeum" to DepartureBoard("muzeum", emptyList())),
        )
        collectUiState(viewModel)

        assertEquals(
            DepartureDisplay.NoService,
            viewModel.uiState.value.favorites.single().nextDeparture,
        )
    }

    private fun kotlinx.coroutines.test.TestScope.collectUiState(viewModel: HomeViewModel) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }
    }

    private fun viewModel(
        favoritesRepository: FavoritesRepository,
        boards: Map<String, DepartureBoard> = emptyMap(),
    ) = HomeViewModel(
        favoritesRepository = favoritesRepository,
        stationRepository = object : StationRepository {
            override fun getStations(): Flow<List<Station>> = flowOf(stations)
        },
        departureRepository = object : DepartureRepository {
            override fun getDepartureBoard(stationId: String): Flow<DepartureBoard> = flowOf(
                boards.getValue(stationId),
            )
        },
        clock = fixedClock,
    )

    private fun board(stationId: String, line: Line, headsign: String) = DepartureBoard(
        stationId = stationId,
        departures = listOf(
            Departure(
                tripId = "$stationId-$headsign",
                line = line,
                headsign = headsign,
                scheduled = now.plusSeconds(180),
                predicted = now.plusSeconds(180),
                delaySeconds = 0,
                isAtStop = false,
                isCanceled = false,
            ),
        ),
    )

    private class FakeFavoritesRepository(
        initialFavorites: List<FavoriteKey> = emptyList(),
    ) : FavoritesRepository {
        override val favorites = MutableStateFlow(initialFavorites)

        override suspend fun add(key: FavoriteKey) {
            if (key !in favorites.value) {
                favorites.value += key
            }
        }

        override suspend fun remove(key: FavoriteKey) {
            favorites.value = favorites.value.filterNot { it == key }
        }

        override suspend fun toggle(key: FavoriteKey) {
            favorites.value = if (key in favorites.value) {
                favorites.value.filterNot { it == key }
            } else {
                favorites.value + key
            }
        }
    }
}
