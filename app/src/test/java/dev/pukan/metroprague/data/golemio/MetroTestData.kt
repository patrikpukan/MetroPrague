package dev.pukan.metroprague.data.golemio

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put

internal class MemoryMetroStore(initial: MetroDataSnapshot = MetroDataSnapshot()) : MetroSnapshotStore {
    override val data = MutableStateFlow(initial)
    var writes = 0
    override suspend fun replace(snapshot: MetroDataSnapshot) {
        writes++
        data.value = snapshot
    }
}

internal object MetroTestData {
    val routes = MetroNetwork.namesByLine.keys.map { line ->
        buildJsonObject {
            put("route_id", "L-${line.name}")
            put("route_short_name", line.name)
            put("route_type", 1)
        }
    }
    val stops = MetroNetwork.namesByLine.flatMap { (line, names) ->
        names.flatMapIndexed { index, name ->
            listOf("up", "down").map { direction -> stop("${line.name}-$index-$direction", name) }
        }
    }
    private fun stop(id: String, name: String) = buildJsonObject {
        put("properties", buildJsonObject {
            put("stop_id", id)
            put("stop_name", name)
            put("location_type", 0)
            put("parent_station", "parent-${MetroNetwork.stationId(name)}")
            put("wheelchair_boarding", 1)
        })
    }
    // Flora remains in stop metadata even when not served in the published trip patterns.
    val patterns = MetroNetwork.namesByLine.flatMap { (line, names) ->
        listOf(false, true).map { reverse ->
            val indexed = names.withIndex().filter { it.value != "Flora" }
                .let { if (reverse) it.reversed() else it }
            buildJsonObject {
                put("route_id", "L-${line.name}")
                put("trip_id", "${line.name}-$reverse")
                put("shape_id", "${line.name}-$reverse")
                put("direction_id", if (reverse) 1 else 0)
                put("stop_times", buildJsonArray {
                    indexed.forEachIndexed { sequence, indexedName ->
                        val id = "${line.name}-${indexedName.index}-${if (reverse) "down" else "up"}"
                        add(buildJsonObject {
                            put("stop_id", id)
                            put("stop_sequence", sequence + 1)
                            put("stop", stop(id, indexedName.value))
                        })
                    }
                })
            }
        }
    }
    val catalogue = parseMetroCatalogue(routes, stops, patterns)
    val service = object : GolemioCatalogueService {
        override suspend fun routes() = Json.encodeToString(kotlinx.serialization.json.JsonArray.serializer(), kotlinx.serialization.json.JsonArray(routes))
        override suspend fun stops(names: List<String>) = buildJsonObject { put("features", kotlinx.serialization.json.JsonArray(stops)) }.toString()
        override suspend fun trips(stopId: String, offset: Int) = if (stopId == MetroNetwork.seedPlatforms.first() && offset == 0) kotlinx.serialization.json.JsonArray(patterns).toString() else "[]"
        override suspend fun trip(id: String) = patterns.first { it.text("trip_id") == id }.toString()
    }
    val board = """{
        "departures": [{
            "route": {"type": 1, "short_name": "B"},
            "trip": {"id": "trip-1", "headsign": "Zličín", "is_at_stop": false, "is_canceled": false},
            "stop": {"id": "B-9-down"},
            "departure_timestamp": {"scheduled": "2026-10-14T12:03:00Z", "predicted": "2026-10-14T12:04:00Z"},
            "delay": {"is_available": true, "seconds": 60}
        }],
        "infotexts": [{"text":"Service information"}]
    }""".trimIndent()
}
