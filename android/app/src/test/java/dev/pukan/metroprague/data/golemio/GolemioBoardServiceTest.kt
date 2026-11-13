package dev.pukan.metroprague.data.golemio

import org.junit.Assert.assertEquals
import org.junit.Test

class GolemioBoardServiceTest {
    @Test
    fun `board URL sends only mapped metro stop IDs`() {
        assertEquals(
            "https://api.golemio.cz/v2/pid/departureboards" +
                "?ids%5B%5D=U1040Z101P&ids%5B%5D=U1040Z102P" +
                "&minutesAfter=60&limit=30&mode=departures&order=real",
            boardUrl(MetroStopIds.idsFor("andel")),
        )
    }
}
