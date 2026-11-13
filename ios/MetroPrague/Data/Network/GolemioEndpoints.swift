import Foundation

/// Golemio request URLs. Query values are form-encoded like Android's `URLEncoder`, and array
/// parameters are sent as `ids%5B%5D=…` and `names%5B%5D=…`.
nonisolated enum GolemioEndpoints {
    static let base = "https://api.golemio.cz"

    /// The departures of the next 60 minutes from `stopIDs`, up to 30, in real-time order.
    static func departureBoard(stopIDs: [String]) -> URL {
        let ids = stopIDs.map { "ids%5B%5D=\(encode($0))" }.joined(separator: "&")
        return url(
            "/v2/pid/departureboards?\(ids)&minutesAfter=60&limit=30&mode=departures&order=real")
    }

    static func routes() -> URL {
        url("/v2/gtfs/routes?limit=10000")
    }

    static func stops(names: [String]) -> URL {
        let names = names.map { "names%5B%5D=\(encode($0))" }.joined(separator: "&")
        return url("/v2/gtfs/stops?\(names)&limit=10000")
    }

    static func trips(stopID: String, offset: Int) -> URL {
        url("/v2/gtfs/trips?stopId=\(encode(stopID))&limit=1000&offset=\(offset)")
    }

    static func trip(id: String) -> URL {
        url("/v2/gtfs/trips/\(encode(id))?includeStops=true&includeStopTimes=true")
    }

    private static func url(_ pathAndQuery: String) -> URL {
        // Every part is either a constant or percent-encoded, so the string is always valid.
        URL(string: base + pathAndQuery)!
    }

    /// `application/x-www-form-urlencoded` encoding: letters, digits, and `.-*_` stay as they
    /// are, a space becomes `+`, and every other UTF-8 byte becomes `%XX`.
    static func encode(_ value: String) -> String {
        var encoded = ""
        for byte in value.utf8 {
            switch byte {
            case UInt8(ascii: "a")...UInt8(ascii: "z"), UInt8(ascii: "A")...UInt8(ascii: "Z"),
                UInt8(ascii: "0")...UInt8(ascii: "9"), UInt8(ascii: "."), UInt8(ascii: "-"),
                UInt8(ascii: "*"), UInt8(ascii: "_"):
                encoded.append(Character(UnicodeScalar(byte)))
            case UInt8(ascii: " "):
                encoded.append("+")
            default:
                encoded.append(String(format: "%%%02X", byte))
            }
        }
        return encoded
    }
}
