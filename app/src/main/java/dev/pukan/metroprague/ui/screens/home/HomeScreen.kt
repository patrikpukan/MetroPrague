package dev.pukan.metroprague.ui.screens.home

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
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
import dev.pukan.metroprague.domain.model.FavoriteKey
import dev.pukan.metroprague.domain.model.Line
import dev.pukan.metroprague.ui.model.DepartureDisplay
import dev.pukan.metroprague.ui.theme.MetroPragueTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    uiState: HomeUiState,
    onRemoveFavorite: (FavoriteKey) -> Unit,
    onNavigateToSearch: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.testTag("HomeScreen"),
        topBar = {
            TopAppBar(title = { Text(stringResource(R.string.home_title)) })
        },
    ) { contentPadding ->
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(contentPadding),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }
            uiState.favorites.isEmpty() -> {
                HomeEmptyState(
                    onNavigateToSearch = onNavigateToSearch,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(contentPadding),
                )
            }
            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(contentPadding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(
                        items = uiState.favorites,
                        key = { it.favoriteTagKey() },
                    ) { favorite ->
                        FavoriteCard(
                            favorite = favorite,
                            onRemoveFavorite = { onRemoveFavorite(favorite.key) },
                            modifier = Modifier.testTag("FavoriteCard_${favorite.favoriteTagKey()}"),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeEmptyState(
    onNavigateToSearch: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = stringResource(R.string.home_empty_title),
            style = MaterialTheme.typography.headlineSmall,
        )
        Text(
            text = stringResource(R.string.home_empty_body),
            modifier = Modifier.padding(top = 8.dp),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Button(
            onClick = onNavigateToSearch,
            modifier = Modifier.padding(top = 24.dp),
        ) {
            Text(stringResource(R.string.home_empty_action))
        }
    }
}

@Composable
fun FavoriteCard(
    favorite: FavoriteCardUiState,
    onRemoveFavorite: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = favorite.stationName,
                    style = MaterialTheme.typography.titleMedium,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(Color(favorite.lineColorHex)),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.line_label, favorite.lineLetter),
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
                Text(
                    text = stringResource(R.string.direction_toward, favorite.terminusName),
                    style = MaterialTheme.typography.bodyLarge,
                )
                Text(
                    text = favorite.nextDeparture.label(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onRemoveFavorite) {
                Icon(
                    imageVector = Icons.Filled.Star,
                    contentDescription = stringResource(R.string.favorite_remove),
                )
            }
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

private fun FavoriteCardUiState.favoriteTagKey(): String =
    "${key.stationId}-${key.line}-${key.terminusStationId}"

@Composable
fun HomeScreenRoute(
    onNavigateToSearch: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    HomeScreen(
        uiState = uiState,
        onRemoveFavorite = viewModel::onRemoveFavorite,
        onNavigateToSearch = onNavigateToSearch,
    )
}

private val previewFavorite = FavoriteCardUiState(
    key = FavoriteKey("muzeum", Line.A, "depo-hostivar"),
    stationName = "Muzeum",
    lineLetter = "A",
    lineColorHex = Line.A.colorHex,
    terminusName = "Depo Hostivař",
    nextDeparture = DepartureDisplay.InMinutes(3),
)

@Preview(showBackground = true, widthDp = 360, heightDp = 640)
@Composable
private fun HomeScreenEmptyLightPreview() {
    MetroPragueTheme(dynamicColor = false) {
        HomeScreen(HomeUiState(isLoading = false), {}, {})
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 640, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun HomeScreenEmptyDarkPreview() {
    MetroPragueTheme(darkTheme = true, dynamicColor = false) {
        HomeScreen(HomeUiState(isLoading = false), {}, {})
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 640)
@Composable
private fun HomeScreenPopulatedLightPreview() {
    MetroPragueTheme(dynamicColor = false) {
        HomeScreen(HomeUiState(isLoading = false, favorites = listOf(previewFavorite)), {}, {})
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 640, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun HomeScreenPopulatedDarkPreview() {
    MetroPragueTheme(darkTheme = true, dynamicColor = false) {
        HomeScreen(HomeUiState(isLoading = false, favorites = listOf(previewFavorite)), {}, {})
    }
}
