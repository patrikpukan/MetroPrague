import Foundation

/// The saved departures of one station, sorted by effective time.
nonisolated struct DepartureBoard: Hashable, Sendable {
    let stationID: String
    var departures: [Departure]
    var infoTexts: [String] = []
    var isUnavailable = false
    var isStale = false
    var fetchedAt: Date? = nil

    /// The oldest whole-second age at which a board is still fresh.
    static let maximumFreshAge = 299

    /// Departures of `direction`'s line toward its terminus. A departure without an assigned
    /// direction matches when its headsign is the terminus name.
    func departures(for direction: Direction) -> [Departure] {
        departures.filter { departure in
            guard departure.line == direction.line else { return false }
            if let terminusID = departure.directionTerminusStationID {
                return terminusID == direction.terminusStationID
            }
            return departure.headsign == direction.terminusName
        }
    }

    /// Applies the time-based rules at `now`.
    ///
    /// A board is fresh only when its whole-second age is 0 through 299. Past departures are
    /// removed, except at-stop departures while the board is fresh. A board without `fetchedAt`
    /// (unavailable or generated) is returned unchanged.
    func evaluated(at now: Date) -> DepartureBoard {
        guard let fetchedAt else { return self }
        let age = Int(now.timeIntervalSince(fetchedAt).rounded(.down))
        let isStale = !(0...Self.maximumFreshAge).contains(age)
        var board = self
        board.isStale = isStale
        board.departures = departures.filter { departure in
            departure.effectiveTime >= now || (departure.isAtStop && !isStale)
        }
        return board
    }
}
