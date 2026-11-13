package dev.pukan.metroprague.data.golemio

/**
 * Metro-only fallback for platforms absent from current trip patterns.
 * Snapshot checked against PID stop-level metroA/metroB/metroC classifications on 2026-10-14.
 * Flora retains its known metro GTFS platforms while omitted by the current PID stop list.
 * Source: https://pid.cz/en/opendata/ (PID stop list); IDs matched to Golemio metadata.
 * Current metro trip patterns take precedence; update this list when the physical network changes.
 */
internal object MetroPlatformFallbacks {
    private val byStation = mapOf(
        "andel" to b("U1040Z101P", "U1040Z102P"),
        "borislavka" to a("U157Z101P", "U157Z102P"),
        "budejovicka" to c("U50Z101P", "U50Z102P"),
        "cerny-most" to b("U897Z101P", "U897Z102P"),
        "ceskomoravska" to b("U510Z101P", "U510Z102P"),
        "chodov" to c("U52Z101P", "U52Z102P"),
        "dejvicka" to a("U321Z101P", "U321Z102P"),
        "depo-hostivar" to a("U1071Z101P", "U1071Z102P"),
        "flora" to a("U118Z101P", "U118Z102P"),
        "florenc" to b("U689Z101P", "U689Z102P") + c("U689Z121P", "U689Z122P"),
        "haje" to c("U286Z101P", "U286Z102P"),
        "hlavni-nadrazi" to c("U142Z101P", "U142Z102P"),
        "hloubetin" to b("U135Z101P", "U135Z102P"),
        "hradcanska" to a("U163Z101P", "U163Z102P"),
        "hurka" to b("U1154Z101P", "U1154Z102P"),
        "invalidovna" to b("U655Z101P", "U655Z102P"),
        "ip-pavlova" to c("U190Z101P", "U190Z102P"),
        "jinonice" to b("U685Z101P", "U685Z102P"),
        "jiriho-z-podebrad" to a("U209Z101P", "U209Z102P"),
        "kacerov" to c("U228Z101P", "U228Z102P"),
        "karlovo-namesti" to b("U237Z101P", "U237Z102P"),
        "kobylisy" to c("U675Z101P", "U675Z102P"),
        "kolbenova" to b("U75Z101P", "U75Z102P"),
        "krizikova" to b("U758Z101P", "U758Z102P"),
        "ladvi" to c("U78Z101P", "U78Z102P"),
        "letnany" to c("U1000Z101P", "U1000Z102P"),
        "luka" to b("U1007Z101P", "U1007Z102P"),
        "luziny" to b("U258Z101P", "U258Z102P"),
        "malostranska" to a("U360Z101P", "U360Z102P"),
        "mustek" to a("U1072Z101P", "U1072Z102P") + b("U1072Z121P", "U1072Z122P"),
        "muzeum" to a("U400Z101P", "U400Z102P") + c("U400Z121P", "U400Z122P"),
        "nadrazi-holesovice" to c("U115Z101P", "U115Z102P"),
        "nadrazi-veleslavin" to a("U462Z101P", "U462Z102P"),
        "namesti-miru" to a("U476Z101P", "U476Z102P"),
        "namesti-republiky" to b("U480Z101P", "U480Z102P"),
        "narodni-trida" to b("U539Z101P", "U539Z102P"),
        "nemocnice-motol" to a("U306Z101P", "U306Z102P"),
        "nove-butovice" to b("U602Z101P", "U602Z102P"),
        "opatov" to c("U106Z101P", "U106Z102P"),
        "palmovka" to b("U529Z101P", "U529Z102P"),
        "pankrac" to c("U385Z101P", "U385Z102P"),
        "petriny" to a("U507Z101P", "U507Z102P"),
        "prazskeho-povstani" to c("U597Z101P", "U597Z102P"),
        "prosek" to c("U603Z101P", "U603Z102P"),
        "radlicka" to b("U957Z101P", "U957Z102P"),
        "rajska-zahrada" to b("U818Z101P", "U818Z102P"),
        "roztyly" to c("U601Z101P", "U601Z102P"),
        "skalka" to a("U953Z101P", "U953Z102P"),
        "smichovske-nadrazi" to b("U458Z101P", "U458Z102P"),
        "staromestska" to a("U703Z101P", "U703Z102P"),
        "stodulky" to b("U1140Z101P", "U1140Z102P"),
        "strasnicka" to a("U713Z101P", "U713Z102P"),
        "strizkov" to c("U332Z101P", "U332Z102P"),
        "vltavska" to c("U100Z101P", "U100Z102P"),
        "vysehrad" to c("U527Z101P", "U527Z102P"),
        "vysocanska" to b("U474Z101P", "U474Z102P"),
        "zelivskeho" to a("U921Z101P", "U921Z102P"),
        "zlicin" to b("U1141Z101P", "U1141Z102P"),
    )

    fun find(stationId: String, stopId: String): MetroPlatform? = byStation[stationId]
        ?.firstOrNull { it.first == stopId }
        ?.let { (id, line) -> MetroPlatform(id, stationId, line) }

    private fun a(vararg ids: String) = ids.map { it to "A" }
    private fun b(vararg ids: String) = ids.map { it to "B" }
    private fun c(vararg ids: String) = ids.map { it to "C" }
}
