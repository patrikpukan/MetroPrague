import Foundation
import Testing

@testable import MetroPrague

@Suite("DepartureBoardParser")
struct DepartureBoardParserTests {
    private func fixture(_ name: String) throws -> String {
        String(decoding: try Fixtures.data("golemio/departureboards/\(name)"), as: UTF8.self)
    }

    private func date(_ string: String) throws -> Date {
        try ISO8601.date(from: string)
    }

    // MARK: Shared fixtures (fixtures/README.md)

    @Test("metro-predicted-delay.json: predicted time, delay, and info text")
    func predictedDelayFixture() throws {
        let board = try DepartureBoardParser.parse(
            stationID: "muzeum", response: try fixture("metro-predicted-delay.json"))

        #expect(board.stationID == "muzeum")
        #expect(board.infoTexts == ["Výluka"])
        #expect(board.departures.count == 1)
        let departure = try #require(board.departures.first)
        #expect(departure.line == .a)
        #expect(departure.headsign == "Depo Hostivař")
        #expect(departure.scheduled == (try date("2026-09-26T10:00:00Z")))
        #expect(departure.predicted == (try date("2026-09-26T10:01:30Z")))
        #expect(departure.delaySeconds == 90)
        #expect(!departure.isCanceled)
    }

    @Test("mixed-transport-scheduled-only.json: non-metro dropped, scheduled-only kept")
    func mixedTransportFixture() throws {
        let board = try DepartureBoardParser.parse(
            stationID: "andel", response: try fixture("mixed-transport-scheduled-only.json"))

        #expect(board.departures.count == 1)
        let departure = try #require(board.departures.first)
        #expect(departure.tripID == "metro")
        #expect(departure.predicted == nil)
        #expect(departure.delaySeconds == nil)
        #expect(departure.isAtStop)
        #expect(departure.scheduled == (try date("2026-09-26T10:03:00Z")))
    }

    @Test("empty-board.json: a valid empty board")
    func emptyBoardFixture() throws {
        let board = try DepartureBoardParser.parse(
            stationID: "haje", response: try fixture("empty-board.json"))

        #expect(board.stationID == "haje")
        #expect(board.departures.isEmpty)
        #expect(board.infoTexts.isEmpty)
    }

    @Test("short-turn.json: direction assigned by platform instead of headsign")
    func shortTurnFixture() throws {
        let board = try DepartureBoardParser.parse(
            stationID: "muzeum", response: try fixture("short-turn.json"))

        let towardHaje = Direction(line: .c, terminusStationID: "haje", terminusName: "Háje")
        #expect(board.departures(for: towardHaje).map(\.tripID) == ["short-turn"])
        #expect(board.departures.first?.scheduled == (try date("2026-09-28T10:00:00Z")))
    }

    // MARK: Lenient decoding

    @Test("Lenient route type, booleans, IDs, and fractional-second timestamps")
    func lenientValues() throws {
        let response = """
            {"departures": [{
                "route": {"type": "1", "short_name": "B"},
                "trip": {"id": 42, "headsign": "Zličín", "is_at_stop": "true", "is_canceled": "false"},
                "departure_timestamp": {"scheduled": "2026-09-26T10:00:00.500+02:00"},
                "delay": {"is_available": "true", "seconds": "30"}
            }]}
            """
        let departure = try #require(
            try DepartureBoardParser.parse(stationID: "andel", response: response).departures.first)
        #expect(departure.tripID == "42")
        #expect(departure.isAtStop)
        #expect(!departure.isCanceled)
        #expect(departure.delaySeconds == 30)
        #expect(departure.scheduled == (try date("2026-09-26T08:00:00Z")).addingTimeInterval(0.5))
    }

    @Test("Other lines are dropped, departures sorted, blank info texts skipped")
    func filteringAndSorting() throws {
        let response = """
            {"departures": [
                \(item(id: "late", line: "A", headsign: "Depo Hostivař", scheduled: "10:05:00", predicted: "10:06:00")),
                \(item(id: "early", line: "A", headsign: "Depo Hostivař", scheduled: "10:04:00")),
                \(item(id: "tram-line-d", line: "D", headsign: "X", scheduled: "10:00:00"))
            ], "infotexts": [{"text": "  "}, {"text_en": "Only English"}, {"text": "Výluka"}]}
            """
        let board = try DepartureBoardParser.parse(stationID: "muzeum", response: response)
        #expect(board.departures.map(\.tripID) == ["early", "late"])
        #expect(board.infoTexts == ["Výluka"])
    }

    @Test("A response without departures throws")
    func invalidResponse() {
        #expect(throws: (any Error).self) {
            try DepartureBoardParser.parse(stationID: "andel", response: "not json")
        }
        #expect(throws: (any Error).self) {
            try DepartureBoardParser.parse(stationID: "andel", response: #"{"infotexts": []}"#)
        }
    }

    // MARK: Direction with a catalogue

    private func catalogue(platforms: [MetroPlatform] = []) -> MetroCatalogue {
        MetroCatalogue(stations: MetroNetwork.stations, platforms: platforms)
    }

    private func toward(
        stationID: String,
        line: String,
        headsign: String,
        stop: String? = nil,
        lastStop: String? = nil,
        catalogue: MetroCatalogue
    ) throws -> String? {
        let response = """
            {"departures": [\(item(id: "t", line: line, headsign: headsign, scheduled: "10:00:00", stop: stop, lastStop: lastStop))]}
            """
        let board = try DepartureBoardParser.parse(
            stationID: stationID, response: response, catalogue: catalogue)
        return try #require(board.departures.first).directionTerminusStationID
    }

    @Test("Catalogue direction by headsign position on the line")
    func directionByHeadsign() throws {
        #expect(
            try toward(stationID: "andel", line: "B", headsign: "Zličín", catalogue: catalogue())
                == "zlicin")
        #expect(
            try toward(stationID: "andel", line: "B", headsign: "Florenc", catalogue: catalogue())
                == "cerny-most")
    }

    @Test("Catalogue direction by platform when the headsign is not a station on the line")
    func directionByPlatform() throws {
        let platform = MetroPlatform(
            id: "P1", stationID: "andel", line: .b, towardStationID: "cerny-most")
        #expect(
            try toward(
                stationID: "andel", line: "B", headsign: "Depo", stop: "P1",
                catalogue: catalogue(platforms: [platform])) == "cerny-most")
    }

    @Test("Catalogue direction by previous stop")
    func directionByPreviousStop() throws {
        let platforms = [
            MetroPlatform(id: "P1", stationID: "andel", line: .b),
            MetroPlatform(id: "PREV", stationID: "smichovske-nadrazi", line: .b),
        ]
        #expect(
            try toward(
                stationID: "andel", line: "B", headsign: "Depo", stop: "P1", lastStop: "PREV",
                catalogue: catalogue(platforms: platforms)) == "cerny-most")
        #expect(
            try toward(
                stationID: "andel", line: "B", headsign: "Depo", stop: "P1",
                catalogue: catalogue(platforms: platforms)) == nil)
    }

    @Test("Skalka's shared platform resolves by headsign")
    func skalkaSharedPlatform() throws {
        let shared = catalogue(platforms: [
            MetroPlatform(id: "U953Z101P", stationID: "skalka", line: .a)
        ])
        #expect(
            try toward(
                stationID: "skalka", line: "A", headsign: "Depo Hostivař", stop: "U953Z101P",
                catalogue: shared) == "depo-hostivar")
        #expect(
            try toward(
                stationID: "skalka", line: "A", headsign: "Nemocnice Motol", stop: "U953Z101P",
                catalogue: shared) == "nemocnice-motol")
    }

    private func item(
        id: String,
        line: String,
        headsign: String,
        scheduled: String,
        predicted: String? = nil,
        stop: String? = nil,
        lastStop: String? = nil
    ) -> String {
        var fields = [
            #""route": {"type": 1, "short_name": "\#(line)"}"#,
            #""trip": {"id": "\#(id)", "headsign": "\#(headsign)", "is_at_stop": false, "is_canceled": false}"#,
            #""departure_timestamp": {"scheduled": "2026-09-26T\#(scheduled)Z", "predicted": \#(predicted.map { "\"2026-09-26T\($0)Z\"" } ?? "null")}"#,
        ]
        if let stop { fields.append(#""stop": {"id": "\#(stop)"}"#) }
        if let lastStop { fields.append(#""last_stop": {"id": "\#(lastStop)"}"#) }
        return "{" + fields.joined(separator: ", ") + "}"
    }
}
