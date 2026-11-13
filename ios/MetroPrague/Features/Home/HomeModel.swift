import Foundation
import Observation

/// One favorite as Home shows it.
nonisolated struct FavoriteRowContent: Identifiable, Equatable, Sendable {
    let key: FavoriteKey
    let stationName: String
    let line: Line
    /// The next departure's headsign, otherwise the terminus name.
    let destination: String
    let display: DepartureDisplay

    var id: FavoriteKey { key }
}

/// The favorites that still resolve to a station and direction, in stored order.
@Observable
final class HomeModel {
    @ObservationIgnored private let metroData: MetroDataStore
    @ObservationIgnored private let favorites: FavoritesStore
    @ObservationIgnored private let timeZone: TimeZone

    init(metroData: MetroDataStore, favorites: FavoritesStore, timeZone: TimeZone = .current) {
        self.metroData = metroData
        self.favorites = favorites
        self.timeZone = timeZone
    }

    /// Favorites whose station or direction no longer exists are omitted.
    func rows(at now: Date) -> [FavoriteRowContent] {
        let stations = metroData.stations
        return favorites.favorites.compactMap { key in
            guard let station = stations.first(where: { $0.id == key.stationID }),
                let direction = StationDirections.directions(at: station, in: stations).first(
                    where: {
                        $0.line == key.line && $0.terminusStationID == key.terminusStationID
                    })
            else { return nil }

            let board = metroData.board(for: station.id, at: now)
            let next = board.departures(for: direction).first
            let display: DepartureDisplay =
                if board.isStale {
                    .stale
                } else if board.isUnavailable {
                    .unavailable
                } else {
                    DepartureDisplay.make(for: next, now: now, timeZone: timeZone)
                }
            return FavoriteRowContent(
                key: key, stationName: station.name, line: direction.line,
                destination: next?.headsign ?? direction.terminusName, display: display)
        }
    }

    /// Removes the favorite at once, without confirmation.
    func remove(_ key: FavoriteKey) {
        favorites.remove(key)
    }
}
