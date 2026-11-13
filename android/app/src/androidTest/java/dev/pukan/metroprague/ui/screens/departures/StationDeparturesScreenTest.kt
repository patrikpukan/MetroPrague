package dev.pukan.metroprague.ui.screens.departures

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.FontScale
import androidx.compose.ui.test.ForcedSize
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.then
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.pukan.metroprague.domain.model.Line
import dev.pukan.metroprague.ui.model.DepartureDisplay
import dev.pukan.metroprague.ui.theme.MetroPragueTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class StationDeparturesScreenTest {

    @Test
    fun populatedState_rendersFiveDeparturesAndBackAction() = runComposeUiTest {
        var navigatedBack = false
        val items = (1..5).map { index ->
            StationDepartureItemUiState(
                tripId = "trip-$index",
                destinationName = "Depo Hostivař",
                departure = DepartureDisplay.InMinutes(index * 2),
            )
        }

        setContent {
            MetroPragueTheme {
                StationDeparturesScreen(
                    uiState = StationDeparturesUiState(
                        isLoading = false,
                        stationName = "Muzeum",
                        lineLetter = "A",
                        lineColorHex = Line.A.colorHex,
                        terminusName = "Depo Hostivař",
                        departures = items,
                    ),
                    onNavigateBack = { navigatedBack = true },
                )
            }
        }

        onNodeWithText("Muzeum").assertIsDisplayed()
        onNodeWithTag("StationDeparturesHeader").assertIsDisplayed()
        items.forEach { item ->
            onNodeWithTag("DepartureRow_${item.tripId}").assertIsDisplayed()
        }

        onNodeWithTag("NavigateBackButton").performClick()

        assertTrue(navigatedBack)
    }

    @Test
    fun narrowLayoutWithLargeText_keepsDepartureTimeVisible() = runComposeUiTest {
        setContent {
            MetroPragueTheme {
                DeviceConfigurationOverride(
                    DeviceConfigurationOverride.FontScale(2f) then
                        DeviceConfigurationOverride.ForcedSize(DpSize(280.dp, 640.dp)),
                ) {
                    StationDeparturesScreen(
                        uiState = StationDeparturesUiState(
                            isLoading = false,
                            stationName = "Muzeum",
                            lineLetter = "A",
                            lineColorHex = Line.A.colorHex,
                            terminusName = "Nemocnice Motol",
                            departures = listOf(
                                StationDepartureItemUiState(
                                    tripId = "trip-1",
                                    destinationName = "Nemocnice Motol",
                                    departure = DepartureDisplay.InMinutes(12),
                                ),
                            ),
                        ),
                        onNavigateBack = {},
                    )
                }
            }
        }

        val departureTime = onNodeWithTag("DepartureTime_trip-1")
        departureTime.assertIsDisplayed()
        val layoutResults = mutableListOf<TextLayoutResult>()
        departureTime.fetchSemanticsNode()
            .config[SemanticsActions.GetTextLayoutResult]
            .action
            ?.invoke(layoutResults)
        val layoutResult = layoutResults.single()
        assertEquals(1, layoutResult.lineCount)
        assertTrue(layoutResult.getLineRight(0) <= layoutResult.size.width)
    }

    @Test
    fun emptyState_rendersNoServiceMessage() = runComposeUiTest {
        setContent {
            MetroPragueTheme {
                StationDeparturesScreen(
                    uiState = StationDeparturesUiState(
                        isLoading = false,
                        stationName = "Muzeum",
                        lineLetter = "A",
                        lineColorHex = Line.A.colorHex,
                        terminusName = "Depo Hostivař",
                        departures = emptyList(),
                        emptyState = DepartureDisplay.NoService,
                    ),
                    onNavigateBack = {},
                )
            }
        }

        onNodeWithTag("StationDeparturesEmptyState").assertIsDisplayed()
        onNodeWithText("No service").assertIsDisplayed()
    }
}
