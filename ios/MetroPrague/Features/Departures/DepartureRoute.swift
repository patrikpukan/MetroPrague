/// Opens the departures of one station and direction.
nonisolated struct DepartureRoute: Hashable, Sendable {
    let stationID: String
    let line: Line
    let terminusStationID: String
}
