import Foundation

/// What a screen shows in place of a departure time.
nonisolated enum DepartureDisplay: Hashable, Sendable {
    case noService
    case unavailable
    case stale
    case cancelled
    case atStation
    case now
    case inMinutes(Int)
    case atTime(hour: Int, minute: Int)

    /// The display for `departure` at `now`.
    ///
    /// Cancelled wins over at-stop, and at-stop wins over the time. Less than 60 seconds away is
    /// "Now", less than 60 minutes shows whole minutes rounded down, and later departures show
    /// the local hour and minute in `timeZone`.
    static func make(for departure: Departure?, now: Date, timeZone: TimeZone) -> DepartureDisplay {
        guard let departure else { return .noService }
        if departure.isCanceled { return .cancelled }
        if departure.isAtStop { return .atStation }

        let secondsUntilDeparture = Int(
            departure.effectiveTime.timeIntervalSince(now).rounded(.down))
        if secondsUntilDeparture < 60 { return .now }
        if secondsUntilDeparture < 60 * 60 { return .inMinutes(secondsUntilDeparture / 60) }

        var calendar = Calendar(identifier: .gregorian)
        calendar.timeZone = timeZone
        let components = calendar.dateComponents([.hour, .minute], from: departure.effectiveTime)
        return .atTime(hour: components.hour ?? 0, minute: components.minute ?? 0)
    }
}
