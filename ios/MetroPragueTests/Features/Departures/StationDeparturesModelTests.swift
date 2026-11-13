import Foundation
import Testing

@testable import MetroPrague

@Suite("StationDeparturesModel")
struct StationDeparturesModelTests {
    private let now = Date(timeIntervalSince1970: 1_791_288_000)
    private let utc = TimeZone(identifier: "UTC")!
    private let towardZlicin = DepartureRoute(
        stationID: "andel", line: .b, terminusStationID: "zlicin")

    private func departure(
        _ id: String,
        headsign: String = "Zličín",
        line: Line = .b,
        in seconds: TimeInterval,
        toward: String? = nil
    ) -> Departure {
        Departure(
            tripID: id, line: line, headsign: headsign, scheduled: now.addingTimeInterval(seconds),
            predicted: nil, delaySeconds: nil, isAtStop: false, isCanceled: false,
            directionTerminusStationID: toward)
    }

    private func model(
        _ route: DepartureRoute? = nil,
        board: @escaping @Sendable (String, Date) -> DepartureBoard
    ) -> StationDeparturesModel {
        StationDeparturesModel(
            route: route ?? towardZlicin,
            metroData: MetroDataStore(source: FixedMetroDataSource(boards: board)),
            timeZone: utc)
    }

    private func board(_ departures: [Departure], fetchedAt: Date? = nil)
        -> @Sendable (String, Date) -> DepartureBoard
    {
        let fetchedAt = fetchedAt ?? now
        return { stationID, _ in
            DepartureBoard(
                stationID: stationID,
                departures: departures.sorted { $0.effectiveTime < $1.effectiveTime },
                fetchedAt: fetchedAt)
        }
    }

    @Test("Shows at most five of the direction's departures, in time order")
    func filteringOrderingAndCutoff() {
        let departures =
            (1...7).map { departure("z\($0)", in: Double($0) * 60) } + [
                departure("other-way", headsign: "Černý Most", in: 30),
                departure("other-line", line: .a, in: 45),
                departure("short-turn", headsign: "Nové Butovice", in: 20, toward: "zlicin"),
            ]
        let content = model(board: board(departures)).content(at: now)

        #expect(content.emptyState == nil)
        #expect(content.rows.map(\.id) == ["short-turn", "z1", "z2", "z3", "z4"])
        #expect(
            content.rows.map(\.display) == [
                .now, .inMinutes(1), .inMinutes(2), .inMinutes(3), .inMinutes(4),
            ])
        // The header follows the next departure's headsign.
        #expect(content.terminusName == "Nové Butovice")
        #expect(content.rows.first?.destination == "Nové Butovice")
    }

    @Test("A past departure is dropped")
    func pastDeparture() {
        let content = model(board: board([departure("past", in: -1), departure("next", in: 120)]))
            .content(at: now)
        #expect(content.rows.map(\.id) == ["next"])
    }

    @Test("A stale board shows the stale state")
    func stale() {
        let content = model(
            board: board([departure("z", in: 600)], fetchedAt: now.addingTimeInterval(-300))
        )
        .content(at: now)
        #expect(content.emptyState == .stale)
        #expect(content.rows.isEmpty)
        #expect(content.terminusName == "Zličín")
    }

    @Test("An unavailable board shows the unavailable state")
    func unavailable() {
        let content = model { stationID, _ in
            DepartureBoard(stationID: stationID, departures: [], isUnavailable: true)
        }.content(at: now)
        #expect(content.emptyState == .unavailable)
    }

    @Test("Without saved data, stale wins over unavailable")
    func noSavedData() {
        let content = model { stationID, _ in
            DepartureBoard(stationID: stationID, departures: [], isUnavailable: true, isStale: true)
        }.content(at: now)
        #expect(content.emptyState == .stale)
    }

    @Test("No matching departures is no service")
    func noService() {
        let content = model(board: board([departure("other-way", headsign: "Černý Most", in: 60)]))
            .content(at: now)
        #expect(content.emptyState == .noService)
        #expect(content.terminusName == "Zličín")
    }

    @Test(
        "An unknown station or direction is no service",
        arguments: [
            DepartureRoute(stationID: "unknown", line: .b, terminusStationID: "zlicin"),
            DepartureRoute(stationID: "andel", line: .a, terminusStationID: "depo-hostivar"),
            DepartureRoute(stationID: "zlicin", line: .b, terminusStationID: "zlicin"),
        ])
    func unknownRoute(route: DepartureRoute) {
        let model = model(route, board: board([departure("z", in: 60)]))
        #expect(model.content(at: now).emptyState == .noService)
    }

    @Test("The station name comes from the current stations")
    func stationName() {
        #expect(model(board: board([])).stationName == "Anděl")
    }
}
