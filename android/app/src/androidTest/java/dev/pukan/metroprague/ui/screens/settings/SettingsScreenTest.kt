package dev.pukan.metroprague.ui.screens.settings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.filterToOne
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.pukan.metroprague.domain.model.ThemePreference
import dev.pukan.metroprague.ui.theme.MetroPragueTheme
import org.junit.Test
import org.junit.runner.RunWith

private fun hasRole(role: Role): SemanticsMatcher =
    SemanticsMatcher.expectValue(SemanticsProperties.Role, role)

@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class SettingsScreenTest {

    private fun ComposeUiTest.setStatelessContent(state: SettingsUiState = SettingsUiState()) {
        setContent {
            MetroPragueTheme {
                SettingsScreen(
                    state = state,
                    onOpenThemeSelection = {},
                    onSelectTheme = {},
                    onOpenLanguageSelection = {},
                    onSelectLanguage = {},
                    onOpenChangelog = {},
                    onDismissDialog = {},
                )
            }
        }
    }

    private fun ComposeUiTest.setStatefulContent() {
        setContent {
            MetroPragueTheme {
                var state by remember { mutableStateOf(SettingsUiState()) }
                SettingsScreen(
                    state = state,
                    onOpenThemeSelection = { state = state.copy(activeDialog = SettingsDialog.ThemeSelection) },
                    onSelectTheme = { theme -> state = state.copy(theme = theme, activeDialog = SettingsDialog.None) },
                    onOpenLanguageSelection = { state = state.copy(activeDialog = SettingsDialog.LanguageSelection) },
                    onSelectLanguage = { lang -> state = state.copy(language = lang, activeDialog = SettingsDialog.None) },
                    onOpenChangelog = { state = state.copy(activeDialog = SettingsDialog.Changelog) },
                    onDismissDialog = { state = state.copy(activeDialog = SettingsDialog.None) },
                )
            }
        }
    }

    // --- Layout tests ---

    @Test
    fun settingsContent_showsAppearanceLanguageAboutSectionsAndVersion() = runComposeUiTest {
        setStatelessContent()

        onNodeWithText("Appearance").assertIsDisplayed()
        onNodeWithText("About").assertIsDisplayed()
        onNodeWithText("App version").assertIsDisplayed()
        onNodeWithText("1.0 (1)").assertIsDisplayed()
        // "Language" appears as both section heading and row title — verify at least one exists
        onAllNodesWithText("Language")[0].assertIsDisplayed()
    }

    @Test
    fun themeRow_isClickable() = runComposeUiTest {
        setStatelessContent()
        onNodeWithText("Theme").assertHasClickAction()
    }

    @Test
    fun changelogRow_isClickable() = runComposeUiTest {
        setStatelessContent()
        onNodeWithText("Changelog").assertHasClickAction()
    }

    @Test
    fun languageRow_isClickable() = runComposeUiTest {
        setStatelessContent()
        onAllNodesWithText("Language")
            .filterToOne(hasClickAction())
            .assertHasClickAction()
    }

    // --- Theme dialog tests ---

    @Test
    fun themeRow_click_opensDialogWithAllThreeChoices() = runComposeUiTest {
        setStatefulContent()

        onNodeWithText("Theme").performClick()

        onNode(hasText("System default") and hasRole(Role.RadioButton)).assertIsDisplayed()
        onNode(hasText("Light") and hasRole(Role.RadioButton)).assertIsDisplayed()
        onNode(hasText("Dark") and hasRole(Role.RadioButton)).assertIsDisplayed()
    }

    @Test
    fun themeDialog_currentSelectionIsMarkedSelected() = runComposeUiTest {
        setStatelessContent(
            state = SettingsUiState(
                theme = ThemePreference.Dark,
                activeDialog = SettingsDialog.ThemeSelection,
            )
        )

        onNode(hasText("Dark") and hasRole(Role.RadioButton)).assertIsSelected()
        onNode(hasText("Light") and hasRole(Role.RadioButton)).assertIsNotSelected()
        onNode(hasText("System default") and hasRole(Role.RadioButton)).assertIsNotSelected()
    }

    @Test
    fun themeDialog_selectDark_closesDialogAndUpdatesSummary() = runComposeUiTest {
        setStatefulContent()

        onNodeWithText("Theme").performClick()
        onNode(hasText("Dark") and hasRole(Role.RadioButton)).performClick()

        onNodeWithText("Cancel").assertDoesNotExist()
        onNodeWithText("Dark").assertIsDisplayed()
    }

    @Test
    fun themeDialog_cancel_preservesExistingSelection() = runComposeUiTest {
        setStatefulContent()

        onNodeWithText("Theme").performClick()
        onNodeWithText("Cancel").performClick()

        onNodeWithText("Cancel").assertDoesNotExist()
        // Both theme and language summaries still show "System default"
        onAllNodesWithText("System default").assertCountEquals(2)
    }

    // --- Language dialog tests ---

    @Test
    fun languageRow_click_opensDialogWithAllThreeChoices() = runComposeUiTest {
        setStatefulContent()

        onAllNodesWithText("Language")
            .filterToOne(hasClickAction())
            .performClick()

        onNode(hasText("System default") and hasRole(Role.RadioButton)).assertIsDisplayed()
        onNode(hasText("English") and hasRole(Role.RadioButton)).assertIsDisplayed()
        onNode(hasText("Czech") and hasRole(Role.RadioButton)).assertIsDisplayed()
    }

    @Test
    fun languageDialog_selectCzech_closesDialogAndUpdatesSummary() = runComposeUiTest {
        setStatefulContent()

        onAllNodesWithText("Language")
            .filterToOne(hasClickAction())
            .performClick()
        onNode(hasText("Czech") and hasRole(Role.RadioButton)).performClick()

        onNodeWithText("Cancel").assertDoesNotExist()
        onNodeWithText("Czech").assertIsDisplayed()
    }

    // --- Changelog dialog tests ---

    @Test
    fun changelogRow_click_opensChangelogWithVersion1_0Notes() = runComposeUiTest {
        setStatefulContent()

        onNodeWithText("Changelog").performClick()

        onNodeWithText("Version 1.0").assertIsDisplayed()
        onNodeWithText("Initial release.", substring = true).assertIsDisplayed()
    }

    @Test
    fun changelogDialog_close_dismissesDialog() = runComposeUiTest {
        setStatefulContent()

        onNodeWithText("Changelog").performClick()
        onNodeWithText("Version 1.0").assertIsDisplayed()

        onNodeWithText("Close").performClick()

        onNodeWithText("Version 1.0").assertDoesNotExist()
    }

    // --- Segmented item semantics tests ---

    @Test
    fun versionRow_isNotClickable() = runComposeUiTest {
        setStatelessContent()
        onNodeWithText("App version").assertHasNoClickAction()
    }

    @Test
    fun settingsRows_eachHaveExactlyOneClickTarget() = runComposeUiTest {
        setStatelessContent()
        // Each row should be a single click target — SegmentedListItem merges semantics so
        // there are no duplicate child click actions within a row.
        onAllNodes(hasClickAction() and hasText("Theme", substring = true)).assertCountEquals(1)
        onAllNodes(hasClickAction() and hasText("Changelog", substring = true)).assertCountEquals(1)
    }
}
