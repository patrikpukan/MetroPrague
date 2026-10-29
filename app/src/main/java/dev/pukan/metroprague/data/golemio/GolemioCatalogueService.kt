package dev.pukan.metroprague.data.golemio

import java.net.URLEncoder
import java.nio.charset.StandardCharsets

internal interface GolemioCatalogueService {
    suspend fun routes(): String
    suspend fun stops(names: List<String>): String
    suspend fun trips(stopId: String, offset: Int): String
    suspend fun trip(id: String): String
}

internal class HttpGolemioCatalogueService(
    private val client: GolemioHttpClient,
) : GolemioCatalogueService {
    override suspend fun routes() = client.get("$BASE/v2/gtfs/routes?limit=10000")

    override suspend fun stops(names: List<String>) = client.get(
        "$BASE/v2/gtfs/stops?" + names.joinToString("&") { "names%5B%5D=${encode(it)}" } +
            "&limit=10000",
    )

    override suspend fun trips(stopId: String, offset: Int) = client.get(
        "$BASE/v2/gtfs/trips?stopId=${encode(stopId)}&limit=1000&offset=$offset",
    )

    override suspend fun trip(id: String) = client.get(
        "$BASE/v2/gtfs/trips/${encode(id)}?includeStops=true&includeStopTimes=true",
    )

    private fun encode(value: String) = URLEncoder.encode(value, StandardCharsets.UTF_8.toString())

    private companion object {
        const val BASE = "https://api.golemio.cz"
    }
}
