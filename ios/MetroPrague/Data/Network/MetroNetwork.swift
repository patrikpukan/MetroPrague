import Foundation

/// The physical network topology, including stations temporarily omitted from GTFS trip patterns.
nonisolated enum MetroNetwork {
    static let namesByLine: [Line: [String]] = [
        .a: [
            "Nemocnice Motol", "Petřiny", "Nádraží Veleslavín", "Bořislavka", "Dejvická",
            "Hradčanská", "Malostranská", "Staroměstská", "Můstek", "Muzeum", "Náměstí Míru",
            "Jiřího z Poděbrad", "Flora", "Želivského", "Strašnická", "Skalka", "Depo Hostivař",
        ],
        .b: [
            "Zličín", "Stodůlky", "Luka", "Lužiny", "Hůrka", "Nové Butovice", "Jinonice",
            "Radlická", "Smíchovské nádraží", "Anděl", "Karlovo náměstí", "Národní třída",
            "Můstek", "Náměstí Republiky", "Florenc", "Křižíkova", "Invalidovna", "Palmovka",
            "Českomoravská", "Vysočanská", "Kolbenova", "Hloubětín", "Rajská zahrada",
            "Černý Most",
        ],
        .c: [
            "Letňany", "Prosek", "Střížkov", "Ládví", "Kobylisy", "Nádraží Holešovice",
            "Vltavská", "Florenc", "Hlavní nádraží", "Muzeum", "I. P. Pavlova", "Vyšehrad",
            "Pražského povstání", "Pankrác", "Budějovická", "Kačerov", "Roztyly", "Chodov",
            "Opatov", "Háje",
        ],
    ]

    /// Every station once, in order of first appearance on lines A, B, then C. Usable before the
    /// first reload, so search and favorites still resolve.
    static let stations: [SavedStation] = {
        var names: [String] = []
        var lineOrders: [String: [Line: Int]] = [:]
        for line in Line.allCases {
            for (order, name) in (namesByLine[line] ?? []).enumerated() {
                if lineOrders[name] == nil { names.append(name) }
                lineOrders[name, default: [:]][line] = order
            }
        }
        return names.map { name in
            SavedStation(id: stationID(for: name), name: name, lineOrders: lineOrders[name] ?? [:])
        }
    }()

    /// Existing central platform IDs that bootstrap GTFS trip patterns, even outside service hours.
    static let seedPlatforms = [
        "U1072Z101P", "U1072Z102P", "U1072Z121P", "U1072Z122P", "U400Z121P", "U400Z122P",
    ]

    /// The station ID for a station name: diacritics removed, lowercased, and runs of characters
    /// other than `a-z` and `0-9` replaced by one `-`, trimmed. `I. P. Pavlova` is `ip-pavlova`.
    static func stationID(for name: String) -> String {
        if name == "I. P. Pavlova" { return "ip-pavlova" }
        let withoutMarks = String(
            String.UnicodeScalarView(
                name.decomposedStringWithCanonicalMapping.unicodeScalars.filter { scalar in
                    switch scalar.properties.generalCategory {
                    case .nonspacingMark, .spacingMark, .enclosingMark: false
                    default: true
                    }
                }
            )
        )
        var id = ""
        var pendingSeparator = false
        for character in withoutMarks.lowercased() {
            if character.isASCII, character.isLetter || character.isNumber {
                if pendingSeparator, !id.isEmpty { id.append("-") }
                pendingSeparator = false
                id.append(character)
            } else {
                pendingSeparator = true
            }
        }
        return id
    }
}
