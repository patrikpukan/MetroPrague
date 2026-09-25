package dev.pukan.metroprague.ui.screens.search

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.pukan.metroprague.R
import dev.pukan.metroprague.domain.model.Direction
import dev.pukan.metroprague.ui.components.DirectionRow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DirectionPickerSheet(
    sheetState: DirectionSheetUiState?,
    onSheetDismissed: () -> Unit,
    onToggleFavorite: (Direction) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (sheetState == null) return

    ModalBottomSheet(
        onDismissRequest = onSheetDismissed,
        modifier = modifier.testTag("DirectionPickerSheet"),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = stringResource(R.string.direction_picker_title),
                modifier = Modifier.padding(horizontal = 16.dp),
                style = MaterialTheme.typography.headlineSmall,
            )
            Text(
                text = sheetState.stationName,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            sheetState.directions.forEach { rowState ->
                key(rowState.direction.line, rowState.direction.terminusStationId) {
                    DirectionRow(
                        directionLabel = stringResource(
                            R.string.direction_toward,
                            rowState.direction.terminusName,
                        ),
                        lineLetter = rowState.lineLetter,
                        lineColor = Color(rowState.lineColorHex),
                        departure = rowState.nextDeparture,
                        isFavorite = rowState.isFavorite,
                        onToggleFavorite = { onToggleFavorite(rowState.direction) },
                        modifier = Modifier.testTag(
                            "DirectionRow_${rowState.direction.line}_${rowState.direction.terminusStationId}",
                        ),
                    )
                }
            }
            TextButton(
                onClick = onSheetDismissed,
                modifier = Modifier.padding(horizontal = 8.dp),
            ) {
                Text(stringResource(R.string.direction_picker_close))
            }
        }
    }
}
