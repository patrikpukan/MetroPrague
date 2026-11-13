package dev.pukan.metroprague.di

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LiveDeparturesConfigTest {
    @Test
    fun debugBuildWithKeyUsesLiveDepartures() {
        assertTrue(LiveDeparturesConfig("test-key", isDebuggable = true).useLiveDepartures)
    }

    @Test
    fun missingKeyOrReleaseBuildUsesMockDepartures() {
        assertFalse(LiveDeparturesConfig("", isDebuggable = true).useLiveDepartures)
        assertFalse(LiveDeparturesConfig("test-key", isDebuggable = false).useLiveDepartures)
    }
}
