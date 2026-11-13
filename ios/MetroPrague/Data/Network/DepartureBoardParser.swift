import Foundation

/// Maps a PID v2 departure-board response to a metro `DepartureBoard`.
///
/// Decoding is lenient where Golemio is: `route.type` may be a number or a numeric string,
/// booleans may be `true`/`false` or the strings `"true"`/`"false"`, and the `delay`, `stop`,
/// `last_stop`, and `infotexts` objects may be missing. Non-metro routes and lines other than
/// A, B, and C are dropped.
nonisolated enum DepartureBoardParser {
    /// Throws when the response is not a valid board.
    ///
    /// With a `catalogue`, a departure's direction comes from its headsign's position on the line,
    /// then its platform, then its previous stop. Without one, it comes from `MetroStopIDs`.
    static func parse(
        stationID: String,
        response: String,
        catalogue: MetroCatalogue? = nil
    ) throws -> DepartureBoard {
        let decoded = try JSONDecoder().decode(Response.self, from: Data(response.utf8))
        let departures = decoded.departures
            .compactMap { item in
                item.metro.map { departure(from: $0, stationID: stationID, catalogue: catalogue) }
            }
            .sorted { $0.effectiveTime < $1.effectiveTime }
        let infoTexts = (decoded.infotexts ?? []).compactMap { info in
            info.text.flatMap {
                $0.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty ? nil : $0
            }
        }
        return DepartureBoard(stationID: stationID, departures: departures, infoTexts: infoTexts)
    }

    private static func departure(
        from item: MetroItem,
        stationID: String,
        catalogue: MetroCatalogue?
    ) -> Departure {
        let towardStationID: String?
        if let catalogue {
            towardStationID = catalogue.direction(
                stationID: stationID,
                line: item.line,
                stopID: item.stopID,
                headsign: item.headsign,
                lastStopID: item.lastStopID
            )
        } else {
            towardStationID = item.stopID.flatMap {
                MetroStopIDs.towardStationID(stationID: stationID, line: item.line, stopID: $0)
            }
        }
        return Departure(
            tripID: item.tripID,
            line: item.line,
            headsign: item.headsign,
            scheduled: item.scheduled,
            predicted: item.predicted,
            delaySeconds: item.delaySeconds,
            isAtStop: item.isAtStop,
            isCanceled: item.isCanceled,
            directionTerminusStationID: towardStationID
        )
    }
}

extension MetroCatalogue {
    /// The terminus a departure from `stationID` travels toward, or `nil` when unknown.
    fileprivate nonisolated func direction(
        stationID: String,
        line: Line,
        stopID: String?,
        headsign: String,
        lastStopID: String?
    ) -> String? {
        let ordered =
            stations
            .compactMap { station in station.lineOrders[line].map { (station, $0) } }
            .sorted { $0.1 < $1.1 }
        guard let current = ordered.first(where: { $0.0.id == stationID }),
            let first = ordered.first?.0, let last = ordered.last?.0
        else { return nil }
        let currentOrder = current.1

        if let target = ordered.first(where: { $0.0.name == headsign }), target.0.id != stationID {
            return target.1 > currentOrder ? last.id : first.id
        }
        if let platform = platforms.first(where: {
            $0.id == stopID && $0.stationID == stationID && $0.line == line
        }), let towardStationID = platform.towardStationID {
            return towardStationID
        }
        let previousStationID = platforms.first { $0.id == lastStopID }?.stationID
        guard let previous = ordered.first(where: { $0.0.id == previousStationID }),
            previous.0.id != stationID
        else { return nil }
        return previous.1 < currentOrder ? last.id : first.id
    }
}

// MARK: - Response types

nonisolated private struct Response: Decodable {
    let departures: [Item]
    let infotexts: [InfoText]?
}

nonisolated private struct InfoText: Decodable {
    let text: String?
}

/// One board item. Only metro items are decoded past their route, so other transport types
/// cannot fail the board.
nonisolated private struct Item: Decodable {
    let metro: MetroItem?

    private enum CodingKeys: String, CodingKey {
        case route
    }

    private struct Route: Decodable {
        let type: LenientInt?
        let shortName: String?

        private enum CodingKeys: String, CodingKey {
            case type
            case shortName = "short_name"
        }
    }

    init(from decoder: any Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)
        let route = try container.decode(Route.self, forKey: .route)
        guard route.type?.value == 1, let shortName = route.shortName,
            let line = Line(rawValue: shortName)
        else {
            metro = nil
            return
        }
        metro = try MetroItem(from: decoder, line: line)
    }
}

nonisolated private struct MetroItem {
    let line: Line
    let tripID: String
    let headsign: String
    let scheduled: Date
    let predicted: Date?
    let delaySeconds: Int?
    let isAtStop: Bool
    let isCanceled: Bool
    let stopID: String?
    let lastStopID: String?

    private enum CodingKeys: String, CodingKey {
        case trip
        case stop
        case lastStop = "last_stop"
        case departureTimestamp = "departure_timestamp"
        case delay
    }

    private struct Trip: Decodable {
        let id: LenientString
        let headsign: String
        let isAtStop: LenientBool
        let isCanceled: LenientBool

        private enum CodingKeys: String, CodingKey {
            case id
            case headsign
            case isAtStop = "is_at_stop"
            case isCanceled = "is_canceled"
        }
    }

    private struct Stop: Decodable {
        let id: String?
    }

    private struct Timestamp: Decodable {
        let scheduled: String
        let predicted: String?
    }

    private struct Delay: Decodable {
        let isAvailable: LenientBool?
        let seconds: LenientInt?

        private enum CodingKeys: String, CodingKey {
            case isAvailable = "is_available"
            case seconds
        }
    }

    init(from decoder: any Decoder, line: Line) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)
        let trip = try container.decode(Trip.self, forKey: .trip)
        let timestamp = try container.decode(Timestamp.self, forKey: .departureTimestamp)
        let delay = try container.decodeIfPresent(Delay.self, forKey: .delay)

        self.line = line
        tripID = trip.id.value
        headsign = trip.headsign
        scheduled = try ISO8601.date(from: timestamp.scheduled)
        predicted = try timestamp.predicted.map(ISO8601.date(from:))
        delaySeconds = delay?.isAvailable?.value == true ? delay?.seconds?.value : nil
        isAtStop = trip.isAtStop.value
        isCanceled = trip.isCanceled.value
        stopID = try container.decodeIfPresent(Stop.self, forKey: .stop)?.id
        lastStopID = try container.decodeIfPresent(Stop.self, forKey: .lastStop)?.id
    }
}

// MARK: - Lenient values

/// An integer encoded as a number or a numeric string. Other values decode as `nil`.
nonisolated private struct LenientInt: Decodable {
    let value: Int?

    init(from decoder: any Decoder) throws {
        let container = try decoder.singleValueContainer()
        if let number = try? container.decode(Int.self) {
            value = number
        } else if let string = try? container.decode(String.self) {
            value = Int(string)
        } else {
            value = nil
        }
    }
}

/// A boolean encoded as `true`/`false` or as the strings `"true"`/`"false"`.
nonisolated private struct LenientBool: Decodable {
    let value: Bool

    init(from decoder: any Decoder) throws {
        let container = try decoder.singleValueContainer()
        if let bool = try? container.decode(Bool.self) {
            value = bool
        } else {
            value = try container.decode(String.self) == "true"
        }
    }
}

/// A string, or a number written as a string.
nonisolated private struct LenientString: Decodable {
    let value: String

    init(from decoder: any Decoder) throws {
        let container = try decoder.singleValueContainer()
        if let string = try? container.decode(String.self) {
            value = string
        } else {
            value = String(try container.decode(Int.self))
        }
    }
}

/// Parses ISO 8601 timestamps with `Z` or an offset, with or without fractional seconds.
nonisolated enum ISO8601 {
    struct InvalidDate: Error {}

    static func date(from string: String) throws -> Date {
        if let date = try? Date(string, strategy: .iso8601) { return date }
        if let date = try? Date(
            string, strategy: Date.ISO8601FormatStyle(includingFractionalSeconds: true))
        {
            return date
        }
        throw InvalidDate()
    }
}
