/// Metro-only fallback for platforms absent from current trip patterns.
///
/// Snapshot checked against PID stop-level metroA/metroB/metroC classifications on 2026-10-14.
/// Flora keeps its known metro GTFS platforms while the current PID stop list omits it.
/// Source: https://pid.cz/en/opendata/ (PID stop list); IDs matched to Golemio metadata.
/// Current metro trip patterns take precedence; update this list when the physical network
/// changes.
nonisolated enum MetroPlatformFallbacks {
    private static let byStation: [String: [(id: String, line: Line)]] = [
        "andel": b("U1040Z101P", "U1040Z102P"),
        "borislavka": a("U157Z101P", "U157Z102P"),
        "budejovicka": c("U50Z101P", "U50Z102P"),
        "cerny-most": b("U897Z101P", "U897Z102P"),
        "ceskomoravska": b("U510Z101P", "U510Z102P"),
        "chodov": c("U52Z101P", "U52Z102P"),
        "dejvicka": a("U321Z101P", "U321Z102P"),
        "depo-hostivar": a("U1071Z101P", "U1071Z102P"),
        "flora": a("U118Z101P", "U118Z102P"),
        "florenc": b("U689Z101P", "U689Z102P") + c("U689Z121P", "U689Z122P"),
        "haje": c("U286Z101P", "U286Z102P"),
        "hlavni-nadrazi": c("U142Z101P", "U142Z102P"),
        "hloubetin": b("U135Z101P", "U135Z102P"),
        "hradcanska": a("U163Z101P", "U163Z102P"),
        "hurka": b("U1154Z101P", "U1154Z102P"),
        "invalidovna": b("U655Z101P", "U655Z102P"),
        "ip-pavlova": c("U190Z101P", "U190Z102P"),
        "jinonice": b("U685Z101P", "U685Z102P"),
        "jiriho-z-podebrad": a("U209Z101P", "U209Z102P"),
        "kacerov": c("U228Z101P", "U228Z102P"),
        "karlovo-namesti": b("U237Z101P", "U237Z102P"),
        "kobylisy": c("U675Z101P", "U675Z102P"),
        "kolbenova": b("U75Z101P", "U75Z102P"),
        "krizikova": b("U758Z101P", "U758Z102P"),
        "ladvi": c("U78Z101P", "U78Z102P"),
        "letnany": c("U1000Z101P", "U1000Z102P"),
        "luka": b("U1007Z101P", "U1007Z102P"),
        "luziny": b("U258Z101P", "U258Z102P"),
        "malostranska": a("U360Z101P", "U360Z102P"),
        "mustek": a("U1072Z101P", "U1072Z102P") + b("U1072Z121P", "U1072Z122P"),
        "muzeum": a("U400Z101P", "U400Z102P") + c("U400Z121P", "U400Z122P"),
        "nadrazi-holesovice": c("U115Z101P", "U115Z102P"),
        "nadrazi-veleslavin": a("U462Z101P", "U462Z102P"),
        "namesti-miru": a("U476Z101P", "U476Z102P"),
        "namesti-republiky": b("U480Z101P", "U480Z102P"),
        "narodni-trida": b("U539Z101P", "U539Z102P"),
        "nemocnice-motol": a("U306Z101P", "U306Z102P"),
        "nove-butovice": b("U602Z101P", "U602Z102P"),
        "opatov": c("U106Z101P", "U106Z102P"),
        "palmovka": b("U529Z101P", "U529Z102P"),
        "pankrac": c("U385Z101P", "U385Z102P"),
        "petriny": a("U507Z101P", "U507Z102P"),
        "prazskeho-povstani": c("U597Z101P", "U597Z102P"),
        "prosek": c("U603Z101P", "U603Z102P"),
        "radlicka": b("U957Z101P", "U957Z102P"),
        "rajska-zahrada": b("U818Z101P", "U818Z102P"),
        "roztyly": c("U601Z101P", "U601Z102P"),
        "skalka": a("U953Z101P", "U953Z102P"),
        "smichovske-nadrazi": b("U458Z101P", "U458Z102P"),
        "staromestska": a("U703Z101P", "U703Z102P"),
        "stodulky": b("U1140Z101P", "U1140Z102P"),
        "strasnicka": a("U713Z101P", "U713Z102P"),
        "strizkov": c("U332Z101P", "U332Z102P"),
        "vltavska": c("U100Z101P", "U100Z102P"),
        "vysehrad": c("U527Z101P", "U527Z102P"),
        "vysocanska": b("U474Z101P", "U474Z102P"),
        "zelivskeho": a("U921Z101P", "U921Z102P"),
        "zlicin": b("U1141Z101P", "U1141Z102P"),
    ]

    static func find(stationID: String, stopID: String) -> MetroPlatform? {
        byStation[stationID]?
            .first { $0.id == stopID }
            .map { MetroPlatform(id: $0.id, stationID: stationID, line: $0.line) }
    }

    private static func a(_ ids: String...) -> [(id: String, line: Line)] { ids.map { ($0, .a) } }
    private static func b(_ ids: String...) -> [(id: String, line: Line)] { ids.map { ($0, .b) } }
    private static func c(_ ids: String...) -> [(id: String, line: Line)] { ids.map { ($0, .c) } }
}
