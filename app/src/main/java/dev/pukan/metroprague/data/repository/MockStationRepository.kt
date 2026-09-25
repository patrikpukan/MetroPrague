package dev.pukan.metroprague.data.repository

import dev.pukan.metroprague.domain.model.Line
import dev.pukan.metroprague.domain.model.LinePosition
import dev.pukan.metroprague.domain.model.Station
import dev.pukan.metroprague.domain.repository.StationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject

class MockStationRepository @Inject constructor() : StationRepository {
    private val stations = listOf(
        Station("nemocnice-motol", "Nemocnice Motol", listOf(LinePosition(Line.A, 0))),
        Station("dejvicka", "Dejvická", listOf(LinePosition(Line.A, 4))),
        Station("hradcanska", "Hradčanská", listOf(LinePosition(Line.A, 5))),
        Station("malostranska", "Malostranská", listOf(LinePosition(Line.A, 6))),
        Station("staromestska", "Staroměstská", listOf(LinePosition(Line.A, 7))),
        Station(
            "mustek",
            "Můstek",
            listOf(LinePosition(Line.A, 8), LinePosition(Line.B, 12)),
        ),
        Station(
            "muzeum",
            "Muzeum",
            listOf(LinePosition(Line.A, 9), LinePosition(Line.C, 9)),
        ),
        Station("depo-hostivar", "Depo Hostivař", listOf(LinePosition(Line.A, 16))),
        Station("zlicin", "Zličín", listOf(LinePosition(Line.B, 0))),
        Station("andel", "Anděl", listOf(LinePosition(Line.B, 9))),
        Station("karlovo-namesti", "Karlovo náměstí", listOf(LinePosition(Line.B, 10))),
        Station("narodni-trida", "Národní třída", listOf(LinePosition(Line.B, 11))),
        Station("namesti-republiky", "Náměstí Republiky", listOf(LinePosition(Line.B, 13))),
        Station(
            "florenc",
            "Florenc",
            listOf(LinePosition(Line.B, 14), LinePosition(Line.C, 7)),
        ),
        Station("cerny-most", "Černý Most", listOf(LinePosition(Line.B, 23))),
        Station("letnany", "Letňany", listOf(LinePosition(Line.C, 0))),
        Station("hlavni-nadrazi", "Hlavní nádraží", listOf(LinePosition(Line.C, 8))),
        Station("ip-pavlova", "I. P. Pavlova", listOf(LinePosition(Line.C, 10))),
        Station("vysehrad", "Vyšehrad", listOf(LinePosition(Line.C, 11))),
        Station("haje", "Háje", listOf(LinePosition(Line.C, 19))),
    )

    override fun getStations(): Flow<List<Station>> {
        return flowOf(stations)
    }
}
