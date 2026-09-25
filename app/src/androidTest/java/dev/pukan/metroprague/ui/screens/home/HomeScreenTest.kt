package dev.pukan.metroprague.ui.screens.home

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.pukan.metroprague.domain.model.FavoriteKey
import dev.pukan.metroprague.domain.model.Line
import dev.pukan.metroprague.ui.model.DepartureDisplay
import dev.pukan.metroprague.ui.theme.MetroPragueTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class HomeScreenTest {

    @Test
    fun emptyState_rendersMessageAndActionNavigates() = runComposeUiTest {
        var navigated = false
        setContent {
            MetroPragueTheme {
                HomeScreen(
                    uiState = HomeUiState(isLoading = false),
                    onRemoveFavorite = {},
                    onNavigateToSearch = { navigated = true },
                )
            }
        }

        onNodeWithText("No favorites yet").assertIsDisplayed()
        onNodeWithText("Search stations").performClick()

        assertTrue(navigated)
    }

    @Test
    fun populatedState_rendersCardPerFavoriteAndRemovesExactKey() = runComposeUiTest {
        val firstKey = FavoriteKey("muzeum", Line.A, "depo-hostivar")
        val secondKey = FavoriteKey("florenc", Line.B, "zlicin")
        var removedKey: FavoriteKey? = null
        setContent {
            MetroPragueTheme {
                HomeScreen(
                    uiState = HomeUiState(
                        isLoading = false,
                        favorites = listOf(
                            favorite(firstKey, "Muzeum", "Depo Hostivař"),
                            favorite(secondKey, "Florenc", "Zličín"),
                        ),
                    ),
                    onRemoveFavorite = { removedKey = it },
                    onNavigateToSearch = {},
                )
            }
        }

        onNodeWithTag("FavoriteCard_muzeum-A-depo-hostivar").assertIsDisplayed()
        onNodeWithTag("FavoriteCard_florenc-B-zlicin").assertIsDisplayed()
        onAllNodesWithContentDescription("Remove from favorites")[0].performClick()

        assertEquals(firstKey, removedKey)
    }

    private fun favorite(
        key: FavoriteKey,
        stationName: String,
        terminusName: String,
    ) = FavoriteCardUiState(
        key = key,
        stationName = stationName,
        lineLetter = key.line.name,
        lineColorHex = key.line.colorHex,
        terminusName = terminusName,
        nextDeparture = DepartureDisplay.InMinutes(2),
    )
}
