package dev.pukan.metroprague.data.golemio

import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

internal fun interface GolemioBoardService {
    suspend fun getDepartures(stopIds: List<String>): String
}

internal class HttpGolemioBoardService(
    private val client: GolemioHttpClient,
) : GolemioBoardService {
    override suspend fun getDepartures(stopIds: List<String>): String = client.get(boardUrl(stopIds))
}

/** One paced transport shared by metadata and board requests. Never logs credentials. */
internal class GolemioHttpClient(private val apiKey: String) {
    private val mutex = Mutex()
    private var nextRequestAtNanos = 0L

    suspend fun get(url: String): String = mutex.withLock {
        val waitMillis = (nextRequestAtNanos - System.nanoTime()) / 1_000_000
        if (waitMillis > 0) delay(waitMillis)
        try {
            request(url)
        } finally {
            nextRequestAtNanos = maxOf(nextRequestAtNanos, System.nanoTime() + 600_000_000L)
        }
    }

    private suspend fun request(url: String): String = withContext(Dispatchers.IO) {
        val connection = URL(url).openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "GET"
            connection.connectTimeout = 10_000
            connection.readTimeout = 10_000
            connection.setRequestProperty("Accept", "application/json")
            connection.setRequestProperty("X-Access-Token", apiKey)
            val status = connection.responseCode
            if (status !in 200..299) {
                if (status == 429) {
                    val retrySeconds = connection.getHeaderField("Retry-After")?.toLongOrNull()
                        ?.coerceIn(1, 3_600) ?: 8
                    nextRequestAtNanos = System.nanoTime() + retrySeconds * 1_000_000_000L
                }
                throw GolemioHttpException(status)
            }
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
