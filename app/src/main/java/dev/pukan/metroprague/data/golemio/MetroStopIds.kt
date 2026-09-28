package dev.pukan.metroprague.data.golemio

import dev.pukan.metroprague.domain.model.Line

/** GTFS metro platform IDs verified against Golemio departure boards on 2026-09-28. */
internal object MetroStopIds {
    private data class Platform(
        val id: String,
        val line: Line,
        val towardStationId: String,
    )

    private val byStationId: Map<String, List<Platform>> = mapOf(
        "nemocnice-motol" to listOf(platform("U306Z101P", Line.A, "depo-hostivar")),
        "dejvicka" to a("U321Z101P", "U321Z102P"),
        "hradcanska" to a("U163Z101P", "U163Z102P"),
        "malostranska" to a("U360Z101P", "U360Z102P"),
        "staromestska" to a("U703Z101P", "U703Z102P"),
        "mustek" to a("U1072Z101P", "U1072Z102P") +
            b("U1072Z121P", "U1072Z122P"),
        "muzeum" to a("U400Z101P", "U400Z102P") +
            c("U400Z121P", "U400Z122P"),
        "depo-hostivar" to listOf(
            platform("U1071Z101P", Line.A, "nemocnice-motol"),
            platform("U1071Z102P", Line.A, "nemocnice-motol"),
        ),
        "zlicin" to listOf(platform("U1141Z102P", Line.B, "cerny-most")),
        "andel" to b("U1040Z101P", "U1040Z102P"),
        "karlovo-namesti" to b("U237Z101P", "U237Z102P"),
        "narodni-trida" to b("U539Z101P", "U539Z102P"),
        "namesti-republiky" to b("U480Z101P", "U480Z102P"),
        "florenc" to b("U689Z101P", "U689Z102P") +
            c("U689Z121P", "U689Z122P"),
        "cerny-most" to listOf(platform("U897Z101P", Line.B, "zlicin")),
        "letnany" to listOf(platform("U1000Z102P", Line.C, "haje")),
        "hlavni-nadrazi" to c("U142Z101P", "U142Z102P"),
        "ip-pavlova" to c("U190Z101P", "U190Z102P"),
        "vysehrad" to c("U527Z101P", "U527Z102P"),
        "haje" to listOf(
            platform("U286Z101P", Line.C, "letnany"),
            platform("U286Z102P", Line.C, "letnany"),
        ),
    )

    fun idsFor(stationId: String): List<String> = byStationId[stationId]?.map(Platform::id).orEmpty()

    fun towardStationId(stationId: String, line: Line, stopId: String): String? =
        byStationId[stationId]
            ?.firstOrNull { it.id == stopId && it.line == line }
            ?.towardStationId

    private fun platform(id: String, line: Line, towardStationId: String) =
        Platform(id, line, towardStationId)

    private fun a(eastbound: String, westbound: String) = listOf(
        platform(eastbound, Line.A, "depo-hostivar"),
        platform(westbound, Line.A, "nemocnice-motol"),
    )

    private fun b(westbound: String, eastbound: String) = listOf(
        platform(westbound, Line.B, "zlicin"),
        platform(eastbound, Line.B, "cerny-most"),
    )

    private fun c(northbound: String, southbound: String) = listOf(
        platform(northbound, Line.C, "letnany"),
        platform(southbound, Line.C, "haje"),
    )
}
