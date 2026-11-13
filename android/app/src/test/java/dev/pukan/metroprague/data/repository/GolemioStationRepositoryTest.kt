package dev.pukan.metroprague.data.repository

import dev.pukan.metroprague.data.golemio.MemoryMetroStore
import dev.pukan.metroprague.data.golemio.MetroDataSnapshot
import dev.pukan.metroprague.data.golemio.MetroTestData
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class GolemioStationRepositoryTest {
    @Test
    fun `stations are available before the first reload and match the saved catalogue`() = runTest {
        val before = GolemioStationRepository(MemoryMetroStore()).getStations().first()
        val after = GolemioStationRepository(MemoryMetroStore(MetroDataSnapshot(MetroTestData.catalogue)))
            .getStations().first()
        assertEquals(58, before.size)
        assertEquals(after, before)
    }
}
