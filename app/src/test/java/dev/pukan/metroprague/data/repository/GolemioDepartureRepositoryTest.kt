package dev.pukan.metroprague.data.repository

import dev.pukan.metroprague.data.golemio.GolemioBoardService
import dev.pukan.metroprague.data.golemio.GolemioDepartureBoardParser
import dev.pukan.metroprague.data.golemio.GolemioHttpException
import dev.pukan.metroprague.domain.model.Line
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GolemioDepartureRepositoryTest {
    @Test
    fun `loads a metro board using mapped platform IDs`() = runTest {
        var requestedIds: List<String>? = null
        val service = GolemioBoardService { stopIds ->
            requestedIds = stopIds
            """{
                "departures": [{
                    "route": {"type": 1, "short_name": "B"},
                    "trip": {"id": "trip-1", "headsign": "Zličín", "is_at_stop": false, "is_canceled": false},
                    "stop": {"id": "U1040Z101P"},
                    "departure_timestamp": {"scheduled": "2026-09-28T12:00:00+02:00", "predicted": null},
                    "delay": {"is_available": false, "seconds": null}
                }],
                "infotexts": []
            }""".trimIndent()
        }

        val board = repository(service).getDepartureBoard("andel").first()

        assertEquals(listOf("U1040Z101P", "U1040Z102P"), requestedIds)
        assertEquals(Line.B, board.departures.single().line)
        assertEquals("zlicin", board.departures.single().directionTerminusStationId)
        assertFalse(board.isUnavailable)
    }

    @Test
    fun `API failure is unavailable rather than no service`() = runTest {
        val board = repository(GolemioBoardService { throw GolemioHttpException(429) })
            .getDepartureBoard("andel")
            .first()

        assertTrue(board.isUnavailable)
        assertTrue(board.departures.isEmpty())
    }

    @Test
    fun `unknown station does not call API`() = runTest {
        val board = repository(GolemioBoardService { error("API should not be called") })
            .getDepartureBoard("unknown")
            .first()

        assertTrue(board.isUnavailable)
    }

    private fun repository(service: GolemioBoardService) = GolemioDepartureRepository(
        service = service,
        parser = GolemioDepartureBoardParser(),
    )
}
