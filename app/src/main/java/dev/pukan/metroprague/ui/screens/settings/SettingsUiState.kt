package dev.pukan.metroprague.ui.screens.settings

enum class ThemePreference { System, Light, Dark }

enum class AppLanguage { System, English, Czech }

sealed interface SettingsDialog {
    data object None : SettingsDialog
    data object ThemeSelection : SettingsDialog
    data object LanguageSelection : SettingsDialog
    data object Changelog : SettingsDialog
}

data class ChangelogEntry(
    val version: String,
    val notes: List<String>,
)

data class SettingsUiState(
    val theme: ThemePreference = ThemePreference.System,
    val language: AppLanguage = AppLanguage.System,
    val activeDialog: SettingsDialog = SettingsDialog.None,
    val versionName: String = "1.0",
    val versionCode: Int = 1,
    val changelogEntries: List<ChangelogEntry> = listOf(
        ChangelogEntry(
            version = "1.0",
            notes = listOf(
                "Initial release.",
                "Metro Prague line and station search.",
                "Theme and language preferences.",
            ),
        ),
    ),
)
