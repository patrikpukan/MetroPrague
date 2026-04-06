package dev.pukan.metroprague.ui.screens

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.pukan.metroprague.ui.theme.MetroPragueTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun bottomNavigation_showsHomeByDefault() {
        composeTestRule.setContent {
            MetroPragueTheme {
                MainScreen()
            }
        }

        // Verify Home is displayed
        composeTestRule.onNodeWithText("Home Screen - Favorites").assertExists()
    }

    @Test
    fun bottomNavigation_navigatesToSearch() {
        composeTestRule.setContent {
            MetroPragueTheme {
                MainScreen()
            }
        }

        // Click Search tab (using content description of the icon or the text label)
        composeTestRule.onNodeWithText("Search").performClick()

        // Verify Search is displayed
        composeTestRule.onNodeWithTag("SearchScreen").assertExists()
        // Home should not be displayed
        composeTestRule.onNodeWithText("Home Screen - Favorites").assertDoesNotExist()
    }

    @Test
    fun bottomNavigation_navigatesToSettings() {
        composeTestRule.setContent {
            MetroPragueTheme {
                MainScreen()
            }
        }

        // Click Settings tab
        composeTestRule.onNodeWithText("Settings").performClick()

        // Verify Settings is displayed
        composeTestRule.onNodeWithText("Settings Screen - Preferences").assertExists()
        // Home should not be displayed
        composeTestRule.onNodeWithText("Home Screen - Favorites").assertDoesNotExist()
    }
}
