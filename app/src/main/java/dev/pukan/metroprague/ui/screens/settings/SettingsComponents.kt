package dev.pukan.metroprague.ui.screens.settings

import android.content.res.Configuration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.pukan.metroprague.R
import dev.pukan.metroprague.ui.theme.MetroPragueTheme

@Composable
fun SettingsSectionHeading(
    title: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 8.dp),
    )
}

@Composable
fun SettingsClickableRow(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    summary: String? = null,
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = summary?.let { { Text(it) } },
        leadingContent = {
            Icon(imageVector = icon, contentDescription = null)
        },
        trailingContent = {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                contentDescription = null,
            )
        },
        modifier = modifier.clickable(role = Role.Button, onClick = onClick),
    )
}

@Composable
fun SettingsInfoRow(
    icon: ImageVector,
    title: String,
    summary: String,
    modifier: Modifier = Modifier,
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(summary) },
        leadingContent = {
            Icon(imageVector = icon, contentDescription = null)
        },
        modifier = modifier,
    )
}

@Composable
fun <T> SingleChoiceDialog(
    title: String,
    options: List<T>,
    selectedOption: T,
    optionLabel: @Composable (T) -> String,
    onOptionSelected: (T) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                options.forEach { option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = option == selectedOption,
                                role = Role.RadioButton,
                                onClick = { onOptionSelected(option) },
                            )
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = option == selectedOption,
                            onClick = null,
                        )
                        Text(
                            text = optionLabel(option),
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(start = 16.dp),
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.settings_dialog_dismiss))
            }
        },
        modifier = modifier,
    )
}

// --- Previews ---

@Preview(showBackground = true)
@Composable
private fun SettingsSectionHeadingPreview() {
    MetroPragueTheme(dynamicColor = false) {
        SettingsSectionHeading(title = "Appearance")
    }
}

@Preview(showBackground = true)
@Composable
private fun SettingsClickableRowWithSummaryPreview() {
    MetroPragueTheme(dynamicColor = false) {
        SettingsClickableRow(
            icon = Icons.Outlined.DarkMode,
            title = "Theme",
            summary = "System default",
            onClick = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SettingsClickableRowNoSummaryPreview() {
    MetroPragueTheme(dynamicColor = false) {
        SettingsClickableRow(
            icon = Icons.Outlined.Language,
            title = "Changelog",
            onClick = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SettingsInfoRowPreview() {
    MetroPragueTheme(dynamicColor = false) {
        SettingsInfoRow(
            icon = Icons.Outlined.Info,
            title = "App version",
            summary = "1.0 (1)",
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SingleChoiceDialogLightPreview() {
    MetroPragueTheme(dynamicColor = false) {
        SingleChoiceDialog(
            title = "Theme",
            options = ThemePreference.entries,
            selectedOption = ThemePreference.System,
            optionLabel = { option ->
                when (option) {
                    ThemePreference.System -> "System default"
                    ThemePreference.Light -> "Light"
                    ThemePreference.Dark -> "Dark"
                }
            },
            onOptionSelected = {},
            onDismiss = {},
        )
    }
}

@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SingleChoiceDialogDarkPreview() {
    MetroPragueTheme(darkTheme = true, dynamicColor = false) {
        SingleChoiceDialog(
            title = "Theme",
            options = ThemePreference.entries,
            selectedOption = ThemePreference.Dark,
            optionLabel = { option ->
                when (option) {
                    ThemePreference.System -> "System default"
                    ThemePreference.Light -> "Light"
                    ThemePreference.Dark -> "Dark"
                }
            },
            onOptionSelected = {},
            onDismiss = {},
        )
    }
}
