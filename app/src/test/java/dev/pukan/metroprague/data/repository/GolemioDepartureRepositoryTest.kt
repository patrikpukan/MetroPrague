package dev.pukan.metroprague.data.repository

import dev.pukan.metroprague.data.golemio.GolemioDepartureBoardParser
import dev.pukan.metroprague.data.golemio.MemoryMetroStore
import dev.pukan.metroprague.data.golemio.MetroDataSnapshot
import dev.pukan.metroprague.data.golemio.MetroTestData
import dev.pukan.metroprague.data.golemio.SavedDepartureBoard
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class GolemioDepartureRepositoryTest {
    private val now = Instant.parse("2026-10-14T12:00:00Z")
    private val store = MemoryMetroStore(MetroDataSnapshot(
        catalogue = MetroTestData.catalogue,
        boards = mapOf("andel" to SavedDepartureBoard(MetroTestData.board, now.toEpochMilli())),
    ))

    @Test
    fun `reads saved data and direction without fetching or writing`() = runTest {
        val board = repository(now).getDepartureBoard("andel").first()
        assertEquals("zlicin", board.departures.single().directionTerminusStationId)
        assertEquals(listOf("Service information"), board.infoTexts)
        assertFalse(board.isStale)
        assertFalse(board.isUnavailable)
        assertEquals(0, store.writes)
    }

    @Test
    fun `expired snapshots are stale and past trains are removed`() = runTest {
        val board = repository(now.plusSeconds(300)).getDepartureBoard("andel").first()
        assertTrue(board.isStale)
        assertTrue(board.departures.isEmpty())
        assertEquals(now, board.fetchedAt)
    }

    @Test
    fun `unknown station is unavailable`() = runTest {
        assertTrue(repository(now).getDepartureBoard("unknown").first().isUnavailable)
    }

    private fun repository(time: Instant) = GolemioDepartureRepository(
        store, GolemioDepartureBoardParser(), Clock.fixed(time, ZoneOffset.UTC),
    )
}
