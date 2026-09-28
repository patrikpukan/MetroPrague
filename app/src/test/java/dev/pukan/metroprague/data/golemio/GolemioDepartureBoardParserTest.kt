package dev.pukan.metroprague.data.golemio

import dev.pukan.metroprague.domain.model.Line
import dev.pukan.metroprague.domain.model.Direction
import dev.pukan.metroprague.domain.model.forDirection
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GolemioDepartureBoardParserTest {
    private val parser = GolemioDepartureBoardParser()

    @Test
    fun `maps metro departures with predicted time and delay`() {
        val board = parser.parse(
            "muzeum",
            """{
                "departures": [
                    {
                        "route": {"type": 1, "short_name": "A"},
                        "trip": {"id": "trip-1", "headsign": "Depo Hostivař", "is_at_stop": false, "is_canceled": false},
                        "departure_timestamp": {"scheduled": "2026-09-26T10:00:00Z", "predicted": "2026-09-26T10:01:30Z"},
                        "delay": {"is_available": true, "seconds": 90}
                    }
                ],
                "infotexts": [{"text": "Výluka", "text_en": "Disruption"}]
            }""".trimIndent(),
        )

        assertEquals("muzeum", board.stationId)
        assertEquals(listOf("Výluka"), board.infoTexts)
        val departure = board.departures.single()
        assertEquals(Line.A, departure.line)
        assertEquals("Depo Hostivař", departure.headsign)
        assertEquals(Instant.parse("2026-09-26T10:00:00Z"), departure.scheduled)
        assertEquals(Instant.parse("2026-09-26T10:01:30Z"), departure.predicted)
        assertEquals(90, departure.delaySeconds)
        assertFalse(departure.isCanceled)
    }

    @Test
    fun `ignores other transport types and supports scheduled only metro service`() {
        val board = parser.parse(
            "andel",
            """{
                "departures": [
                    {
                        "route": {"type": 0, "short_name": "9"},
                        "trip": {"id": "tram", "headsign": "Spojovací", "is_at_stop": false, "is_canceled": false},
                        "departure_timestamp": {"scheduled": "2026-09-26T10:00:00Z"}
                    },
                    {
                        "route": {"type": 1, "short_name": "B"},
                        "trip": {"id": "metro", "headsign": "Zličín", "is_at_stop": true, "is_canceled": false},
                        "departure_timestamp": {"scheduled": "2026-09-26T10:03:00Z", "predicted": null},
                        "delay": {"is_available": false, "seconds": null}
                    }
                ],
                "infotexts": []
            }""".trimIndent(),
        )

        assertEquals(1, board.departures.size)
        assertEquals("metro", board.departures.single().tripId)
        assertNull(board.departures.single().predicted)
        assertNull(board.departures.single().delaySeconds)
        assertTrue(board.departures.single().isAtStop)
    }

    @Test
    fun `empty departure board is a valid response`() {
        val board = parser.parse("haje", """{"departures": [], "infotexts": []}""")

        assertEquals("haje", board.stationId)
        assertTrue(board.departures.isEmpty())
        assertTrue(board.infoTexts.isEmpty())
    }

    @Test
    fun `short turn is assigned by metro platform instead of headsign`() {
        val board = parser.parse(
            "muzeum",
            """{
                "departures": [{
                    "route": {"type": 1, "short_name": "C"},
                    "trip": {"id": "short-turn", "headsign": "Pražského povstání", "is_at_stop": false, "is_canceled": false},
                    "stop": {"id": "U400Z122P"},
                    "departure_timestamp": {"scheduled": "2026-09-28T12:00:00+02:00", "predicted": null}
                }]
            }""".trimIndent(),
        )

        val towardHaje = Direction(Line.C, "haje", "Háje")
        assertEquals("short-turn", board.forDirection(towardHaje).single().tripId)
        assertEquals(Instant.parse("2026-09-28T10:00:00Z"), board.departures.single().scheduled)
    }
}
