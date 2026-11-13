/// GTFS metro platform IDs verified against Golemio departure boards on 2026-09-28.
///
/// Used to assign directions when a board is parsed without a saved catalogue.
nonisolated enum MetroStopIDs {
    private struct Platform {
        let id: String
        let line: Line
        let towardStationID: String
    }

    private static let byStationID: [String: [Platform]] = [
        "nemocnice-motol": [Platform(id: "U306Z101P", line: .a, towardStationID: "depo-hostivar")],
        "dejvicka": a("U321Z101P", "U321Z102P"),
        "hradcanska": a("U163Z101P", "U163Z102P"),
        "malostranska": a("U360Z101P", "U360Z102P"),
        "staromestska": a("U703Z101P", "U703Z102P"),
        "mustek": a("U1072Z101P", "U1072Z102P") + b("U1072Z121P", "U1072Z122P"),
        "muzeum": a("U400Z101P", "U400Z102P") + c("U400Z121P", "U400Z122P"),
        "depo-hostivar": [
            Platform(id: "U1071Z101P", line: .a, towardStationID: "nemocnice-motol"),
            Platform(id: "U1071Z102P", line: .a, towardStationID: "nemocnice-motol"),
        ],
        "zlicin": [Platform(id: "U1141Z102P", line: .b, towardStationID: "cerny-most")],
        "andel": b("U1040Z101P", "U1040Z102P"),
        "karlovo-namesti": b("U237Z101P", "U237Z102P"),
        "narodni-trida": b("U539Z101P", "U539Z102P"),
        "namesti-republiky": b("U480Z101P", "U480Z102P"),
        "florenc": b("U689Z101P", "U689Z102P") + c("U689Z121P", "U689Z122P"),
        "cerny-most": [Platform(id: "U897Z101P", line: .b, towardStationID: "zlicin")],
        "letnany": [Platform(id: "U1000Z102P", line: .c, towardStationID: "haje")],
        "hlavni-nadrazi": c("U142Z101P", "U142Z102P"),
        "ip-pavlova": c("U190Z101P", "U190Z102P"),
        "vysehrad": c("U527Z101P", "U527Z102P"),
        "haje": [
            Platform(id: "U286Z101P", line: .c, towardStationID: "letnany"),
            Platform(id: "U286Z102P", line: .c, towardStationID: "letnany"),
        ],
    ]

    static func ids(for stationID: String) -> [String] {
        byStationID[stationID]?.map(\.id) ?? []
    }

    static func towardStationID(stationID: String, line: Line, stopID: String) -> String? {
        byStationID[stationID]?.first { $0.id == stopID && $0.line == line }?.towardStationID
    }

    private static func a(_ eastbound: String, _ westbound: String) -> [Platform] {
        [
            Platform(id: eastbound, line: .a, towardStationID: "depo-hostivar"),
            Platform(id: westbound, line: .a, towardStationID: "nemocnice-motol"),
        ]
    }

    private static func b(_ westbound: String, _ eastbound: String) -> [Platform] {
        [
            Platform(id: westbound, line: .b, towardStationID: "zlicin"),
            Platform(id: eastbound, line: .b, towardStationID: "cerny-most"),
        ]
    }

    private static func c(_ northbound: String, _ southbound: String) -> [Platform] {
        [
            Platform(id: northbound, line: .c, towardStationID: "letnany"),
            Platform(id: southbound, line: .c, towardStationID: "haje"),
        ]
    }
}
