package dev.pukan.metroprague.ui.screens.settings

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SettingsViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    fun openThemeSelection() {
        _uiState.value = _uiState.value.copy(activeDialog = SettingsDialog.ThemeSelection)
    }

    fun selectTheme(theme: ThemePreference) {
        _uiState.value = _uiState.value.copy(theme = theme, activeDialog = SettingsDialog.None)
    }

    fun openLanguageSelection() {
        _uiState.value = _uiState.value.copy(activeDialog = SettingsDialog.LanguageSelection)
    }

    fun selectLanguage(language: AppLanguage) {
        _uiState.value = _uiState.value.copy(language = language, activeDialog = SettingsDialog.None)
    }

    fun openChangelog() {
        _uiState.value = _uiState.value.copy(activeDialog = SettingsDialog.Changelog)
    }

    fun dismissDialog() {
        _uiState.value = _uiState.value.copy(activeDialog = SettingsDialog.None)
    }
}
