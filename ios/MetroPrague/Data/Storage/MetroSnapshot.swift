import Foundation

/// One complete, saved metro data reload. Written as a whole or not at all.
nonisolated struct MetroSnapshot: Codable, Hashable, Sendable {
    static let currentVersion = 1

    var version = currentVersion
    let catalogue: MetroCatalogue
    /// Raw board responses by station ID. They are re-parsed on load, so parser fixes apply to
    /// saved data.
    let boards: [String: SavedBoard]
    let reloadedAt: Date
    let departureCount: Int
}

/// A saved departure-board response and when it was fetched.
nonisolated struct SavedBoard: Codable, Hashable, Sendable {
    let response: String
    let fetchedAt: Date
}

/// A snapshot with its boards parsed, ready for screens to read.
nonisolated struct LoadedSnapshot: Sendable {
    let snapshot: MetroSnapshot
    /// Parsed boards by station ID, with `fetchedAt` set. Unparsable boards are absent.
    let boards: [String: DepartureBoard]

    /// Parses every saved board. Call it off the main actor.
    init(snapshot: MetroSnapshot) {
        self.snapshot = snapshot
        var boards: [String: DepartureBoard] = [:]
        for (stationID, saved) in snapshot.boards {
            guard
                var board = try? DepartureBoardParser.parse(
                    stationID: stationID, response: saved.response, catalogue: snapshot.catalogue)
            else { continue }
            board.fetchedAt = saved.fetchedAt
            boards[stationID] = board
        }
        self.boards = boards
    }
}
