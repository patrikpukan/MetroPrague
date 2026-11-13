import Foundation

/// One metro departure from a station.
nonisolated struct Departure: Hashable, Sendable {
    let tripID: String
    let line: Line
    let headsign: String
    let scheduled: Date
    let predicted: Date?
    let delaySeconds: Int?
    let isAtStop: Bool
    let isCanceled: Bool
    /// The terminus this departure travels toward, when the parser could assign one.
    var directionTerminusStationID: String? = nil

    /// The predicted time when present, otherwise the scheduled time.
    var effectiveTime: Date { predicted ?? scheduled }
}
