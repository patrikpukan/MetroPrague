import Foundation
import Testing

@testable import MetroPrague

@Suite("DepartureDisplay")
struct DepartureDisplayTests {
    private let now = Date(timeIntervalSince1970: 1_785_924_000)  // 2026-08-05T10:00:00Z
    private let prague = TimeZone(identifier: "Europe/Prague")!

    private func departure(
        secondsFromNow: TimeInterval,
        isAtStop: Bool = false,
        isCanceled: Bool = false
    ) -> Departure {
        Departure(
            tripID: "trip", line: .a, headsign: "Depo Hostivař",
            scheduled: now.addingTimeInterval(secondsFromNow), predicted: nil, delaySeconds: nil,
            isAtStop: isAtStop, isCanceled: isCanceled)
    }

    private func display(_ departure: Departure?) -> DepartureDisplay {
        DepartureDisplay.make(for: departure, now: now, timeZone: prague)
    }

    @Test("No departure is no service")
    func noDeparture() {
        #expect(display(nil) == .noService)
    }

    @Test("Cancelled wins over at-stop, and at-stop wins over the time")
    func precedence() {
        #expect(
            display(departure(secondsFromNow: 300, isAtStop: true, isCanceled: true)) == .cancelled)
        #expect(display(departure(secondsFromNow: 300, isAtStop: true)) == .atStation)
    }

    @Test(
        "Time boundaries",
        arguments: [
            (30.0, DepartureDisplay.now),
            (59, .now),
            (59.9, .now),
            (60, .inMinutes(1)),
            (300, .inMinutes(5)),
            (3599, .inMinutes(59)),
            (3600, .atTime(hour: 13, minute: 0)),
            (5400, .atTime(hour: 13, minute: 30)),
        ])
    func boundaries(seconds: TimeInterval, expected: DepartureDisplay) {
        #expect(display(departure(secondsFromNow: seconds)) == expected)
    }

    @Test("A past departure is now")
    func pastDeparture() {
        #expect(display(departure(secondsFromNow: -90)) == .now)
    }

    @Test("The predicted time is used when present")
    func predictedTime() {
        let delayed = Departure(
            tripID: "trip", line: .a, headsign: "X", scheduled: now,
            predicted: now.addingTimeInterval(180), delaySeconds: 180, isAtStop: false,
            isCanceled: false)
        #expect(display(delayed) == .inMinutes(3))
    }

    @Test("Labels use the shared English copy and a zero-padded 24-hour time")
    func labels() {
        #expect(String(localized: DepartureDisplay.inMinutes(5).label) == "5 min")
        #expect(String(localized: DepartureDisplay.atTime(hour: 7, minute: 5).label) == "07:05")
        #expect(String(localized: DepartureDisplay.atTime(hour: 23, minute: 45).label) == "23:45")
        #expect(String(localized: DepartureDisplay.stale.label) == "Reload departures in Settings")
    }
}
