package dev.pukan.metroprague.data.repository

import dev.pukan.metroprague.data.golemio.*
import dev.pukan.metroprague.di.LiveDeparturesConfig
import dev.pukan.metroprague.domain.repository.MetroReloadError
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
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
        val appScope = CoroutineScope(Job())
        var calls = 0
        val repository = repository(GolemioBoardService { calls++; entered.complete(Unit); wait.await() }, scope = appScope)
        val job = launch { repository.reload() }
        entered.await()
        repository.reload()
        assertEquals(1, calls)
        appScope.cancel()
        job.join()
        assertEquals(previous, store.data.value)
        assertFalse(repository.status.first().isReloading)
        assertEquals(0, store.writes)
    }

    @Test
    fun `cancelling the caller does not cancel the reload`() = runTest {
        val entered = CompletableDeferred<Unit>()
        val wait = CompletableDeferred<Unit>()
        val repository = repository(GolemioBoardService {
            entered.complete(Unit)
            wait.await()
            """{"departures":[]}"""
        })
        val caller = launch { repository.reload() }
        entered.await()
        caller.cancelAndJoin()
        wait.complete(Unit)
        assertEquals(58, store.data.first { it.boards.isNotEmpty() }.boards.size)
        assertEquals(1, store.writes)
    }

    @Test
    fun `missing development key never fetches`() = runTest {
        val repository = repository(GolemioBoardService { error("Must not fetch") }, enabled = false)
        repository.reload()
        assertFalse(repository.status.first().canReload)
        assertEquals(0, store.writes)
    }

    private fun TestScope.repository(
        service: GolemioBoardService,
        enabled: Boolean = true,
        scope: CoroutineScope = backgroundScope,
    ) = GolemioMetroDataRepository(
        store, GolemioCatalogueLoader(MetroTestData.service), service, GolemioDepartureBoardParser(), clock,
        LiveDeparturesConfig(if (enabled) "test-key" else "", isDebuggable = true), scope,
    )
}
