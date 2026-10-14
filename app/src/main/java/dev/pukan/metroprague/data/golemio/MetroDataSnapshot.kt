package dev.pukan.metroprague.data.golemio

import dev.pukan.metroprague.domain.model.Line
import dev.pukan.metroprague.domain.model.LinePosition
import dev.pukan.metroprague.domain.model.Station
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
internal data class MetroDataSnapshot(
    val catalogue: MetroCatalogue? = null,
    val boards: Map<String, SavedDepartureBoard> = emptyMap(),
    val reloadedAtMillis: Long? = null,
    val departureCount: Int = 0,
)

@Serializable
internal data class MetroCatalogue(
    val stations: List<SavedStation>,
    val platforms: List<MetroPlatform>,
    val routes: List<JsonObject>,
    val stops: List<JsonObject>,
    val tripPatterns: List<JsonObject>,
) {
    fun domainStations(): List<Station> = stations.map { station ->
        Station(station.id, station.name, station.lineOrders.map { (line, order) ->
            LinePosition(Line.valueOf(line), order)
        })
    }
}

@Serializable
internal data class SavedStation(
    val id: String,
    val name: String,
    val lineOrders: Map<String, Int>,
)

@Serializable
internal data class MetroPlatform(
    val id: String,
    val stationId: String,
    val line: String,
    val towardStationId: String? = null,
)

@Serializable
internal data class SavedDepartureBoard(
    val response: String,
    val fetchedAtMillis: Long,
)
