package dev.pukan.metroprague.data.repository

import dev.pukan.metroprague.domain.model.Line
import dev.pukan.metroprague.domain.model.directionsAt
import dev.pukan.metroprague.domain.model.forDirection
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MockDepartureRepositoryTest {

    private val zone = ZoneId.of("Europe/Prague")
    private val stationRepository = MockStationRepository()

    @Test
    fun `peak board has three sorted future departures for both directions`() = runTest {
        val now = Instant.parse("2026-01-15T07:00:00Z")
        val board = repositoryAt(now).getDepartureBoard("andel").first()

        assertEquals(setOf("Zličín", "Černý Most"), board.departures.map { it.headsign }.toSet())
        assertEquals(listOf(3, 3), board.departures.groupingBy { it.headsign }.eachCount().values.sorted())
        assertTrue(board.departures.all { it.effectiveTime > now })
        assertEquals(
            board.departures.map { it.effectiveTime }.sorted(),
            board.departures.map { it.effectiveTime },
        )
    }

    @Test
    fun `peak departures within one direction are 120 seconds apart`() = runTest {
        val board = repositoryAt(Instant.parse("2026-01-15T07:00:00Z"))
            .getDepartureBoard("andel")
            .first()

        assertEquals(listOf(120L, 120L), gapsFor(board.departures, "Zličín"))
    }

    @Test
    fun `off-peak departures within one direction are 300 seconds apart`() = runTest {
        val board = repositoryAt(Instant.parse("2026-01-15T11:00:00Z"))
            .getDepartureBoard("andel")
            .first()

        assertEquals(listOf(300L, 300L), gapsFor(board.departures, "Zličín"))
    }

    @Test
    fun `mocked boards include delayed trips across successive departures`() = runTest {
        val firstInstant = Instant.parse("2026-01-15T07:00:00Z")
        val departures = (0..5).flatMap { step ->
            repositoryAt(firstInstant.plusSeconds(step * 120L))
                .getDepartureBoard("andel")
                .first()
                .departures
        }

        assertTrue(departures.any { it.delaySeconds == 60 && it.predicted == it.scheduled.plusSeconds(60) })
        assertTrue(departures.any { it.delaySeconds == 0 })
    }

    @Test
    fun `no-service window produces an empty board`() = runTest {
        val board = repositoryAt(Instant.parse("2026-01-15T01:00:00Z"))
            .getDepartureBoard("andel")
            .first()

        assertTrue(board.departures.isEmpty())
    }

    @Test
    fun `terminus has one direction and interchange has four directions`() = runTest {
        val repository = repositoryAt(Instant.parse("2026-01-15T07:00:00Z"))
        val hajeBoard = repository.getDepartureBoard("haje").first()
        val muzeumBoard = repository.getDepartureBoard("muzeum").first()

        assertEquals(1, hajeBoard.departures.map { it.line to it.headsign }.toSet().size)
        assertEquals(4, muzeumBoard.departures.map { it.line to it.headsign }.toSet().size)
    }

    @Test
    fun `opposing directions have different departure instants`() = runTest {
        val board = repositoryAt(Instant.parse("2026-01-15T07:00:00Z"))
            .getDepartureBoard("andel")
            .first()
        val departuresByDirection = board.departures.groupBy { it.headsign }

        assertTrue(
            departuresByDirection.getValue("Zličín").map { it.effectiveTime }.toSet()
                .intersect(
                    departuresByDirection.getValue("Černý Most").map { it.effectiveTime }.toSet(),
                )
                .isEmpty(),
        )
    }

    @Test
    fun `forDirection matches both line and headsign`() = runTest {
        val allStations = stationRepository.getStations().first()
        val muzeum = allStations.first { it.id == "muzeum" }
        val direction = allStations.directionsAt(muzeum)
            .first { it.line == Line.A && it.terminusName == "Depo Hostivař" }
        val board = repositoryAt(Instant.parse("2026-01-15T07:00:00Z"))
            .getDepartureBoard(muzeum.id)
            .first()

        val matchingDepartures = board.forDirection(direction)

        assertEquals(3, matchingDepartures.size)
        assertTrue(matchingDepartures.all { it.line == Line.A })
        assertTrue(matchingDepartures.all { it.headsign == "Depo Hostivař" })
    }

    @Test
    fun `unknown station id produces an empty board`() = runTest {
        val board = repositoryAt(Instant.parse("2026-01-15T07:00:00Z"))
            .getDepartureBoard("unknown")
            .first()

        assertEquals("unknown", board.stationId)
        assertTrue(board.departures.isEmpty())
    }

    private fun repositoryAt(now: Instant): MockDepartureRepository = MockDepartureRepository(
        stationRepository = stationRepository,
        clock = Clock.fixed(now, zone),
    )

    private fun gapsFor(
        departures: List<dev.pukan.metroprague.domain.model.Departure>,
        headsign: String,
    ): List<Long> = departures
        .filter { it.headsign == headsign }
        .sortedBy { it.scheduled }
        .zipWithNext { first, second ->
            Duration.between(first.scheduled, second.scheduled).seconds
        }
}
