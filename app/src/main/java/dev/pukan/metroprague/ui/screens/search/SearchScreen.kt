package dev.pukan.metroprague.ui.screens.search

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.pukan.metroprague.domain.model.Direction
import dev.pukan.metroprague.domain.model.Line
import dev.pukan.metroprague.domain.model.Station

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    searchQuery: String,
    selectedLine: Line?,
    stations: List<Station>,
    sheetState: DirectionSheetUiState?,
    onSearchQueryChange: (String) -> Unit,
    onLineFilterChange: (Line?) -> Unit,
    onStationSelected: (Station) -> Unit,
    onSheetDismissed: () -> Unit,
    onToggleFavorite: (Direction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("SearchScreen"),
    ) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Search stations...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search Icon") },
            singleLine = true,
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilterChip(
                selected = selectedLine == null,
                onClick = { onLineFilterChange(null) },
                label = { Text("All") },
            )
            Line.entries.forEach { line ->
                FilterChip(
                    selected = selectedLine == line,
                    onClick = { onLineFilterChange(line) },
                    label = { Text("Line ${line.name}") },
                )
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(stations, key = { it.id }) { station ->
                StationItem(
                    station = station,
                    onClick = { onStationSelected(station) },
                )
            }
        }
    }

    DirectionPickerSheet(
        sheetState = sheetState,
        onSheetDismissed = onSheetDismissed,
        onToggleFavorite = onToggleFavorite,
    )
}

@Composable
fun StationItem(
    station: Station,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .testTag("StationItem_${station.id}"),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            station.lines
                .sortedBy { it.line.ordinal }
                .forEach { linePosition ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(Color(linePosition.line.colorHex)),
                        )
                        Text(
                            text = linePosition.line.name,
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = station.name,
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
}

@Composable
fun SearchScreenRoute(
    viewModel: SearchViewModel = hiltViewModel(),
) {
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedLine by viewModel.selectedLine.collectAsStateWithLifecycle()
    val stations by viewModel.filteredStations.collectAsStateWithLifecycle()
    val sheetState by viewModel.sheetState.collectAsStateWithLifecycle()

    SearchScreen(
        searchQuery = searchQuery,
        selectedLine = selectedLine,
        stations = stations,
        sheetState = sheetState,
        onSearchQueryChange = viewModel::onSearchQueryChange,
        onLineFilterChange = viewModel::onLineFilterChange,
        onStationSelected = viewModel::onStationSelected,
        onSheetDismissed = viewModel::onSheetDismissed,
        onToggleFavorite = viewModel::onToggleFavorite,
    )
}
