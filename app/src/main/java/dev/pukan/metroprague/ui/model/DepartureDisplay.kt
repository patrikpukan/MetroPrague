package dev.pukan.metroprague.ui.model

import dev.pukan.metroprague.domain.model.Departure
import java.time.Duration
import java.time.Instant
import java.time.ZoneId

sealed interface DepartureDisplay {
    data object NoService : DepartureDisplay
    data object Cancelled : DepartureDisplay
    data object AtStation : DepartureDisplay
    data object Now : DepartureDisplay
    data class InMinutes(val minutes: Int) : DepartureDisplay
    data class AtTime(val hour: Int, val minute: Int) : DepartureDisplay
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
