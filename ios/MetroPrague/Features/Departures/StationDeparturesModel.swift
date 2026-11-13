import Foundation
import Observation

/// What the departures screen shows at one moment.
nonisolated struct StationDeparturesContent: Equatable, Sendable {
    struct Row: Identifiable, Equatable, Sendable {
        let id: String
        let destination: String
        let display: DepartureDisplay
    }

    /// The next departure's headsign, otherwise the terminus name.
    var terminusName: String
    /// At most five departures in effective-time order; empty when `emptyState` is set.
    var rows: [Row] = []
    /// Stale, unavailable, or no service.
    var emptyState: DepartureDisplay?
}

/// The departures of one station toward one terminus.
@Observable
final class StationDeparturesModel {
    static let maximumRows = 5

    let route: DepartureRoute

    @ObservationIgnored private let metroData: MetroDataStore
    @ObservationIgnored private let timeZone: TimeZone

    init(route: DepartureRoute, metroData: MetroDataStore, timeZone: TimeZone = .current) {
        self.route = route
        self.metroData = metroData
        self.timeZone = timeZone
    }

    var station: Station? {
        metroData.stations.first { $0.id == route.stationID }
    }

    var stationName: String { station?.name ?? "" }

    /// The route's direction, if the station still has it.
    var direction: Direction? {
        guard let station else { return nil }
        return StationDirections.directions(at: station, in: metroData.stations).first {
            $0.line == route.line && $0.terminusStationID == route.terminusStationID
        }
    }

    func content(at now: Date) -> StationDeparturesContent {
        guard let direction else {
            // The station or direction no longer exists.
            return StationDeparturesContent(terminusName: "", emptyState: .noService)
        }
        let board = metroData.board(for: route.stationID, at: now)
        let departures = Array(board.departures(for: direction).prefix(Self.maximumRows))
        var content = StationDeparturesContent(
            terminusName: departures.first?.headsign ?? direction.terminusName)

        if board.isStale {
            content.emptyState = .stale
        } else if board.isUnavailable {
            content.emptyState = .unavailable
        } else if departures.isEmpty {
            content.emptyState = .noService
        } else {
            content.rows = departures.map { departure in
                StationDeparturesContent.Row(
                    id: departure.tripID,
                    destination: departure.headsign,
                    display: DepartureDisplay.make(for: departure, now: now, timeZone: timeZone)
                )
            }
        }
        return content
    }
}
