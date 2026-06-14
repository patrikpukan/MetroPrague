package dev.pukan.metroprague.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.pukan.metroprague.data.preferences.UserPreferencesRepository
import dev.pukan.metroprague.domain.model.ThemePreference
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: UserPreferencesRepository,
) : ViewModel() {

    private data class TransientState(
        val language: AppLanguage = AppLanguage.System,
        val activeDialog: SettingsDialog = SettingsDialog.None,
    )

    private val transient = MutableStateFlow(TransientState())

    val uiState: StateFlow<SettingsUiState> =
        combine(repository.themePreference, transient) { theme, tr ->
            SettingsUiState(
                theme = theme,
                language = tr.language,
                activeDialog = tr.activeDialog,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = SettingsUiState(),
        )

    fun openThemeSelection() {
        transient.update { it.copy(activeDialog = SettingsDialog.ThemeSelection) }
    }

    fun selectTheme(theme: ThemePreference) {
        transient.update { it.copy(activeDialog = SettingsDialog.None) }
        viewModelScope.launch { repository.setThemePreference(theme) }
    }

    fun openLanguageSelection() {
        transient.update { it.copy(activeDialog = SettingsDialog.LanguageSelection) }
    }

    fun selectLanguage(language: AppLanguage) {
        transient.update { it.copy(language = language, activeDialog = SettingsDialog.None) }
    }

    fun openChangelog() {
        transient.update { it.copy(activeDialog = SettingsDialog.Changelog) }
    }

    fun dismissDialog() {
        transient.update { it.copy(activeDialog = SettingsDialog.None) }
    }
}
