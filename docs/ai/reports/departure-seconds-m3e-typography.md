# Investigation Report: Second-Precision Departure Countdowns & M3E Typography (Variant C)

Date: 2026-10-31  
Status: Decision-ready handoff for `planner.md`

---

## 1. Problem

Departure times on `StationDeparturesScreen` (`app/src/main/java/dev/pukan/metroprague/ui/screens/departures/StationDeparturesScreen.kt`) and `HomeScreen` (`FavoriteCard` in `app/src/main/java/dev/pukan/metroprague/ui/screens/home/HomeScreen.kt`) currently truncate countdowns to whole minutes (`"2 min"`), mask the final 60 seconds behind `"Now"` or `"At station"`, refresh only every 15 seconds, and render in low-prominence typography (`titleMedium` 16sp on `StationDeparturesScreen`; `bodyMedium` 14sp `onSurfaceVariant` on `FavoriteCard`). Riders need second-level countdown granularity (`X min YY s`, `YY s`, and `Now` only under 5 seconds), a separate visual indicator when a train is at the station (`isAtStop`), and prominent right-aligned Material 3 Expressive (M3E) typography.

---

## 2. Findings

1. **Display Mapping & Truncation (`app/src/main/java/dev/pukan/metroprague/ui/model/DepartureDisplay.kt`)**:
   - `Departure?.toDisplay(now, zone)` checks `if (isAtStop) return DepartureDisplay.AtStation` (line 37) before computing `secondsUntilDeparture`. Because `MockDepartureRepository` (`app/src/main/java/dev/pukan/metroprague/data/repository/MockDepartureRepository.kt`, line 123) sets `isAtStop = secondsUntilDeparture in 0..30`, the last 30 seconds never show a time.
   - `secondsUntilDeparture < 60` returns `DepartureDisplay.Now` (line 40), hiding `5..59` seconds.
   - `secondsUntilDeparture < 3600` returns `DepartureDisplay.InMinutes((secondsUntilDeparture / 60).toInt())` (line 42), discarding the remainder seconds (`secondsUntilDeparture % 60`).
   - `DepartureDisplay` is consumed across three surfaces:
     - `StationDeparturesViewModel` (`ui/screens/departures/StationDeparturesViewModel.kt`, line 93) → `StationDepartureRow` (`ui/screens/departures/StationDeparturesScreen.kt`, lines 176–201)
     - `HomeViewModel` (`ui/screens/home/HomeViewModel.kt`, line 65) → `FavoriteCard` (`ui/screens/home/HomeScreen.kt`, lines 137–188)
     - `SearchViewModel` (`ui/screens/search/SearchViewModel.kt`, line 104) → `DirectionRow` (`ui/components/DirectionRow.kt`, lines 34–85)

2. **15-Second Ticker Bottleneck & JSON Re-Parsing**:
   - `GolemioDepartureRepository` (`app/src/main/java/dev/pukan/metroprague/data/repository/GolemioDepartureRepository.kt`, lines 25–41) ticks every `15_000 ms` and calls `parser.parse(stationId, saved.response, catalogue)` inside the `combine` block on line 41. Changing `delay(15_000)` to `delay(1_000)` without separating snapshot parsing from the clock tick would re-parse raw JSON from DataStore every second per observed station.
   - `MockDepartureRepository` (`app/src/main/java/dev/pukan/metroprague/data/repository/MockDepartureRepository.kt`, line 44) uses `REFRESH_INTERVAL_MILLIS = 15_000L` (line 143). Because `buildDepartureBoard` (lines 49–75) anchors scheduled departure instants deterministically to `localDate.atStartOfDay().plusSeconds(scheduledSeconds)`, reducing `REFRESH_INTERVAL_MILLIS` to `1_000L` produces smooth 1-second countdowns without shifting scheduled/predicted timestamps.

3. **M3E Typography Availability**:
   - `androidx.compose.material3:material3` is pinned to `1.5.0-alpha21` in `gradle/libs.versions.toml` (line 14).
   - `MaterialTheme.typography` exposes M3E emphasized styles directly: `headlineMediumEmphasized` (28sp Medium), `headlineSmallEmphasized` (24sp Medium), `titleLargeEmphasized` (22sp Medium), `titleMediumEmphasized` (16sp Bold), `titleSmallEmphasized` (14sp Bold), and `labelSmallEmphasized` (11sp Bold).

4. **Accessibility & Narrow Screen Constraint (`app/src/androidTest/java/dev/pukan/metroprague/ui/screens/departures/StationDeparturesScreenTest.kt`)**:
   - `narrowLayoutWithLargeText_keepsDepartureTimeVisible` (lines 69–107) forces `DpSize(280.dp, 640.dp)` and `FontScale(2f)` and asserts that `onNodeWithTag("DepartureTime_trip-1")` has `lineCount == 1` and `getLineRight(0) <= size.width`.

---

## 3. Chosen Approach (Variant C)

### Time Granularity & At-Station Rules

1. **Countdown thresholds** (for non-cancelled departures with `secondsUntilDeparture = Duration.between(now, effectiveTime).seconds`):
   - `secondsUntilDeparture < 5`: `DepartureDisplay.Now` → `"Now"` (covers `0..4 s` as well as negative `secondsUntilDeparture` when an `isAtStop` train remains on a fresh Golemio board).
   - `secondsUntilDeparture in 5..59`: `DepartureDisplay.InSeconds(seconds: Int)` → `"%1$d s"` (e.g., `"48 s"`, `"22 s"`).
   - `secondsUntilDeparture in 60..3599`: `DepartureDisplay.InMinutesSeconds(minutes: Int, seconds: Int)` → `"%1$d min %2$02d s"` (e.g., `"2 min 15 s"`, `"4 min 05 s"`), where `minutes = (secondsUntilDeparture / 60).toInt()` and `seconds = (secondsUntilDeparture % 60).toInt()`.
   - `secondsUntilDeparture >= 3600`: `DepartureDisplay.AtTime(hour, minute)` → `"%1$02d:%2$02d"`.
2. **Decouple `isAtStop` from `DepartureDisplay`**:
   - A train at the station (`departure.isAtStop == true`) **still computes its time display** via the rules above (e.g., `"22 s"` when 22 seconds remain, or `"Now"` when `< 5` seconds remain) instead of replacing the countdown with `DepartureDisplay.AtStation`.
   - Expose `isAtStation: Boolean` alongside `DepartureDisplay` in `StationDepartureItemUiState` (`ui/screens/departures/StationDeparturesUiState.kt`) and `FavoriteCardUiState` (`ui/screens/home/HomeUiState.kt`) — `false` when the board is stale/unavailable or when the departure is null/cancelled.
3. **1-Second Ticker**:
   - In `GolemioDepartureRepository`, parse `store.data` for `stationId` **before** combining with the clock ticker so JSON parsing happens only when the persisted snapshot changes, and change the ticker delay from `15_000` to `1_000`.
   - In `MockDepartureRepository`, change `REFRESH_INTERVAL_MILLIS` from `15_000L` to `1_000L`.

### Concrete UI Specification — Variant C

#### A. Shared M3E Split-Weight Countdown Text & At-Station Sub-Label

- **Countdown `Text` (`StyledUnitTimeText` / `DepartureTimeText`)**:
  - Base style: `MaterialTheme.typography.headlineMediumEmphasized`, `maxLines = 1`, `softWrap = false`.
  - When rendering `InMinutesSeconds`, `InSeconds`, `Now`, or `AtTime`:
    - **Numeric / primary tokens** (`minutes`, `seconds`, `"Now"`, `"HH:mm"`): `MaterialTheme.typography.headlineMediumEmphasized.toSpanStyle().copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontFeatureSettings = "tnum")`.
    - **Unit tokens** (`"min"`, `"s"`): `MaterialTheme.typography.titleSmallEmphasized.toSpanStyle().copy(color = MaterialTheme.colorScheme.onSurfaceVariant)`.
    - **Fallback/status states** (`NoService`, `Unavailable`, `Stale`, `Cancelled` when shown on a card): `MaterialTheme.typography.titleMediumEmphasized` in `MaterialTheme.colorScheme.onSurfaceVariant` so multi-word status messages (`"Reload departures in Settings"`, `"Live departures unavailable"`) can wrap naturally without clipping.
- **At-Station Indicator (shown above the countdown when `isAtStation == true`)**:
  - `Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp))` aligned to `Alignment.End` inside the right-hand `Column`:
    - `Icon(imageVector = Icons.Filled.WarningAmber, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.tertiary)`
    - `Text(text = stringResource(R.string.departure_at_station), style = MaterialTheme.typography.labelSmallEmphasized, color = MaterialTheme.colorScheme.tertiary)`

```kotlin
@Composable
private fun StyledUnitTimeText(
    timeText: String,
    modifier: Modifier = Modifier,
) {
    val numberStyle = MaterialTheme.typography.headlineMediumEmphasized.toSpanStyle().copy(
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        fontFeatureSettings = "tnum",
    )
    val unitStyle = MaterialTheme.typography.titleSmallEmphasized.toSpanStyle().copy(
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    val annotated = buildAnnotatedString {
        val parts = timeText.split(" ")
        parts.forEachIndexed { index, part ->
            if (index > 0) append(" ")
            val isUnit = part == "min" || part == "s"
            withStyle(if (isUnit) unitStyle else numberStyle) {
                append(part)
            }
        }
    }
    Text(
        text = annotated,
        modifier = modifier,
        style = MaterialTheme.typography.headlineMediumEmphasized,
        maxLines = 1,
        softWrap = false,
    )
}
```

#### B. `StationDepartureRow` on `StationDeparturesScreen`

- `Card(modifier = modifier.fillMaxWidth())`
- Inner `Row` (or `FlowRow` to handle extreme `280.dp` @ `2f` font scale without squeezing the destination text) with `Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)`:
  - **Left**: `Text(text = stringResource(R.string.direction_toward, item.destinationName), modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)`
  - `Spacer(modifier = Modifier.width(12.dp))`
  - **Right**: `Column(horizontalAlignment = Alignment.End)`:
    - Optional At-Station indicator row (`if (item.isAtStation)`).
    - Countdown `Text` with `Modifier.testTag("DepartureTime_${item.tripId}")`.

#### C. `FavoriteCard` on `HomeScreen`

- Two-row card layout inside `Card(modifier = modifier.fillMaxWidth())` with `Column(modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 14.dp))`:
  1. **Top Header Row** (`Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically)`):
     - Line color circle: `Box(modifier = Modifier.size(20.dp).clip(CircleShape).background(Color(favorite.lineColorHex)))`
     - `Spacer(modifier = Modifier.width(8.dp))`
     - Station + Line title: `Text(text = stringResource(R.string.favorite_station_line, favorite.stationName, favorite.lineLetter), modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleMediumEmphasized)` (backed by `favorite_station_line` = `"%1$s · Line %2$s"` in `res/values/strings.xml`).
     - Remove favorite button: `IconButton(onClick = onRemoveFavorite)` with `Icon(Icons.Filled.Star, contentDescription = stringResource(R.string.favorite_remove))`.
  2. **Bottom Content Row** (`Row(modifier = Modifier.fillMaxWidth().padding(end = 8.dp), verticalAlignment = Alignment.Bottom)`):
     - **Left**: `Text(text = stringResource(R.string.direction_toward, favorite.terminusName), modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)`
     - `Spacer(modifier = Modifier.width(12.dp))`
     - **Right**: `Column(horizontalAlignment = Alignment.End)` containing the optional At-Station indicator (`if (favorite.isAtStation)`) and the M3E split-weight countdown `Text`.

### Alternatives Rejected

- **Variant A (Single-style `headlineSmallEmphasized` + tonal pill under destination, single-row `FavoriteCard`)**: Rejected; keeping the star button beside the countdown in `FavoriteCard` squeezed `"Line A · To Depo Hostivař"` into wrapping.
- **Variant B (`primaryContainer` / `tertiaryContainer` time pill)**: Rejected in favor of Variant C's cleaner split-weight typography and header-anchored star layout.
- **Wall-clock timestamp (`HH:mm:ss`)**: Rejected because riders need relative countdown time rather than mental subtraction.

---

## 4. Risks & Constraints

1. **Narrow layout + `FontScale(2f)` test (`StationDeparturesScreenTest.kt`)**:
   - `DepartureTime_<tripId>` must keep `maxLines = 1` and `softWrap = false` so `layoutResult.lineCount == 1` and `getLineRight(0) <= layoutResult.size.width` pass at `280.dp` width and `2.0x` font scale.
   - Rendering Variant C at `280.dp` width and `2.0x` font scale showed that while `maxLines = 1, softWrap = false` keeps `DepartureTime_<tripId>` on 1 line, a plain horizontal `Row` leaves only ~`28.dp` for `"To Nemocnice Motol"`, squeezing the destination into a vertical 1–2 character column. Using `FlowRow` (so the destination and right-aligned countdown stack when horizontal space is exhausted) preserves both single-line `DepartureTime_<tripId>` semantics and readable destination text.
2. **Existing tests expecting `DepartureDisplay.InMinutes` and `DepartureDisplay.AtStation`**:
   - `DepartureDisplayTest.kt`, `HomeViewModelTest.kt`, `StationDeparturesViewModelTest.kt`, `SearchViewModelTest.kt`, `GolemioDepartureRepositoryTest.kt`, `HomeScreenTest.kt`, and `StationDeparturesScreenTest.kt` construct or assert `DepartureDisplay.InMinutes` / `DepartureDisplay.AtStation` and must be updated consistently.

---

## 5. Open Questions

None. All product and visual decisions are locked in.
