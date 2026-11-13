package dev.pukan.metroprague.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.pukan.metroprague.R
import dev.pukan.metroprague.domain.repository.MetroDataStatus
import dev.pukan.metroprague.domain.repository.MetroReloadError
import dev.pukan.metroprague.ui.theme.MetroPragueTheme
import java.text.DateFormat
import java.util.Date

@Composable
internal fun MetroDataSettings(
    status: MetroDataStatus,
    onReload: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(stringResource(R.string.settings_metro_data), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(R.string.settings_data_description), style = MaterialTheme.typography.bodyMedium)
            if (status.reloadedAtMillis == null) {
                Text(stringResource(R.string.settings_data_not_loaded))
            } else {
                Text(stringResource(R.string.settings_data_counts, status.stationCount, status.departureCount))
                Text(stringResource(
                    R.string.settings_data_last_reload,
                    DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
                        .format(Date(status.reloadedAtMillis)),
                ))
            }
            if (status.isReloading) {
                val message = if (status.totalStations == 0) {
                    stringResource(R.string.settings_data_loading_catalogue)
                } else {
                    stringResource(R.string.settings_data_loading_boards, status.completedStations, status.totalStations)
                }
                Text(message, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
                if (status.totalStations == 0) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                } else {
                    LinearProgressIndicator(
                        progress = { status.completedStations.toFloat() / status.totalStations },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
            status.error?.let { error ->
                val resource = when (error) {
                    MetroReloadError.Authentication -> R.string.settings_data_auth_error
                    MetroReloadError.RateLimited -> R.string.settings_data_rate_error
                    MetroReloadError.NetworkOrData -> R.string.settings_data_reload_error
                }
                Text(
                    stringResource(resource),
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                )
            }
            if (!status.canReload) Text(stringResource(R.string.settings_data_reload_unavailable))
            Button(onClick = onReload, enabled = status.canReload && !status.isReloading) {
                Icon(Icons.Outlined.Refresh, contentDescription = null, modifier = Modifier.padding(end = ButtonDefaults.IconSpacing))
                Text(stringResource(R.string.settings_data_reload))
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun MetroDataSettingsPreview() {
    MetroPragueTheme(dynamicColor = false) {
        MetroDataSettings(
            status = MetroDataStatus(canReload = true, isReloading = true, completedStations = 20, totalStations = 58),
            onReload = {},
        )
    }
}
