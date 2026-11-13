import Foundation

@testable import MetroPrague

/// A small but complete Golemio catalogue for the whole network, ported from Android's
/// `MetroTestData`. Each station has an `up` and a `down` platform per line (`B-9-down` is
/// Anděl toward Zličín), and each line has one pattern per direction. Flora stays in the stop
/// metadata but is missing from the trip patterns, so its platforms come from the fallback list.
nonisolated enum MetroTestData {
    static let emptyBoard = #"{"departures": [], "infotexts": []}"#

    /// One line-B departure from Anděl's `B-9-down` platform toward Zličín.
    static let board = """
        {"departures": [{
            "route": {"type": 1, "short_name": "B"},
            "trip": {"id": "trip-1", "headsign": "Zličín", "is_at_stop": false, "is_canceled": false},
            "stop": {"id": "B-9-down"},
            "departure_timestamp": {"scheduled": "2026-10-14T12:03:00Z", "predicted": "2026-10-14T12:04:00Z"},
            "delay": {"is_available": true, "seconds": 60}
        }], "infotexts": [{"text": "Service information"}]}
        """

    static var routes: [[String: Any]] {
        Line.allCases.map { line in
            ["route_id": "L-\(line.rawValue)", "route_short_name": line.rawValue, "route_type": 1]
        }
    }

    static var stops: [[String: Any]] {
        Line.allCases.flatMap { line in
            names(line).enumerated().flatMap { index, name in
                ["up", "down"].map { direction in
                    stop(
                        id: platformID(line: line, index: index, name: name, direction: direction),
                        name: name)
                }
            }
        }
    }

    static var patterns: [[String: Any]] {
        Line.allCases.flatMap { line in
            [false, true].map { reverse in
                var indexed = names(line).enumerated().filter { $0.element != "Flora" }
                if reverse { indexed.reverse() }
                let direction = reverse ? "down" : "up"
                let stopTimes: [[String: Any]] = indexed.enumerated().map { sequence, item in
                    let id = "\(line.rawValue)-\(item.offset)-\(direction)"
                    return [
                        "stop_id": id, "stop_sequence": sequence + 1,
                        "stop": stop(id: id, name: item.element),
                    ]
                }
                return [
                    "route_id": "L-\(line.rawValue)",
                    "trip_id": "\(line.rawValue)-\(reverse)",
                    "shape_id": "\(line.rawValue)-\(reverse)",
                    "direction_id": reverse ? 1 : 0,
                    "stop_times": stopTimes,
                ]
            }
        }
    }

    static func stop(id: String, name: String) -> [String: Any] {
        [
            "properties": [
                "stop_id": id, "stop_name": name, "location_type": 0,
                "parent_station": "parent-\(MetroNetwork.stationID(for: name))",
                "wheelchair_boarding": 1,
            ]
        ]
    }

    static func json(_ object: Any) -> Data {
        try! JSONSerialization.data(withJSONObject: object)
    }

    static func decode<T: Decodable>(_ type: T.Type, _ object: Any) throws -> T {
        try JSONDecoder().decode(type, from: json(object))
    }

    /// The catalogue parsed from the given (by default, unmodified) test metadata.
    static func catalogue(
        routes: [[String: Any]] = routes,
        stops: [[String: Any]] = stops,
        patterns: [[String: Any]] = patterns
    ) throws -> MetroCatalogue {
        try CatalogueLoader.parse(
            routes: decode([GTFSRoute].self, routes),
            stops: decode(GTFSStopCollection.self, ["features": stops]).features,
            patterns: patterns.map { try decode(GTFSTripDetail.self, $0) })
    }

    /// Answers catalogue requests from the test metadata and board requests through `board`.
    static func transport(
        board: @escaping @Sendable (URLRequest) async throws -> FakeTransport.Reply = { _ in
            .json(emptyBoard)
        }
    ) -> FakeTransport {
        FakeTransport { request in
            let url = request.url!
            let path = url.path()
            let query = URLComponents(url: url, resolvingAgainstBaseURL: false)?.queryItems ?? []
            switch path {
            case "/v2/pid/departureboards":
                return try await board(request)
            case "/v2/gtfs/routes":
                return FakeTransport.Reply(body: json(routes))
            case "/v2/gtfs/stops":
                return FakeTransport.Reply(body: json(["features": stops]))
            case "/v2/gtfs/trips":
                let stopID = query.first { $0.name == "stopId" }?.value
                let offset = query.first { $0.name == "offset" }?.value
                let isFirstPage = stopID == MetroNetwork.seedPlatforms[0] && offset == "0"
                let page = isFirstPage ? patterns.map { $0.filter { $0.key != "stop_times" } } : []
                return FakeTransport.Reply(body: json(page))
            default:
                let tripID = url.lastPathComponent
                guard let pattern = patterns.first(where: { $0["trip_id"] as? String == tripID })
                else {
                    return .status(404)
                }
                return FakeTransport.Reply(body: json(pattern))
            }
        }
    }

    private static func names(_ line: Line) -> [String] {
        MetroNetwork.namesByLine[line] ?? []
    }

    private static func platformID(line: Line, index: Int, name: String, direction: String)
        -> String
    {
        if name == "Flora" { return direction == "up" ? "U118Z101P" : "U118Z102P" }
        return "\(line.rawValue)-\(index)-\(direction)"
    }
}
