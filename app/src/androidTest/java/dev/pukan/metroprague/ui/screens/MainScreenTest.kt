package dev.pukan.metroprague.ui.screens

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.pukan.metroprague.ui.theme.MetroPragueTheme
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class MainScreenTest {

    @Test
    fun bottomNavigation_showsHomeByDefault() = runComposeUiTest {
        setContent {
            MetroPragueTheme {
                MainScreen()
            }
        }

        onNodeWithText("Home Screen - Favorites").assertExists()
    }

    @Test
    fun bottomNavigation_navigatesToSearch() = runComposeUiTest {
        setContent {
            MetroPragueTheme {
                MainScreen()
            }
        }

        onNodeWithText("Search").performClick()

        onNodeWithTag("SearchScreen").assertExists()
        onNodeWithText("Home Screen - Favorites").assertDoesNotExist()
    }

    @Test
    fun bottomNavigation_navigatesToSettings() = runComposeUiTest {
        setContent {
            MetroPragueTheme {
                MainScreen()
            }
        }

        onNodeWithText("Settings").performClick()

        onNodeWithText("Appearance").assertExists()
        onNodeWithText("Home Screen - Favorites").assertDoesNotExist()
    }
}
