import Foundation

/// Generated departures for builds without a Golemio key. It cannot reload and saves nothing.
nonisolated struct MockMetroDataSource: MetroDataSource {
    var timeZone: TimeZone = .current

    var canReload: Bool { false }

    func loadSnapshot() async -> LoadedSnapshot? { nil }

    func reload(
        progress: @escaping @MainActor @Sendable (MetroReloadPhase) -> Void
    ) async throws -> LoadedSnapshot {
        throw GolemioError.invalidData
    }

    func board(for stationID: String, in data: LoadedSnapshot?, at now: Date) -> DepartureBoard {
        let stations = MetroNetwork.stations.map(\.domainStation)
        guard let station = stations.first(where: { $0.id == stationID }) else {
            return DepartureBoard(stationID: stationID, departures: [])
        }
        return MockDepartures.board(
            at: now, station: station, stations: stations, timeZone: timeZone)
    }
}

/// The mock timetable: no service 00:30–04:30, a train every 2 minutes 07–09 and 15–19, every
/// 5 minutes 09–15 and 19–20, and every 8 minutes otherwise. Each direction shows its next five
/// trains; every fifth trip runs 60 seconds late, and a train within 30 seconds is at the stop.
nonisolated enum MockDepartures {
    static let departuresPerDirection = 5
    static let delayTripInterval = 5
    static let delaySeconds = 60
    static let atStopThresholdSeconds = 30

    private static let noService = (30 * 60)..<(4 * 3600 + 30 * 60)
    private static let peakHeadway = 2 * 60
    private static let offPeakHeadway = 5 * 60
    private static let nightHeadway = 8 * 60

    static func board(
        at now: Date,
        station: Station,
        stations: [Station],
        timeZone: TimeZone
    ) -> DepartureBoard {
        var calendar = Calendar(identifier: .gregorian)
        calendar.timeZone = timeZone
        let local = calendar.dateComponents(
            [.year, .month, .day, .hour, .minute, .second], from: now)
        let secondsSinceMidnight =
            (local.hour ?? 0) * 3600 + (local.minute ?? 0) * 60 + (local.second ?? 0)
        guard let headway = headway(at: secondsSinceMidnight) else {
            return DepartureBoard(stationID: station.id, departures: [])
        }

        let departures = StationDirections.directions(at: station, in: stations)
            .flatMap { direction in
                (1...departuresPerDirection).compactMap { index in
                    departure(
                        index: index, station: station, direction: direction, headway: headway,
                        secondsSinceMidnight: secondsSinceMidnight, day: local, now: now,
                        calendar: calendar)
                }
            }
            .sorted { $0.effectiveTime < $1.effectiveTime }
        return DepartureBoard(stationID: station.id, departures: departures)
    }

    private static func departure(
        index: Int,
        station: Station,
        direction: Direction,
        headway: Int,
        secondsSinceMidnight: Int,
        day: DateComponents,
        now: Date,
        calendar: Calendar
    ) -> Departure? {
        // A stable per-direction offset, so opposite directions do not depart together.
        let offset = Int(javaHashCode(station.id + direction.terminusStationID).magnitude) % headway
        var secondsToFirst = (offset - secondsSinceMidnight) % headway
        if secondsToFirst < 0 { secondsToFirst += headway }
        if secondsToFirst == 0 { secondsToFirst = headway }
        let scheduledSeconds = secondsSinceMidnight + secondsToFirst + (index - 1) * headway
        if noService.contains(scheduledSeconds % 86_400) { return nil }

        var components = DateComponents()
        components.year = day.year
        components.month = day.month
        components.day = (day.day ?? 1) + scheduledSeconds / 86_400
        components.hour = (scheduledSeconds % 86_400) / 3600
        components.minute = (scheduledSeconds % 3600) / 60
        components.second = scheduledSeconds % 60
        guard let scheduled = calendar.date(from: components) else { return nil }

        let tripNumber = scheduledSeconds / headway + 1
        let delay = tripNumber % delayTripInterval == 0 ? delaySeconds : 0
        let predicted = scheduled.addingTimeInterval(TimeInterval(delay))
        let secondsUntilDeparture = Int(predicted.timeIntervalSince(now).rounded(.down))
        return Departure(
            tripID: "\(station.id)-\(direction.terminusStationID)-\(index)",
            line: direction.line,
            headsign: direction.terminusName,
            scheduled: scheduled,
            predicted: predicted,
            delaySeconds: delay,
            isAtStop: (0...atStopThresholdSeconds).contains(secondsUntilDeparture),
            isCanceled: false
        )
    }

    private static func headway(at secondsSinceMidnight: Int) -> Int? {
        switch secondsSinceMidnight {
        case noService: nil
        case (7 * 3600)..<(9 * 3600), (15 * 3600)..<(19 * 3600): peakHeadway
        case (9 * 3600)..<(15 * 3600), (19 * 3600)..<(20 * 3600): offPeakHeadway
        default: nightHeadway
        }
    }

    /// Java's `String.hashCode`, so the offsets match Android's mock.
    private static func javaHashCode(_ string: String) -> Int32 {
        string.utf16.reduce(Int32(0)) { hash, unit in 31 &* hash &+ Int32(unit) }
    }
}
