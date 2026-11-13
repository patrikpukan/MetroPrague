package dev.pukan.metroprague.ui.model

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import dev.pukan.metroprague.R
import dev.pukan.metroprague.domain.model.Departure
import java.time.Duration
import java.time.Instant
import java.time.ZoneId

sealed interface DepartureDisplay {
    data object NoService : DepartureDisplay
    data object Unavailable : DepartureDisplay
    data object Stale : DepartureDisplay
    data object Cancelled : DepartureDisplay
    data object AtStation : DepartureDisplay
    data object Now : DepartureDisplay
    data class InMinutes(val minutes: Int) : DepartureDisplay
    data class AtTime(val hour: Int, val minute: Int) : DepartureDisplay
}

@Composable
internal fun DepartureDisplay.label(): String = when (this) {
    DepartureDisplay.NoService -> stringResource(R.string.departure_no_service)
    DepartureDisplay.Unavailable -> stringResource(R.string.departure_unavailable)
    DepartureDisplay.Stale -> stringResource(R.string.departure_stale)
    DepartureDisplay.Cancelled -> stringResource(R.string.departure_cancelled)
    DepartureDisplay.AtStation -> stringResource(R.string.departure_at_station)
    DepartureDisplay.Now -> stringResource(R.string.departure_now)
    is DepartureDisplay.InMinutes -> stringResource(R.string.departure_in_minutes, minutes)
    is DepartureDisplay.AtTime -> stringResource(R.string.departure_at_time, hour, minute)
}

fun Departure?.toDisplay(now: Instant, zone: ZoneId): DepartureDisplay {
    if (this == null) return DepartureDisplay.NoService
    if (isCanceled) return DepartureDisplay.Cancelled
    if (isAtStop) return DepartureDisplay.AtStation

    val secondsUntilDeparture = Duration.between(now, effectiveTime).seconds
    if (secondsUntilDeparture < 60) return DepartureDisplay.Now
    if (secondsUntilDeparture < 60 * 60) {
        return DepartureDisplay.InMinutes((secondsUntilDeparture / 60).toInt())
    }

    val localDeparture = effectiveTime.atZone(zone)
    return DepartureDisplay.AtTime(
        hour = localDeparture.hour,
        minute = localDeparture.minute,
    )
}
