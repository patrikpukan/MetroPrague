/// A travel direction at a station: a line toward one of its termini.
nonisolated struct Direction: Hashable, Sendable {
    let line: Line
    let terminusStationID: String
    let terminusName: String
}
