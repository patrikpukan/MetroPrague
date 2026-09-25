package dev.pukan.metroprague.ui.screens.search

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.pukan.metroprague.domain.model.Direction
import dev.pukan.metroprague.domain.model.Line
import dev.pukan.metroprague.domain.model.LinePosition
import dev.pukan.metroprague.domain.model.Station
import dev.pukan.metroprague.ui.model.DepartureDisplay
import dev.pukan.metroprague.ui.theme.MetroPragueTheme
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class SearchScreenTest {

    private val station = Station(
        id = "muzeum",
        name = "Muzeum",
        lines = listOf(LinePosition(Line.A, 9), LinePosition(Line.C, 9)),
    )

    @Test
    fun sheetState_showsSheetAndOneNodePerDirection() = runComposeUiTest {
        val directions = listOf(
            directionRow(Line.A, "nemocnice-motol", "Nemocnice Motol"),
            directionRow(Line.C, "haje", "Háje"),
        )
        setContent {
            MetroPragueTheme {
                SearchScreen(
                    searchQuery = "",
                    selectedLine = null,
                    stations = listOf(station),
                    sheetState = DirectionSheetUiState("Muzeum", directions),
                    onSearchQueryChange = {},
                    onLineFilterChange = {},
                    onStationSelected = {},
                    onSheetDismissed = {},
                    onToggleFavorite = {},
                )
            }
        }

        onNodeWithTag("DirectionPickerSheet").assertIsDisplayed()
        onAllNodesWithTag("DirectionRow_A_nemocnice-motol").assertCountEquals(1)
        onAllNodesWithTag("DirectionRow_C_haje").assertCountEquals(1)
    }

    @Test
    fun stationItemClick_invokesStationSelected() = runComposeUiTest {
        var selectedStation: Station? = null
        setContent {
            MetroPragueTheme {
                SearchScreen(
                    searchQuery = "",
                    selectedLine = null,
                    stations = listOf(station),
                    sheetState = null,
                    onSearchQueryChange = {},
                    onLineFilterChange = {},
                    onStationSelected = { selectedStation = it },
                    onSheetDismissed = {},
                    onToggleFavorite = {},
                )
            }
        }

        onNodeWithTag("StationItem_muzeum").performClick()

        assertEquals(station, selectedStation)
    }

    private fun directionRow(
        line: Line,
        terminusStationId: String,
        terminusName: String,
    ) = DirectionRowUiState(
        direction = Direction(line, terminusStationId, terminusName),
        lineLetter = line.name,
        lineColorHex = line.colorHex,
        nextDeparture = DepartureDisplay.NoService,
        isFavorite = false,
    )
}
