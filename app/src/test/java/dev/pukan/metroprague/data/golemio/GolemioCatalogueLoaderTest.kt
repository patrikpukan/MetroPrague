package dev.pukan.metroprague.data.golemio

import dev.pukan.metroprague.domain.model.Line
import dev.pukan.metroprague.domain.model.directionsAt
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class GolemioCatalogueLoaderTest {
    @Test
    fun `loads complete catalogue retaining closed stations and favorite IDs`() = runTest {
        val catalogue = GolemioCatalogueLoader(MetroTestData.service).load()
        val stations = catalogue.domainStations()
        assertEquals(58, stations.size)
        assertEquals(17, stations.count { s -> s.lines.any { it.line == Line.A } })
        assertEquals(24, stations.count { s -> s.lines.any { it.line == Line.B } })
        assertEquals(20, stations.count { s -> s.lines.any { it.line == Line.C } })
        assertEquals(4, stations.directionsAt(stations.first { it.id == "mustek" }).size)
        assertTrue(stations.any { it.id == "ip-pavlova" })
        assertTrue(stations.any { it.id == "flora" })
        assertTrue(catalogue.platforms.any { it.stationId == "flora" })
    }

    @Test(expected = IllegalArgumentException::class)
    fun `incomplete station metadata fails validation`() {
        parseMetroCatalogue(MetroTestData.routes, emptyList(), MetroTestData.patterns)
    }

    @Test
    fun `new platform uses short turn destination to select full line direction`() {
        val response = MetroTestData.board.replace("B-9-down", "new-platform").replace("Zličín", "Smíchovské nádraží")
        val board = GolemioDepartureBoardParser().parse("andel", response, MetroTestData.catalogue)
        assertEquals("zlicin", board.departures.single().directionTerminusStationId)
        assertEquals("Smíchovské nádraží", board.departures.single().headsign)
    }
}
