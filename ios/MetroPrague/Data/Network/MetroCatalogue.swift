/// The metro stations and platforms saved with a snapshot.
nonisolated struct MetroCatalogue: Codable, Hashable, Sendable {
    let stations: [SavedStation]
    let platforms: [MetroPlatform]

    var domainStations: [Station] { stations.map(\.domainStation) }
}

/// A saved station with its position on each line that serves it.
nonisolated struct SavedStation: Codable, Hashable, Sendable {
    let id: String
    let name: String
    let lineOrders: [Line: Int]

    var domainStation: Station {
        Station(
            id: id,
            name: name,
            lines: Line.allCases.compactMap { line in
                lineOrders[line].map { LinePosition(line: line, order: $0) }
            }
        )
    }
}

/// A GTFS metro platform. `towardStationID` is the terminus departures from it travel toward,
/// or `nil` when trains leave it in both directions (for example, a reversing platform).
nonisolated struct MetroPlatform: Codable, Hashable, Sendable {
    let id: String
    let stationID: String
    let line: Line
    var towardStationID: String? = nil
}
