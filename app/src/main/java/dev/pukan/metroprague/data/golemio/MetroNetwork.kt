package dev.pukan.metroprague.data.golemio

import dev.pukan.metroprague.domain.model.Line
import java.text.Normalizer
import java.util.Locale

/** Physical network topology, including stations temporarily omitted from GTFS trip patterns. */
internal object MetroNetwork {
    val namesByLine = mapOf(
        Line.A to listOf(
            "Nemocnice Motol", "Petřiny", "Nádraží Veleslavín", "Bořislavka", "Dejvická",
            "Hradčanská", "Malostranská", "Staroměstská", "Můstek", "Muzeum", "Náměstí Míru",
            "Jiřího z Poděbrad", "Flora", "Želivského", "Strašnická", "Skalka", "Depo Hostivař",
        ),
        Line.B to listOf(
            "Zličín", "Stodůlky", "Luka", "Lužiny", "Hůrka", "Nové Butovice", "Jinonice",
            "Radlická", "Smíchovské nádraží", "Anděl", "Karlovo náměstí", "Národní třída",
            "Můstek", "Náměstí Republiky", "Florenc", "Křižíkova", "Invalidovna", "Palmovka",
            "Českomoravská", "Vysočanská", "Kolbenova", "Hloubětín", "Rajská zahrada", "Černý Most",
        ),
        Line.C to listOf(
            "Letňany", "Prosek", "Střížkov", "Ládví", "Kobylisy", "Nádraží Holešovice",
            "Vltavská", "Florenc", "Hlavní nádraží", "Muzeum", "I. P. Pavlova", "Vyšehrad",
            "Pražského povstání", "Pankrác", "Budějovická", "Kačerov", "Roztyly", "Chodov",
            "Opatov", "Háje",
        ),
    )

    /** Existing central platform IDs bootstrap GTFS patterns, including during non-service hours. */
    val seedPlatforms = listOf(
        "U1072Z101P", "U1072Z102P", "U1072Z121P", "U1072Z122P", "U400Z121P", "U400Z122P",
    )

    fun stationId(name: String): String = if (name == "I. P. Pavlova") {
        "ip-pavlova"
    } else {
        Normalizer.normalize(name, Normalizer.Form.NFD)
            .replace(Regex("\\p{M}+"), "")
            .lowercase(Locale.ROOT)
            .replace(Regex("[^a-z0-9]+"), "-")
            .trim('-')
    }
}
