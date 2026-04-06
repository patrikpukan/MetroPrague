package dev.pukan.metroprague.data.repository

import dev.pukan.metroprague.domain.model.Line
import dev.pukan.metroprague.domain.model.Station
import dev.pukan.metroprague.domain.repository.StationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class MockStationRepository : StationRepository {
    private val stations = listOf(
        Station("a1", "Dejvická", Line.A),
        Station("a2", "Hradčanská", Line.A),
        Station("a3", "Malostranská", Line.A),
        Station("b1", "Anděl", Line.B),
        Station("b2", "Karlovo náměstí", Line.B),
        Station("b3", "Národní třída", Line.B),
        Station("c1", "I. P. Pavlova", Line.C),
        Station("c2", "Muzeum", Line.C),
        Station("c3", "Hlavní nádraží", Line.C)
    )

    override fun getStations(): Flow<List<Station>> {
        return flowOf(stations)
    }
}