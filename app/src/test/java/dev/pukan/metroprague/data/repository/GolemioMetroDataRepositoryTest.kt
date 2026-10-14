package dev.pukan.metroprague.data.repository

import dev.pukan.metroprague.data.golemio.*
import dev.pukan.metroprague.di.LiveDeparturesConfig
import dev.pukan.metroprague.domain.repository.MetroReloadError
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class GolemioMetroDataRepositoryTest {
    private val clock = Clock.fixed(Instant.parse("2026-10-14T12:00:00Z"), ZoneOffset.UTC)
    private val previous = MetroDataSnapshot(MetroTestData.catalogue, reloadedAtMillis = 123)
    private val store = MemoryMetroStore(previous)

    @Test
    fun `manual reload saves all stations and boards in one write`() = runTest {
        var calls = 0
        val repository = repository(GolemioBoardService { calls++; """{"departures":[],"infotexts":[]}""" })
        assertEquals(0, calls)
        repository.status.first()
        assertEquals(0, calls)
        repository.reload()
        assertEquals(58, calls)
        assertEquals(1, store.writes)
        assertEquals(58, store.data.value.boards.size)
        assertEquals(clock.millis(), store.data.value.reloadedAtMillis)
        assertFalse(repository.status.first().isReloading)
        assertNull(repository.status.first().error)
    }

    @Test
    fun `partial download and HTTP failure keep previous snapshot`() = runTest {
        var calls = 0
        val repository = repository(GolemioBoardService {
            if (++calls == 3) throw GolemioHttpException(401)
            """{"departures":[]}"""
        })
        repository.reload()
        assertEquals(0, store.writes)
        assertEquals(previous, store.data.value)
        assertEquals(MetroReloadError.Authentication, repository.status.first().error)
    }

    @Test
    fun `simultaneous reload is ignored and cancellation keeps saved data`() = runTest {
        val entered = CompletableDeferred<Unit>()
        val wait = CompletableDeferred<String>()
        var calls = 0
        val repository = repository(GolemioBoardService { calls++; entered.complete(Unit); wait.await() })
        val job = launch { repository.reload() }
        entered.await()
        repository.reload()
        assertEquals(1, calls)
        job.cancel()
        job.join()
        assertEquals(previous, store.data.value)
        assertFalse(repository.status.first().isReloading)
        assertEquals(0, store.writes)
    }

    @Test
    fun `missing development key never fetches`() = runTest {
        val repository = repository(GolemioBoardService { error("Must not fetch") }, enabled = false)
        repository.reload()
        assertFalse(repository.status.first().canReload)
        assertEquals(0, store.writes)
    }

    private fun repository(service: GolemioBoardService, enabled: Boolean = true) = GolemioMetroDataRepository(
        store, GolemioCatalogueLoader(MetroTestData.service), service, GolemioDepartureBoardParser(), clock,
        LiveDeparturesConfig(if (enabled) "test-key" else "", isDebuggable = true),
    )
}
