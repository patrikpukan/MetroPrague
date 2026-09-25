package dev.pukan.metroprague.domain.model

fun List<Station>.terminiOf(line: Line): List<Station> {
    val stationsByOrder = mapNotNull { station ->
        station.lines
            .firstOrNull { it.line == line }
            ?.let { station to it.order }
    }.sortedBy { (_, order) -> order }

    return when (stationsByOrder.size) {
        0 -> emptyList()
        1 -> listOf(stationsByOrder.first().first)
        else -> listOf(stationsByOrder.first().first, stationsByOrder.last().first)
    }
}

fun List<Station>.directionsAt(station: Station): List<Direction> = station.lines
    .sortedBy { it.line.ordinal }
    .flatMap { linePosition ->
        terminiOf(linePosition.line).mapNotNull { terminus ->
            val terminusOrder = terminus.lines
                .first { it.line == linePosition.line }
                .order

            if (terminusOrder == linePosition.order) {
                null
            } else {
                Direction(
                    line = linePosition.line,
                    terminusStationId = terminus.id,
                    terminusName = terminus.name,
                )
            }
        }
    }
