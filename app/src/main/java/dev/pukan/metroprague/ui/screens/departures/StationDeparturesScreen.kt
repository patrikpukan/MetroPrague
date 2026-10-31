package dev.pukan.metroprague.ui.screens.departures

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.pukan.metroprague.R
import dev.pukan.metroprague.domain.model.Line
import dev.pukan.metroprague.ui.model.DepartureDisplay
import dev.pukan.metroprague.ui.model.label
import dev.pukan.metroprague.ui.theme.MetroPragueTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StationDeparturesScreen(
    uiState: StationDeparturesUiState,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.testTag("StationDeparturesScreen"),
        topBar = {
            TopAppBar(
                title = { Text(uiState.stationName) },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("NavigateBackButton"),
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.navigate_back),
                        )
                    }
                },
            )
        },
    ) { contentPadding ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(contentPadding),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (uiState.lineLetter.isNotEmpty() || uiState.terminusName.isNotEmpty()) {
                DirectionHeader(
                    lineLetter = uiState.lineLetter,
                    lineColorHex = uiState.lineColorHex,
                    terminusName = uiState.terminusName,
                    modifier = Modifier.testTag("StationDeparturesHeader"),
                )
            }

            if (uiState.departures.isEmpty()) {
                val emptyDisplay = uiState.emptyState ?: DepartureDisplay.NoService
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("StationDeparturesEmptyState"),
                ) {
                    Text(
                        text = emptyDisplay.label(),
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("StationDeparturesList"),
                    contentPadding = PaddingValues(bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    itemsIndexed(
                        items = uiState.departures,
                        key = { index, item -> "${item.tripId}_$index" },
                    ) { _, item ->
                        StationDepartureRow(
                            item = item,
                            modifier = Modifier.testTag("DepartureRow_${item.tripId}"),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DirectionHeader(
    lineLetter: String,
    lineColorHex: Long,
    terminusName: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (lineLetter.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(Color(lineColorHex)),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.line_label, lineLetter),
                style = MaterialTheme.typography.labelLarge,
            )
            Spacer(modifier = Modifier.width(12.dp))
        }
        if (terminusName.isNotEmpty()) {
            Text(
                text = stringResource(R.string.direction_toward, terminusName),
                style = MaterialTheme.typography.titleMedium,
            )
        }
    }
}

@Composable
private fun StationDepartureRow(
    item: StationDepartureItemUiState,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.direction_toward, item.destinationName),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyLarge,
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = item.departure.label(),
                modifier = Modifier.testTag("DepartureTime_${item.tripId}"),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
fun StationDeparturesScreenRoute(
    onNavigateBack: () -> Unit,
    viewModel: StationDeparturesViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    StationDeparturesScreen(
        uiState = uiState,
        onNavigateBack = onNavigateBack,
    )
}

private val previewDeparturesUiState = StationDeparturesUiState(
    isLoading = false,
    stationName = "Muzeum",
    lineLetter = "A",
    lineColorHex = Line.A.colorHex,
    terminusName = "Depo Hostivař",
    departures = listOf(
        StationDepartureItemUiState("trip-1", "Depo Hostivař", DepartureDisplay.Now),
        StationDepartureItemUiState("trip-2", "Depo Hostivař", DepartureDisplay.InMinutes(2)),
        StationDepartureItemUiState("trip-3", "Depo Hostivař", DepartureDisplay.InMinutes(4)),
        StationDepartureItemUiState("trip-4", "Depo Hostivař", DepartureDisplay.InMinutes(6)),
        StationDepartureItemUiState("trip-5", "Depo Hostivař", DepartureDisplay.InMinutes(8)),
    ),
)

@Preview(showBackground = true, widthDp = 360, heightDp = 640)
@Composable
private fun StationDeparturesScreenLightPreview() {
    MetroPragueTheme(dynamicColor = false) {
        StationDeparturesScreen(previewDeparturesUiState, onNavigateBack = {})
    }
}

@Preview(
    showBackground = true,
    widthDp = 360,
    heightDp = 640,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun StationDeparturesScreenDarkPreview() {
    MetroPragueTheme(darkTheme = true, dynamicColor = false) {
        StationDeparturesScreen(previewDeparturesUiState, onNavigateBack = {})
    }
}
