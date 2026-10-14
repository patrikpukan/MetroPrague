package dev.pukan.metroprague.data.golemio

import dev.pukan.metroprague.domain.model.Line
import javax.inject.Inject
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

internal class GolemioCatalogueLoader @Inject constructor(
    private val service: GolemioCatalogueService,
) {
    suspend fun load(): MetroCatalogue {
        val routes = Json.parseToJsonElement(service.routes()).jsonArray.map { it.jsonObject }
            .filter { it.getValue("route_type").jsonPrimitive.int == 1 && it.text("route_short_name") in setOf("A", "B", "C") }
        require(routes.map { it.text("route_short_name") }.toSet() == setOf("A", "B", "C"))
        val routeIds = routes.map { it.text("route_id") }.toSet()
        val names = MetroNetwork.namesByLine.values.flatten().distinct()
        val stops = Json.parseToJsonElement(service.stops(names)).jsonObject
            .getValue("features").jsonArray.map { it.jsonObject }
        val patterns = linkedMapOf<String, JsonObject>()
        for (stopId in MetroNetwork.seedPlatforms) {
            var offset = 0
            do {
                val page = Json.parseToJsonElement(service.trips(stopId, offset)).jsonArray
                page.map { it.jsonObject }.filter { it.text("route_id") in routeIds }.forEach { trip ->
                    patterns.putIfAbsent(
                        "${trip.text("route_id")}:${trip.text("shape_id")}:${trip.text("direction_id")}",
                        trip,
                    )
                }
                offset += page.size
                require(offset <= 10_000) { "Too many trips for a metro platform" }
            } while (page.size == 1_000)
        }
        val details = patterns.values.map { Json.parseToJsonElement(service.trip(it.text("trip_id"))).jsonObject }
        return parseMetroCatalogue(routes, stops, details)
    }
}

/** Hydrates the full physical network with current GTFS platform, accessibility and route data. */
internal fun parseMetroCatalogue(
    routes: List<JsonObject>,
    stops: List<JsonObject>,
    patterns: List<JsonObject>,
): MetroCatalogue {
    val routeLines = routes.associate { it.text("route_id") to Line.valueOf(it.text("route_short_name")) }
    require(routeLines.values.toSet() == Line.entries.toSet())
    val metroStops = stops.filter {
        val props = it.getValue("properties").jsonObject
        props.getValue("location_type").jsonPrimitive.int == 0 &&
            props["parent_station"]?.jsonPrimitive?.content?.let { parent -> parent != "null" && parent.isNotBlank() } == true
    }
    val byName = metroStops.groupBy { it.getValue("properties").jsonObject.text("stop_name") }
    val stationLines = linkedMapOf<String, MutableMap<String, Int>>()
    MetroNetwork.namesByLine.forEach { (line, names) ->
        names.forEachIndexed { order, name ->
            require(!byName[name].isNullOrEmpty()) { "Missing metro station metadata: $name" }
            stationLines.getOrPut(name) { linkedMapOf() }[line.name] = order
        }
    }
    val stations = stationLines.map { (name, lines) -> SavedStation(MetroNetwork.stationId(name), name, lines) }
    val learnedPlatforms = linkedMapOf<String, MetroPlatform>()
    patterns.forEach { pattern ->
        val line = routeLines.getValue(pattern.text("route_id"))
        val names = MetroNetwork.namesByLine.getValue(line)
        val times = pattern.getValue("stop_times").jsonArray.map { it.jsonObject }
            .sortedBy { it.getValue("stop_sequence").jsonPrimitive.int }
        require(times.size >= 2)
        fun name(time: JsonObject) = time.getValue("stop").jsonObject.getValue("properties").jsonObject.text("stop_name")
        require(times.all { name(it) in names }) { "Metro network topology needs updating" }
        val start = names.indexOf(name(times.first()))
        val end = names.indexOf(name(times.last()))
        require(start != end)
        val toward = MetroNetwork.stationId(if (start < end) names.last() else names.first())
        // The final stop is an arrival platform; it does not establish a departure direction.
        times.dropLast(1).forEach { time ->
            val id = time.text("stop_id")
            val platform = MetroPlatform(id, MetroNetwork.stationId(name(time)), line.name, toward)
            require(learnedPlatforms[id]?.let { it == platform } != false) { "Ambiguous metro platform direction" }
            learnedPlatforms[id] = platform
        }
    }
    require(learnedPlatforms.values.map { it.line }.toSet() == setOf("A", "B", "C"))
    val platforms = metroStops.mapNotNull { stop ->
        val props = stop.getValue("properties").jsonObject
        val name = props.text("stop_name")
        val id = props.text("stop_id")
        learnedPlatforms[id] ?: stationLines[name]?.keys?.singleOrNull()?.let { line ->
            // Unserved/alternate terminal platforms still belong to a single-line station.
            MetroPlatform(id, MetroNetwork.stationId(name), line)
        }
    }.distinctBy { it.id }
    require(stations.all { station -> station.lineOrders.keys.all { line -> platforms.any { it.stationId == station.id && it.line == line } } })
    return MetroCatalogue(stations, platforms, routes, metroStops, patterns)
}

internal fun JsonObject.text(key: String): String = getValue(key).jsonPrimitive.content
