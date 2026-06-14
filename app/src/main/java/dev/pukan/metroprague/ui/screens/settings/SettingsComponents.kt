@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package dev.pukan.metroprague.ui.screens.settings

import android.content.res.Configuration
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Article
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.pukan.metroprague.R
import dev.pukan.metroprague.domain.model.ThemePreference
import dev.pukan.metroprague.ui.theme.MetroPragueTheme

// --- Item models ---

sealed interface SettingsGroupItem {
    data class Clickable(
        val icon: ImageVector,
        val title: String,
        val onClick: () -> Unit,
        val summary: String? = null,
    ) : SettingsGroupItem

    data class Info(
        val icon: ImageVector,
        val title: String,
        val summary: String,
    ) : SettingsGroupItem
}

// --- Segmented group component ---

@Composable
fun SettingsGroup(
    items: List<SettingsGroupItem>,
    modifier: Modifier = Modifier,
) {
    val count = items.size
    Column(modifier = modifier) {
        items.forEachIndexed { index, item ->
            if (index > 0) {
                Spacer(modifier = Modifier.height(ListItemDefaults.SegmentedGap))
            }
            val shapes = ListItemDefaults.segmentedShapes(index, count)
            when (item) {
                is SettingsGroupItem.Clickable -> SegmentedListItem(
                    onClick = item.onClick,
                    shapes = shapes,
                    modifier = Modifier.fillMaxWidth(),
                    leadingContent = {
                        Icon(imageVector = item.icon, contentDescription = null)
                    },
                    trailingContent = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                            contentDescription = null,
                        )
                    },
                    supportingContent = item.summary?.let { s -> { Text(s) } },
                ) {
                    Text(item.title)
                }

                is SettingsGroupItem.Info -> {
                    val segmentedColors = ListItemDefaults.segmentedColors()
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = shapes.shape,
                        color = segmentedColors.containerColor(
                            enabled = true,
                            selected = false,
                            dragged = false,
                        ),
                    ) {
                        ListItem(
                            headlineContent = { Text(item.title) },
                            leadingContent = {
                                Icon(imageVector = item.icon, contentDescription = null)
                            },
                            supportingContent = { Text(item.summary) },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                            tonalElevation = 0.dp,
                        )
                    }
                }
            }
        }
    }
}

// --- Section heading ---

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

// --- Choice dialog ---

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
private fun OneItemGroupPreview() {
    MetroPragueTheme(dynamicColor = false) {
        SettingsGroup(
            items = listOf(
                SettingsGroupItem.Clickable(
                    icon = Icons.Outlined.DarkMode,
                    title = "Theme",
                    summary = "System default",
                    onClick = {},
                )
            )
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun TwoItemGroupPreview() {
    MetroPragueTheme(dynamicColor = false) {
        SettingsGroup(
            items = listOf(
                SettingsGroupItem.Clickable(
                    icon = Icons.AutoMirrored.Outlined.Article,
                    title = "Changelog",
                    onClick = {},
                ),
                SettingsGroupItem.Info(
                    icon = Icons.Outlined.Info,
                    title = "App version",
                    summary = "1.0 (1)",
                ),
            )
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
