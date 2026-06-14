package dev.pukan.metroprague.ui.screens.settings

import android.content.res.Configuration
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Article
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.pukan.metroprague.R
import dev.pukan.metroprague.domain.model.ThemePreference
import dev.pukan.metroprague.ui.theme.MetroPragueTheme

@Composable
fun SettingsScreen(
    state: SettingsUiState,
    onOpenThemeSelection: () -> Unit,
    onSelectTheme: (ThemePreference) -> Unit,
    onOpenLanguageSelection: () -> Unit,
    onSelectLanguage: (AppLanguage) -> Unit,
    onOpenChangelog: () -> Unit,
    onDismissDialog: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        SettingsSectionHeading(title = stringResource(R.string.settings_section_appearance))
        SettingsGroup(
            modifier = Modifier.padding(horizontal = 16.dp),
            items = listOf(
                SettingsGroupItem.Clickable(
                    icon = Icons.Outlined.DarkMode,
                    title = stringResource(R.string.settings_theme_label),
                    summary = state.theme.label(),
                    onClick = onOpenThemeSelection,
                ),
            ),
        )

        SettingsSectionHeading(title = stringResource(R.string.settings_section_language))
        SettingsGroup(
            modifier = Modifier.padding(horizontal = 16.dp),
            items = listOf(
                SettingsGroupItem.Clickable(
                    icon = Icons.Outlined.Language,
                    title = stringResource(R.string.settings_language_label),
                    summary = state.language.label(),
                    onClick = onOpenLanguageSelection,
                ),
            ),
        )

        SettingsSectionHeading(title = stringResource(R.string.settings_section_about))
        SettingsGroup(
            modifier = Modifier.padding(horizontal = 16.dp),
            items = listOf(
                SettingsGroupItem.Clickable(
                    icon = Icons.AutoMirrored.Outlined.Article,
                    title = stringResource(R.string.settings_changelog_label),
                    onClick = onOpenChangelog,
                ),
                SettingsGroupItem.Info(
                    icon = Icons.Outlined.Info,
                    title = stringResource(R.string.settings_version_label),
                    summary = stringResource(
                        R.string.settings_version_format,
                        state.versionName,
                        state.versionCode,
                    ),
                ),
            ),
        )
    }

    when (state.activeDialog) {
        SettingsDialog.ThemeSelection -> SingleChoiceDialog(
            title = stringResource(R.string.settings_theme_dialog_title),
            options = ThemePreference.entries,
            selectedOption = state.theme,
            optionLabel = { it.label() },
            onOptionSelected = onSelectTheme,
            onDismiss = onDismissDialog,
        )
        SettingsDialog.LanguageSelection -> SingleChoiceDialog(
            title = stringResource(R.string.settings_language_dialog_title),
            options = AppLanguage.entries,
            selectedOption = state.language,
            optionLabel = { it.label() },
            onOptionSelected = onSelectLanguage,
            onDismiss = onDismissDialog,
        )
        SettingsDialog.Changelog -> ChangelogDialog(
            entries = state.changelogEntries,
            onDismiss = onDismissDialog,
        )
        SettingsDialog.None -> Unit
    }
}

@Composable
private fun ChangelogDialog(
    entries: List<ChangelogEntry>,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_changelog_dialog_title)) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                entries.forEach { entry ->
                    Text(
                        text = stringResource(R.string.settings_changelog_version_heading, entry.version),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                    entry.notes.forEach { note ->
                        Text(
                            text = "• $note",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(start = 8.dp, bottom = 4.dp),
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.settings_dialog_close))
            }
        },
    )
}

@Composable
private fun ThemePreference.label(): String = when (this) {
    ThemePreference.System -> stringResource(R.string.settings_theme_system)
    ThemePreference.Light -> stringResource(R.string.settings_theme_light)
    ThemePreference.Dark -> stringResource(R.string.settings_theme_dark)
}

@Composable
private fun AppLanguage.label(): String = when (this) {
    AppLanguage.System -> stringResource(R.string.settings_language_system)
    AppLanguage.English -> stringResource(R.string.settings_language_english)
    AppLanguage.Czech -> stringResource(R.string.settings_language_czech)
}

@Composable
fun SettingsScreenRoute(
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    SettingsScreen(
        state = state,
        onOpenThemeSelection = viewModel::openThemeSelection,
        onSelectTheme = viewModel::selectTheme,
        onOpenLanguageSelection = viewModel::openLanguageSelection,
        onSelectLanguage = viewModel::selectLanguage,
        onOpenChangelog = viewModel::openChangelog,
        onDismissDialog = viewModel::dismissDialog,
    )
}

// --- Previews ---

@Preview(showBackground = true, widthDp = 360, heightDp = 640)
@Composable
private fun SettingsScreenLightPreview() {
    MetroPragueTheme(dynamicColor = false) {
        SettingsScreen(
            state = SettingsUiState(),
            onOpenThemeSelection = {},
            onSelectTheme = {},
            onOpenLanguageSelection = {},
            onSelectLanguage = {},
            onOpenChangelog = {},
            onDismissDialog = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 640, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SettingsScreenDarkPreview() {
    MetroPragueTheme(darkTheme = true, dynamicColor = false) {
        SettingsScreen(
            state = SettingsUiState(theme = ThemePreference.Dark, language = AppLanguage.Czech),
            onOpenThemeSelection = {},
            onSelectTheme = {},
            onOpenLanguageSelection = {},
            onSelectLanguage = {},
            onOpenChangelog = {},
            onDismissDialog = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 640)
@Composable
private fun SettingsScreenThemeDialogPreview() {
    MetroPragueTheme(dynamicColor = false) {
        SettingsScreen(
            state = SettingsUiState(activeDialog = SettingsDialog.ThemeSelection),
            onOpenThemeSelection = {},
            onSelectTheme = {},
            onOpenLanguageSelection = {},
            onSelectLanguage = {},
            onOpenChangelog = {},
            onDismissDialog = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 640)
@Composable
private fun SettingsScreenChangelogDialogPreview() {
    MetroPragueTheme(dynamicColor = false) {
        SettingsScreen(
            state = SettingsUiState(activeDialog = SettingsDialog.Changelog),
            onOpenThemeSelection = {},
            onSelectTheme = {},
            onOpenLanguageSelection = {},
            onSelectLanguage = {},
            onOpenChangelog = {},
            onDismissDialog = {},
        )
    }
}
