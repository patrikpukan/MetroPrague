package dev.pukan.metroprague.data.repository

import dev.pukan.metroprague.data.golemio.GolemioDepartureBoardParser
import dev.pukan.metroprague.data.golemio.MemoryMetroStore
import dev.pukan.metroprague.data.golemio.MetroDataSnapshot
import dev.pukan.metroprague.data.golemio.MetroTestData
import dev.pukan.metroprague.data.golemio.SavedDepartureBoard
import dev.pukan.metroprague.ui.model.DepartureDisplay
import dev.pukan.metroprague.ui.model.toDisplay
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

    @Test
    fun `no saved catalogue prompts a reload`() = runTest {
        val board = repository(now, MemoryMetroStore()).getDepartureBoard("andel").first()
        assertTrue(board.isStale)
        assertTrue(board.isUnavailable)
    }

    @Test
    fun `unreadable saved board is unavailable instead of failing`() = runTest {
        val broken = MemoryMetroStore(MetroDataSnapshot(
            catalogue = MetroTestData.catalogue,
            boards = mapOf("andel" to SavedDepartureBoard("not json", now.toEpochMilli())),
        ))
        val board = repository(now, broken).getDepartureBoard("andel").first()
        assertTrue(board.isUnavailable)
        assertFalse(board.isStale)
    }

    @Test
    fun `at stop departure with past timestamp is displayed until the snapshot expires`() = runTest {
        val source = pastDeparture(isAtStop = true)
        val board = repository(now.plusSeconds(299), source).getDepartureBoard("andel").first()

        assertFalse(board.isStale)
        assertEquals(
            DepartureDisplay.AtStation,
            board.departures.single().toDisplay(now.plusSeconds(299), ZoneOffset.UTC),
        )
        val expired = repository(now.plusSeconds(300), source).getDepartureBoard("andel").first()
        assertTrue(expired.isStale)
        assertTrue(expired.departures.isEmpty())
    }

    @Test
    fun `ordinary past departure is still filtered in a fresh snapshot`() = runTest {
        val board = repository(now, pastDeparture(isAtStop = false)).getDepartureBoard("andel").first()
        assertFalse(board.isStale)
        assertTrue(board.departures.isEmpty())
    }

    private fun pastDeparture(isAtStop: Boolean): MemoryMetroStore {
        val response = MetroTestData.board
            .replace("2026-10-14T12:03:00Z", "2026-10-14T11:59:59Z")
            .replace("2026-10-14T12:04:00Z", "2026-10-14T11:59:59Z")
            .replace("\"is_at_stop\": false", "\"is_at_stop\": $isAtStop")
        return MemoryMetroStore(MetroDataSnapshot(
            catalogue = MetroTestData.catalogue,
            boards = mapOf("andel" to SavedDepartureBoard(response, now.toEpochMilli())),
        ))
    }

    private fun repository(time: Instant, source: MemoryMetroStore = store) = GolemioDepartureRepository(
        source, GolemioDepartureBoardParser(), Clock.fixed(time, ZoneOffset.UTC),
    )
}
