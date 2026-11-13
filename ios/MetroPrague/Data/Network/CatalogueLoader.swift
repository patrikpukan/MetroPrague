import Foundation

/// Loads the metro catalogue: GTFS routes, stops, and trip patterns hydrate the full physical
/// network with current platforms and their directions.
nonisolated enum CatalogueLoader {
    /// A trip page this full may have a next page.
    static let tripPageSize = 1000
    /// More trips than this at one platform means the response is not what we expect.
    static let maximumTripsPerPlatform = 10_000

    static func load(using client: GolemioHTTPClient) async throws -> MetroCatalogue {
        let routes = try decode([GTFSRoute].self, from: await client.get(GolemioEndpoints.routes()))
            .filter(\.isMetro)
        guard Set(routes.compactMap(\.line)) == Set(Line.allCases) else {
            throw GolemioError.invalidData
        }
        let routeIDs = Set(routes.map(\.id))

        var names: [String] = []
        for line in Line.allCases {
            for name in MetroNetwork.namesByLine[line] ?? [] where !names.contains(name) {
                names.append(name)
            }
        }
        let stops = try decode(
            GTFSStopCollection.self, from: await client.get(GolemioEndpoints.stops(names: names))
        ).features

        // One trip per pattern (route, shape, and direction) is enough to learn its stops.
        var patternKeys: Set<String> = []
        var patternTripIDs: [String] = []
        for stopID in MetroNetwork.seedPlatforms {
            var offset = 0
            var pageCount: Int
            repeat {
                let page = try decode(
                    [GTFSTrip].self,
                    from: await client.get(GolemioEndpoints.trips(stopID: stopID, offset: offset)))
                for trip in page where routeIDs.contains(trip.routeID) {
                    guard let shapeID = trip.shapeID?.value,
                        let directionID = trip.directionID?.value,
                        let tripID = trip.tripID?.value
                    else { throw GolemioError.invalidData }
                    let key = "\(trip.routeID):\(shapeID):\(directionID)"
                    if patternKeys.insert(key).inserted { patternTripIDs.append(tripID) }
                }
                pageCount = page.count
                offset += pageCount
                guard offset <= maximumTripsPerPlatform else { throw GolemioError.invalidData }
            } while pageCount == tripPageSize
        }

        var patterns: [GTFSTripDetail] = []
        for tripID in patternTripIDs {
            patterns.append(
                try decode(
                    GTFSTripDetail.self, from: await client.get(GolemioEndpoints.trip(id: tripID))))
        }
        return try parse(routes: routes, stops: stops, patterns: patterns)
    }

    /// Builds the catalogue from decoded GTFS data. Throws `GolemioError.invalidData` when the
    /// metadata is incomplete or contradicts the known network, so a reload never saves bad data.
    static func parse(
        routes: [GTFSRoute],
        stops: [GTFSStop],
        patterns: [GTFSTripDetail]
    ) throws -> MetroCatalogue {
        var routeLines: [String: Line] = [:]
        for route in routes {
            guard let line = route.line else { throw GolemioError.invalidData }
            routeLines[route.id] = line
        }
        try require(Set(routeLines.values) == Set(Line.allCases))

        let metroStops = stops.filter(\.isPlatform)
        let stopNames = Set(metroStops.map(\.name))
        let stations = MetroNetwork.stations
        // Every known station must have current metadata.
        try require(stations.allSatisfy { stopNames.contains($0.name) })
        let stationsByName = Dictionary(uniqueKeysWithValues: stations.map { ($0.name, $0) })

        var routePlatforms: [String: MetroPlatform] = [:]
        var learnedPlatforms: [String: MetroPlatform] = [:]
        for pattern in patterns {
            let line = try required(routeLines[pattern.routeID])
            let names = MetroNetwork.namesByLine[line] ?? []
            let times = pattern.stopTimes.sorted { $0.sequence < $1.sequence }
            try require(times.count >= 2)
            // A stop outside the known line means the network topology needs updating.
            try require(times.allSatisfy { names.contains($0.stopName) })
            let start = try required(names.firstIndex(of: times[0].stopName))
            let end = try required(names.firstIndex(of: times[times.count - 1].stopName))
            try require(start != end)
            let toward = MetroNetwork.stationID(
                for: start < end ? names[names.count - 1] : names[0])

            // Arrival platforms are also proven metro stops, even without an observed departure.
            for time in times {
                let platform = MetroPlatform(
                    id: time.stopID, stationID: MetroNetwork.stationID(for: time.stopName),
                    line: line)
                if let existing = routePlatforms[platform.id] {
                    try require(existing == platform)
                }
                routePlatforms[platform.id] = platform
            }
            // The final stop is an arrival platform; it does not establish a departure direction.
            for time in times.dropLast() {
                let platform = MetroPlatform(
                    id: time.stopID, stationID: MetroNetwork.stationID(for: time.stopName),
                    line: line, towardStationID: toward)
                guard var existing = learnedPlatforms[platform.id] else {
                    learnedPlatforms[platform.id] = platform
                    continue
                }
                try require(existing.stationID == platform.stationID && existing.line == line)
                // A reversing train can leave the same platform in either direction (Skalka).
                if existing.towardStationID != toward { existing.towardStationID = nil }
                learnedPlatforms[platform.id] = existing
            }
        }
        try require(Set(learnedPlatforms.values.map(\.line)) == Set(Line.allCases))

        var platformIDs: Set<String> = []
        var platforms: [MetroPlatform] = []
        for stop in metroStops {
            let stationID = MetroNetwork.stationID(for: stop.name)
            let candidate =
                learnedPlatforms[stop.id] ?? routePlatforms[stop.id]
                ?? MetroPlatformFallbacks.find(stationID: stationID, stopID: stop.id)
            guard let platform = candidate, platform.stationID == stationID,
                stationsByName[stop.name]?.lineOrders[platform.line] != nil,
                platformIDs.insert(platform.id).inserted
            else { continue }
            platforms.append(platform)
        }
        // Every line of every station needs at least one platform to request departures from.
        try require(
            stations.allSatisfy { station in
                station.lineOrders.keys.allSatisfy { line in
                    platforms.contains { $0.stationID == station.id && $0.line == line }
                }
            })
        return MetroCatalogue(stations: stations, platforms: platforms)
    }

    private static func decode<T: Decodable>(_ type: T.Type, from data: Data) throws -> T {
        do {
            return try JSONDecoder().decode(type, from: data)
        } catch {
            throw GolemioError.invalidData
        }
    }

    private static func require(_ condition: Bool) throws {
        guard condition else { throw GolemioError.invalidData }
    }

    private static func required<T>(_ value: T?) throws -> T {
        guard let value else { throw GolemioError.invalidData }
        return value
    }
}

// MARK: - GTFS response types

/// A `/v2/gtfs/routes` item.
nonisolated struct GTFSRoute: Decodable, Sendable {
    let id: String
    let shortName: String?
    let type: LenientInt?

    /// The metro line, for a metro route (type 1) named A, B, or C.
    var line: Line? {
        guard type?.value == 1 else { return nil }
        return shortName.flatMap(Line.init(rawValue:))
    }

    var isMetro: Bool { line != nil }

    private enum CodingKeys: String, CodingKey {
        case id = "route_id"
        case shortName = "route_short_name"
        case type = "route_type"
    }
}

/// The `/v2/gtfs/stops` GeoJSON response.
nonisolated struct GTFSStopCollection: Decodable, Sendable {
    let features: [GTFSStop]
}

/// A GTFS stop (a GeoJSON feature, or the `stop` of a stop time).
nonisolated struct GTFSStop: Decodable, Sendable {
    let id: String
    let name: String
    let locationType: LenientInt?
    let parentStation: String?

    /// A boarding platform (location type 0) that belongs to a parent station.
    var isPlatform: Bool {
        guard locationType?.value == 0, let parentStation else { return false }
        let parent = parentStation.trimmingCharacters(in: .whitespacesAndNewlines)
        return !parent.isEmpty && parent != "null"
    }

    private enum CodingKeys: String, CodingKey {
        case properties
    }

    private enum PropertyKeys: String, CodingKey {
        case id = "stop_id"
        case name = "stop_name"
        case locationType = "location_type"
        case parentStation = "parent_station"
    }

    init(from decoder: any Decoder) throws {
        let properties = try decoder.container(keyedBy: CodingKeys.self)
            .nestedContainer(keyedBy: PropertyKeys.self, forKey: .properties)
        id = try properties.decode(LenientString.self, forKey: .id).value
        name = try properties.decode(String.self, forKey: .name)
        locationType = try properties.decodeIfPresent(LenientInt.self, forKey: .locationType)
        parentStation = try properties.decodeIfPresent(LenientString.self, forKey: .parentStation)?
            .value
    }
}

/// A `/v2/gtfs/trips` item.
nonisolated struct GTFSTrip: Decodable, Sendable {
    let routeID: String
    let tripID: LenientString?
    let shapeID: LenientString?
    let directionID: LenientString?

    private enum CodingKeys: String, CodingKey {
        case routeID = "route_id"
        case tripID = "trip_id"
        case shapeID = "shape_id"
        case directionID = "direction_id"
    }
}

/// A `/v2/gtfs/trips/{id}` response with its stop times.
nonisolated struct GTFSTripDetail: Decodable, Sendable {
    let routeID: String
    let stopTimes: [StopTime]

    struct StopTime: Decodable, Sendable {
        let stopID: String
        let sequence: Int
        let stopName: String

        private enum CodingKeys: String, CodingKey {
            case stopID = "stop_id"
            case sequence = "stop_sequence"
            case stop
        }

        init(from decoder: any Decoder) throws {
            let container = try decoder.container(keyedBy: CodingKeys.self)
            stopID = try container.decode(LenientString.self, forKey: .stopID).value
            guard let sequence = try container.decode(LenientInt.self, forKey: .sequence).value
            else { throw GolemioError.invalidData }
            self.sequence = sequence
            stopName = try container.decode(GTFSStop.self, forKey: .stop).name
        }
    }

    private enum CodingKeys: String, CodingKey {
        case routeID = "route_id"
        case stopTimes = "stop_times"
    }
}
