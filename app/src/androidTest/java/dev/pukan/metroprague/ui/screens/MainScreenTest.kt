package dev.pukan.metroprague.ui.screens

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dev.pukan.metroprague.HiltComponentActivity
import dev.pukan.metroprague.ui.theme.MetroPragueTheme
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class MainScreenTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<HiltComponentActivity>()

    @Before
    fun setUp() {
        hiltRule.inject()
    }

    @Test
    fun bottomNavigation_showsHomeByDefault() {
        composeRule.setContent {
            MetroPragueTheme {
                MainScreen()
            }
        }

        composeRule.onNodeWithText("Home Screen - Favorites").assertExists()
    }

    @Test
    fun bottomNavigation_navigatesToSearch() {
        composeRule.setContent {
            MetroPragueTheme {
                MainScreen()
            }
        }

        composeRule.onNodeWithText("Search").performClick()

        composeRule.onNodeWithTag("SearchScreen").assertExists()
        composeRule.onNodeWithText("Home Screen - Favorites").assertDoesNotExist()
    }

    @Test
    fun bottomNavigation_navigatesToSettings() {
        composeRule.setContent {
            MetroPragueTheme {
                MainScreen()
            }
        }

        composeRule.onNodeWithText("Settings").performClick()

        composeRule.onNodeWithText("Appearance").assertExists()
        composeRule.onNodeWithText("Home Screen - Favorites").assertDoesNotExist()
    }
}
