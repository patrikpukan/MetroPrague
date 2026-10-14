package dev.pukan.metroprague.data.golemio

import dev.pukan.metroprague.domain.model.Line
import dev.pukan.metroprague.domain.model.directionsAt
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
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

    @Test
    fun `a reversing train can use a platform in either direction`() {
        val reverse = MetroTestData.patterns.first { it.text("route_id") == "L-A" && it.text("direction_id") == "1" }
        val times = reverse.getValue("stop_times").jsonArray.drop(1).mapIndexed { index, element ->
            if (index == 0) JsonObject(element.jsonObject + ("stop_id" to JsonPrimitive("A-15-up"))) else element.jsonObject
        }
        val sharedPattern = JsonObject(reverse + ("stop_times" to JsonArray(times)))
        val catalogue = parseMetroCatalogue(MetroTestData.routes, MetroTestData.stops, MetroTestData.patterns + sharedPattern)
        assertNull(catalogue.platforms.first { it.id == "A-15-up" }.towardStationId)
        val parser = GolemioDepartureBoardParser()
        val base = MetroTestData.board.replace("\"short_name\": \"B\"", "\"short_name\": \"A\"")
            .replace("B-9-down", "A-15-up")
        assertEquals("nemocnice-motol", parser.parse("skalka", base.replace("Zličín", "Nemocnice Motol"), catalogue).departures.single().directionTerminusStationId)
        assertEquals("depo-hostivar", parser.parse("skalka", base.replace("Zličín", "Depo Hostivař"), catalogue).departures.single().directionTerminusStationId)
    }
}
