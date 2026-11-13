import Foundation

/// An integer encoded as a number or a numeric string. Other values decode as `nil`.
nonisolated struct LenientInt: Decodable {
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
nonisolated struct LenientBool: Decodable {
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
nonisolated struct LenientString: Decodable {
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
