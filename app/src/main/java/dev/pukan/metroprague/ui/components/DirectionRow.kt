package dev.pukan.metroprague.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.pukan.metroprague.R
import dev.pukan.metroprague.ui.model.DepartureDisplay

@Composable
fun DirectionRow(
    directionLabel: String,
    lineLetter: String,
    lineColor: Color,
    departure: DepartureDisplay,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(lineColor),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = stringResource(R.string.line_label, lineLetter),
            style = MaterialTheme.typography.labelLarge,
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = directionLabel,
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                text = departure.label(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        IconButton(onClick = onToggleFavorite) {
            Icon(
                imageVector = if (isFavorite) Icons.Filled.Star else Icons.Outlined.Star,
                contentDescription = stringResource(
                    if (isFavorite) R.string.favorite_remove else R.string.favorite_add,
                ),
            )
        }
    }
}

@Composable
private fun DepartureDisplay.label(): String = when (this) {
    DepartureDisplay.NoService -> stringResource(R.string.departure_no_service)
    DepartureDisplay.Cancelled -> stringResource(R.string.departure_cancelled)
    DepartureDisplay.AtStation -> stringResource(R.string.departure_at_station)
    DepartureDisplay.Now -> stringResource(R.string.departure_now)
    is DepartureDisplay.InMinutes -> stringResource(R.string.departure_in_minutes, minutes)
    is DepartureDisplay.AtTime -> stringResource(R.string.departure_at_time, hour, minute)
}
