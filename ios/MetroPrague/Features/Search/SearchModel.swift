import Foundation
import Observation

/// Station search, the line filter, and the selected station's directions.
@Observable
final class SearchModel {
    var query = ""
    private(set) var selectedLine: Line?
    /// The station whose direction picker is open.
    var selectedStation: Station?

    @ObservationIgnored private let metroData: MetroDataStore
    @ObservationIgnored private let favorites: FavoritesStore
    @ObservationIgnored private let timeZone: TimeZone

    init(metroData: MetroDataStore, favorites: FavoritesStore, timeZone: TimeZone = .current) {
        self.metroData = metroData
        self.favorites = favorites
        self.timeZone = timeZone
    }

    /// Selects `line`, or clears the filter when `line` is already selected or `nil` (All).
    func toggleLine(_ line: Line?) {
        selectedLine = line == selectedLine ? nil : line
    }

    /// Stations whose name contains the query, ignoring case, and that serve the selected line.
    /// An empty query matches every station.
    var filteredStations: [Station] {
        metroData.stations.filter { station in
            let matchesLine = selectedLine.map { line in station.lines.contains { $0.line == line }
            }
            let matchesQuery =
                query.isEmpty || station.name.range(of: query, options: .caseInsensitive) != nil
            return matchesLine != false && matchesQuery
        }
    }

    /// The selected station's directions with their next departure and favorite state.
    func directionRows(at now: Date) -> [DirectionRowContent] {
        guard let station = selectedStation else { return [] }
        let board = metroData.board(for: station.id, at: now)
        return displayedDirections.map { direction in
            let next = board.departures(for: direction).first
            let display: DepartureDisplay =
                if board.isStale {
                    .stale
                } else if board.isUnavailable {
                    .unavailable
                } else {
                    DepartureDisplay.make(for: next, now: now, timeZone: timeZone)
                }
            return DirectionRowContent(
                direction: direction,
                destination: next?.headsign ?? direction.terminusName,
                display: display,
                isFavorite: favorites.contains(favoriteKey(station: station, direction: direction))
            )
        }
    }

    /// Adds or removes the favorite. Only a direction displayed for the selected station counts.
    func toggleFavorite(_ direction: Direction) {
        guard let station = selectedStation, displayedDirections.contains(direction) else { return }
        favorites.toggle(favoriteKey(station: station, direction: direction))
    }

    /// The departures route for `direction` at the selected station.
    func route(for direction: Direction) -> DepartureRoute? {
        guard let station = selectedStation else { return nil }
        return DepartureRoute(
            stationID: station.id, line: direction.line,
            terminusStationID: direction.terminusStationID)
    }

    private var displayedDirections: [Direction] {
        guard let selectedStation,
            let station = metroData.stations.first(where: { $0.id == selectedStation.id })
        else { return [] }
        return StationDirections.directions(at: station, in: metroData.stations)
    }

    private func favoriteKey(station: Station, direction: Direction) -> FavoriteKey {
        FavoriteKey(
            stationID: station.id, line: direction.line,
            terminusStationID: direction.terminusStationID)
    }
}
