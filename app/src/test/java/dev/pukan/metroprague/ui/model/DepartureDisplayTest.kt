package dev.pukan.metroprague.ui.model

import dev.pukan.metroprague.domain.model.Departure
import dev.pukan.metroprague.domain.model.Line
import java.time.Instant
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Test

class DepartureDisplayTest {

    private val now = Instant.parse("2026-08-05T10:00:00Z")
    private val zone = ZoneId.of("Europe/Prague")

    @Test
    fun `null departure is no service`() {
        assertEquals(DepartureDisplay.NoService, null.toDisplay(now, zone))
    }

    @Test
    fun `canceled departure is cancelled`() {
        assertEquals(
            DepartureDisplay.Cancelled,
            departure(secondsFromNow = 300, isCanceled = true).toDisplay(now, zone),
        )
    }

    @Test
    fun `departure at stop is at station`() {
        assertEquals(
            DepartureDisplay.AtStation,
            departure(secondsFromNow = 300, isAtStop = true).toDisplay(now, zone),
        )
    }

    @Test
    fun `departure 30 seconds away is now`() {
        assertEquals(
            DepartureDisplay.Now,
            departure(secondsFromNow = 30).toDisplay(now, zone),
        )
    }

    @Test
    fun `departure 5 minutes away is in 5 minutes`() {
        assertEquals(
            DepartureDisplay.InMinutes(5),
            departure(secondsFromNow = 5 * 60).toDisplay(now, zone),
        )
    }

    @Test
    fun `departure 90 minutes away is local time`() {
        assertEquals(
            DepartureDisplay.AtTime(hour = 13, minute = 30),
            departure(secondsFromNow = 90 * 60).toDisplay(now, zone),
        )
    }

    @Test
    fun `departure exactly 60 minutes away is local time`() {
        assertEquals(
            DepartureDisplay.AtTime(hour = 13, minute = 0),
            departure(secondsFromNow = 60 * 60).toDisplay(now, zone),
        )
    }

    private fun departure(
        secondsFromNow: Long,
        isAtStop: Boolean = false,
        isCanceled: Boolean = false,
    ) = Departure(
        tripId = "trip",
        line = Line.A,
        headsign = "Depo Hostivař",
        scheduled = now.plusSeconds(secondsFromNow),
        predicted = null,
        delaySeconds = null,
        isAtStop = isAtStop,
        isCanceled = isCanceled,
    )
}
