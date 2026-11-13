import Foundation
import Testing

@testable import MetroPrague

@Suite("DepartureBoard")
struct DepartureBoardTests {
    private let fetchedAt = Date(timeIntervalSince1970: 1_791_288_000)  // 2026-10-06T12:00:00Z

    private func departure(
        _ id: String,
        line: Line = .b,
        headsign: String = "Zličín",
        at seconds: TimeInterval,
        isAtStop: Bool = false,
        toward: String? = nil
    ) -> Departure {
        Departure(
            tripID: id, line: line, headsign: headsign,
            scheduled: fetchedAt.addingTimeInterval(seconds), predicted: nil, delaySeconds: nil,
            isAtStop: isAtStop, isCanceled: false, directionTerminusStationID: toward)
    }

    private func board(_ departures: [Departure]) -> DepartureBoard {
        DepartureBoard(stationID: "andel", departures: departures, fetchedAt: fetchedAt)
    }

    @Test(
        "Staleness by whole-second age",
        arguments: [
            (0.0, false), (299, false), (299.9, false), (300, true), (-1, true), (-0.5, true),
        ])
    func staleness(age: TimeInterval, isStale: Bool) {
        #expect(board([]).evaluated(at: fetchedAt.addingTimeInterval(age)).isStale == isStale)
    }

    @Test("A past departure is removed, a current one kept")
    func pastDepartureRemoved() {
        let evaluated = board([departure("past", at: -1), departure("now", at: 0)])
            .evaluated(at: fetchedAt)
        #expect(evaluated.departures.map(\.tripID) == ["now"])
    }

    @Test("A past at-stop departure is kept while fresh and removed when stale")
    func pastAtStopDeparture() {
        let saved = board([departure("at-stop", at: -1, isAtStop: true)])
        #expect(saved.evaluated(at: fetchedAt.addingTimeInterval(299)).departures.count == 1)
        let stale = saved.evaluated(at: fetchedAt.addingTimeInterval(300))
        #expect(stale.isStale)
        #expect(stale.departures.isEmpty)
    }

    @Test("A board without a fetch time is unchanged")
    func boardWithoutFetchTime() {
        let unavailable = DepartureBoard(
            stationID: "andel", departures: [], isUnavailable: true, isStale: true)
        #expect(unavailable.evaluated(at: fetchedAt) == unavailable)
    }

    @Test("Direction filtering uses the assigned terminus, else the headsign")
    func directionFiltering() {
        let towardZlicin = Direction(line: .b, terminusStationID: "zlicin", terminusName: "Zličín")
        let saved = board([
            departure("assigned", headsign: "Nové Butovice", at: 60, toward: "zlicin"),
            departure("headsign", at: 120),
            departure("other-way", headsign: "Zličín", at: 180, toward: "cerny-most"),
            departure("other-line", line: .a, at: 240),
        ])
        #expect(saved.departures(for: towardZlicin).map(\.tripID) == ["assigned", "headsign"])
    }
}
