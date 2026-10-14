package dev.pukan.metroprague.data.golemio

import dev.pukan.metroprague.domain.model.Departure
import dev.pukan.metroprague.domain.model.DepartureBoard
import dev.pukan.metroprague.domain.model.Line
import java.time.Instant
import javax.inject.Inject
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/** Maps the public PID v2 departure-board response to the app's current metro model. */
internal class GolemioDepartureBoardParser @Inject constructor() {
    fun parse(stationId: String, response: String, catalogue: MetroCatalogue? = null): DepartureBoard {
        val root = Json.parseToJsonElement(response).jsonObject
        val departures = root.getValue("departures").jsonArray.mapNotNull { item ->
            parseDeparture(stationId, item.jsonObject, catalogue)
        }.sortedBy { it.effectiveTime }
        val infoTexts = root["infotexts"]?.jsonArray?.mapNotNull { item ->
            item.jsonObject.stringOrNull("text")?.takeIf(String::isNotBlank)
        }.orEmpty()

        return DepartureBoard(stationId, departures, infoTexts)
    }

    private fun parseDeparture(stationId: String, item: JsonObject, catalogue: MetroCatalogue?): Departure? {
        val route = item.getValue("route").jsonObject
        if (route.getValue("type").jsonPrimitive.intOrNull != METRO_ROUTE_TYPE) return null
        val line = when (route.stringOrNull("short_name")) {
            "A" -> Line.A
            "B" -> Line.B
            "C" -> Line.C
            else -> return null
        }
        val trip = item.getValue("trip").jsonObject
        val stopId = item["stop"]?.jsonObject?.stringOrNull("id")
        val timestamp = item.getValue("departure_timestamp").jsonObject
        val delay = item["delay"]?.jsonObject

        return Departure(
            tripId = trip.getValue("id").jsonPrimitive.content,
            line = line,
            headsign = trip.getValue("headsign").jsonPrimitive.content,
            scheduled = Instant.parse(timestamp.getValue("scheduled").jsonPrimitive.content),
            predicted = timestamp.stringOrNull("predicted")?.let(Instant::parse),
            delaySeconds = if (delay?.get("is_available")?.jsonPrimitive?.content == "true") {
                delay["seconds"]?.jsonPrimitive?.intOrNull
            } else {
                null
            },
            isAtStop = trip.getValue("is_at_stop").jsonPrimitive.content == "true",
            isCanceled = trip.getValue("is_canceled").jsonPrimitive.content == "true",
            directionTerminusStationId = if (catalogue == null) {
                stopId?.let { MetroStopIds.towardStationId(stationId, line, it) }
            } else {
                catalogue.directionFor(stationId, line, stopId, trip.stringOrNull("headsign"),
                    item["last_stop"]?.jsonObject?.stringOrNull("id"))
            },
        )
    }

    private fun JsonObject.stringOrNull(key: String): String? =
        this[key]?.jsonPrimitive?.contentOrNull

    private companion object {
        const val METRO_ROUTE_TYPE = 1
    }
}

private fun MetroCatalogue.directionFor(
    stationId: String,
    line: Line,
    stopId: String?,
    headsign: String?,
    lastStopId: String?,
): String? {
    platforms.firstOrNull { it.id == stopId && it.stationId == stationId && it.line == line.name }
        ?.towardStationId?.let { return it }
    val ordered = stations.filter { line.name in it.lineOrders }.sortedBy { it.lineOrders.getValue(line.name) }
    val current = ordered.firstOrNull { it.id == stationId } ?: return null
    val target = ordered.firstOrNull { it.name == headsign }
    val previousStationId = platforms.firstOrNull { it.id == lastStopId }?.stationId
    val previous = ordered.firstOrNull { it.id == previousStationId }
    val currentOrder = current.lineOrders.getValue(line.name)
    val increasing = when {
        target != null && target.id != stationId -> target.lineOrders.getValue(line.name) > currentOrder
        previous != null && previous.id != stationId -> previous.lineOrders.getValue(line.name) < currentOrder
        else -> return null
    }
    return if (increasing) ordered.last().id else ordered.first().id
}
