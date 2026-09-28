package dev.pukan.metroprague.data.golemio

import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal fun interface GolemioBoardService {
    suspend fun getDepartures(stopIds: List<String>): String
}

internal class HttpGolemioBoardService(
    private val apiKey: String,
) : GolemioBoardService {
    override suspend fun getDepartures(stopIds: List<String>): String = withContext(Dispatchers.IO) {
        val connection = URL(boardUrl(stopIds)).openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "GET"
            connection.connectTimeout = 10_000
            connection.readTimeout = 10_000
            connection.setRequestProperty("Accept", "application/json")
            connection.setRequestProperty("X-Access-Token", apiKey)
            val status = connection.responseCode
            if (status !in 200..299) throw GolemioHttpException(status)
            connection.inputStream.bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }
}

internal fun boardUrl(stopIds: List<String>): String {
    require(stopIds.isNotEmpty())
    val idsQuery = stopIds.joinToString("&") { stopId ->
        "ids%5B%5D=${URLEncoder.encode(stopId, StandardCharsets.UTF_8.toString())}"
    }
    return "https://api.golemio.cz/v2/pid/departureboards" +
        "?$idsQuery&minutesAfter=60&limit=30&mode=departures&order=real"
}

internal class GolemioHttpException(val statusCode: Int) :
    IOException("Golemio returned HTTP $statusCode")
