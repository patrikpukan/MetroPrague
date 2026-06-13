package dev.pukan.metroprague.ui.screens.settings

import org.junit.Assert.assertEquals
import org.junit.Test

class SettingsViewModelTest {

    private fun viewModel() = SettingsViewModel()

    @Test
    fun `initial state has System theme, System language, no dialog, and version 1_0 (1)`() {
        val state = viewModel().uiState.value

        assertEquals(ThemePreference.System, state.theme)
        assertEquals(AppLanguage.System, state.language)
        assertEquals(SettingsDialog.None, state.activeDialog)
        assertEquals("1.0", state.versionName)
        assertEquals(1, state.versionCode)
    }

    @Test
    fun `openThemeSelection sets ThemeSelection dialog`() {
        val vm = viewModel()
        vm.openThemeSelection()

        assertEquals(SettingsDialog.ThemeSelection, vm.uiState.value.activeDialog)
    }

    @Test
    fun `selectTheme Dark updates theme and closes dialog`() {
        val vm = viewModel()
        vm.openThemeSelection()
        vm.selectTheme(ThemePreference.Dark)

        val state = vm.uiState.value
        assertEquals(ThemePreference.Dark, state.theme)
        assertEquals(SettingsDialog.None, state.activeDialog)
    }

    @Test
    fun `openLanguageSelection sets LanguageSelection dialog`() {
        val vm = viewModel()
        vm.openLanguageSelection()

        assertEquals(SettingsDialog.LanguageSelection, vm.uiState.value.activeDialog)
    }

    @Test
    fun `selectLanguage Czech updates language and closes dialog`() {
        val vm = viewModel()
        vm.openLanguageSelection()
        vm.selectLanguage(AppLanguage.Czech)

        val state = vm.uiState.value
        assertEquals(AppLanguage.Czech, state.language)
        assertEquals(SettingsDialog.None, state.activeDialog)
    }

    @Test
    fun `openChangelog then dismissDialog closes dialog without changing theme or language`() {
        val vm = viewModel()
        vm.selectTheme(ThemePreference.Light)
        vm.selectLanguage(AppLanguage.English)
        vm.openChangelog()
        assertEquals(SettingsDialog.Changelog, vm.uiState.value.activeDialog)

        vm.dismissDialog()

        val state = vm.uiState.value
        assertEquals(SettingsDialog.None, state.activeDialog)
        assertEquals(ThemePreference.Light, state.theme)
        assertEquals(AppLanguage.English, state.language)
    }

    @Test
    fun `fresh ViewModel resets all choices to mock defaults`() {
        val vm1 = viewModel()
        vm1.selectTheme(ThemePreference.Dark)
        vm1.selectLanguage(AppLanguage.Czech)

        val state = viewModel().uiState.value
        assertEquals(ThemePreference.System, state.theme)
        assertEquals(AppLanguage.System, state.language)
        assertEquals(SettingsDialog.None, state.activeDialog)
    }
}
