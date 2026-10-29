package dev.pukan.metroprague.ui.screens.settings

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.pukan.metroprague.domain.repository.MetroDataStatus
import dev.pukan.metroprague.domain.repository.MetroReloadError
import dev.pukan.metroprague.ui.theme.MetroPragueTheme
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class MetroDataSettingsTest {
    @Test
    fun reloadButton_callsReloadAndShowsSavedCounts() = runComposeUiTest {
        var calls = 0
        setContent {
            MetroPragueTheme {
                MetroDataSettings(
                    MetroDataStatus(canReload = true, stationCount = 58, departureCount = 900, reloadedAtMillis = 1_000),
                    onReload = { calls++ },
                )
            }
        }
        onNodeWithText("58 stations · 900 saved departures").assertIsDisplayed()
        onNodeWithText("Reload data").performClick()
        runOnIdle { assertEquals(1, calls) }
    }

    @Test
    fun loadingDisablesDuplicateReloadAndShowsProgress() = runComposeUiTest {
        setContent {
            MetroPragueTheme {
                MetroDataSettings(MetroDataStatus(canReload = true, isReloading = true, completedStations = 7, totalStations = 58), {})
            }
        }
        onNodeWithText("Reload data").assertIsNotEnabled()
        onNodeWithText("Loading departures: 7 of 58 stations…").assertIsDisplayed()
    }

    @Test
    fun failureExplainsThatSavedDataWasRetained() = runComposeUiTest {
        setContent {
            MetroPragueTheme {
                MetroDataSettings(MetroDataStatus(canReload = true, error = MetroReloadError.NetworkOrData), {})
            }
        }
        onNodeWithText("Could not reload metro data. Check your connection and try again. Previous saved data was kept.").assertIsDisplayed()
    }
}
