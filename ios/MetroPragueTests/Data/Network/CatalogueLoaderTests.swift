import Foundation
import Testing

@testable import MetroPrague

@Suite("CatalogueLoader")
struct CatalogueLoaderTests {
    @Test("Loads the complete network, keeping stations missing from trip patterns")
    func loadsCompleteCatalogue() async throws {
        let transport = MetroTestData.transport()
        let client = GolemioHTTPClient(transport: transport, clock: TestClock())
        let catalogue = try await CatalogueLoader.load(using: client)
        let stations = catalogue.domainStations

        #expect(stations.count == 58)
        #expect(stations.filter { $0.lines.contains { $0.line == .a } }.count == 17)
        #expect(stations.filter { $0.lines.contains { $0.line == .b } }.count == 24)
        #expect(stations.filter { $0.lines.contains { $0.line == .c } }.count == 20)
        let mustek = try #require(stations.first { $0.id == "mustek" })
        #expect(StationDirections.directions(at: mustek, in: stations).count == 4)
        #expect(stations.contains { $0.id == "ip-pavlova" })
        // Flora is missing from the patterns; its platforms come from the fallback list.
        #expect(
            Set(catalogue.platforms.filter { $0.stationID == "flora" }.map(\.id)) == [
                "U118Z101P", "U118Z102P",
            ])
        // One detail request per pattern (two directions on three lines).
        #expect(
            transport.requests.filter { $0.url!.path().hasPrefix("/v2/gtfs/trips/") }.count == 6)
    }

    @Test("Learned platforms carry their direction; a line's last stop is arrival-only")
    func platformDirections() throws {
        let catalogue = try MetroTestData.catalogue()
        let platform = { (id: String) in catalogue.platforms.first { $0.id == id } }

        #expect(platform("B-9-down")?.towardStationID == "zlicin")
        #expect(platform("B-9-up")?.towardStationID == "cerny-most")
        #expect(platform("B-9-up")?.stationID == "andel")
        // Černý Most's up platform is only ever arrived at.
        #expect(platform("B-23-up")?.towardStationID == nil)
        #expect(platform("B-23-down")?.towardStationID == "zlicin")
    }

    @Test("Missing station metadata throws")
    func missingStation() {
        let stops = MetroTestData.stops.filter {
            ($0["properties"] as? [String: Any])?["stop_name"] as? String != "Háje"
        }
        #expect(throws: GolemioError.invalidData) { try MetroTestData.catalogue(stops: stops) }
    }

    @Test("A platform seen on two stations or lines throws")
    func conflictingPlatform() {
        var patterns = MetroTestData.patterns
        var times = patterns[2]["stop_times"] as! [[String: Any]]
        // Line B's first stop reuses line A's Petřiny platform ID.
        times[0]["stop_id"] = "A-1-up"
        patterns[2]["stop_times"] = times
        #expect(throws: GolemioError.invalidData) {
            try MetroTestData.catalogue(patterns: patterns)
        }
    }

    @Test("A stop outside the known line throws")
    func unknownTopology() {
        var patterns = MetroTestData.patterns
        var times = patterns[0]["stop_times"] as! [[String: Any]]
        times[1]["stop"] = MetroTestData.stop(id: "X", name: "Nová stanice")
        patterns[0]["stop_times"] = times
        #expect(throws: GolemioError.invalidData) {
            try MetroTestData.catalogue(patterns: patterns)
        }
    }

    @Test("A same-named surface stop is not a metro platform")
    func surfaceStop() throws {
        var surface = MetroTestData.stop(id: "U118Z3P", name: "Flora")
        var properties = surface["properties"] as! [String: Any]
        properties["stop_id"] = "U118Z3P"
        surface["properties"] = properties
        let catalogue = try MetroTestData.catalogue(stops: MetroTestData.stops + [surface])

        #expect(!catalogue.platforms.contains { $0.id == "U118Z3P" })
    }

    @Test("A stop without a parent station is not a platform")
    func stopWithoutParent() throws {
        let stops = MetroTestData.stops.map { stop -> [String: Any] in
            var properties = stop["properties"] as! [String: Any]
            if properties["stop_id"] as? String == "U118Z101P" {
                properties["parent_station"] = NSNull()
            }
            return ["properties": properties]
        }
        let catalogue = try MetroTestData.catalogue(stops: stops)
        #expect(catalogue.platforms.filter { $0.stationID == "flora" }.map(\.id) == ["U118Z102P"])
    }

    @Test("A reversing train makes a platform usable in either direction")
    func reversingPlatform() throws {
        var reverse = MetroTestData.patterns[1]  // Line A, toward Nemocnice Motol.
        var times = Array((reverse["stop_times"] as! [[String: Any]]).dropFirst())
        // The shortened pattern starts at Skalka's eastbound platform.
        times[0]["stop_id"] = "A-15-up"
        reverse["stop_times"] = times
        reverse["shape_id"] = "A-shared"
        let catalogue = try MetroTestData.catalogue(patterns: MetroTestData.patterns + [reverse])

        let skalka = try #require(catalogue.platforms.first { $0.id == "A-15-up" })
        #expect(skalka.towardStationID == nil)
        let board = { (headsign: String) in
            """
            {"departures": [{"route": {"type": 1, "short_name": "A"},
              "trip": {"id": "t", "headsign": "\(headsign)", "is_at_stop": false, "is_canceled": false},
              "stop": {"id": "A-15-up"},
              "departure_timestamp": {"scheduled": "2026-10-14T12:03:00Z"}}]}
            """
        }
        let toward = { (headsign: String) in
            try DepartureBoardParser.parse(
                stationID: "skalka", response: board(headsign), catalogue: catalogue
            ).departures.first?.directionTerminusStationID
        }
        #expect(try toward("Nemocnice Motol") == "nemocnice-motol")
        #expect(try toward("Depo Hostivař") == "depo-hostivar")
    }

    @Test("A new platform uses the short-turn destination to pick the full-line direction")
    func newPlatformShortTurn() throws {
        let response = MetroTestData.board
            .replacing("B-9-down", with: "new-platform")
            .replacing("Zličín", with: "Smíchovské nádraží")
        let board = try DepartureBoardParser.parse(
            stationID: "andel", response: response, catalogue: try MetroTestData.catalogue())
        #expect(board.departures.first?.directionTerminusStationID == "zlicin")
        #expect(board.departures.first?.headsign == "Smíchovské nádraží")
    }

    @Test("Trip pages continue while full and fail above 10 000 trips")
    func paging() async throws {
        let trip: [String: Any] = [
            "route_id": "L-A", "trip_id": "A-false", "shape_id": "A-false", "direction_id": 0,
        ]
        let fullPage = MetroTestData.json(Array(repeating: trip, count: 1000))
        let base = MetroTestData.transport()

        let patternPage = MetroTestData.json(
            MetroTestData.patterns.map { $0.filter { $0.key != "stop_times" } })
        let paged = FakeTransport { request in
            let query = request.url!.query() ?? ""
            if request.url!.path() == "/v2/gtfs/trips", query.contains("stopId=U1072Z101P") {
                if query.contains("offset=0") { return FakeTransport.Reply(body: fullPage) }
                if query.contains("offset=1000") { return FakeTransport.Reply(body: patternPage) }
            }
            return FakeTransport.Reply(body: try await base.data(for: request).0)
        }
        _ = try await CatalogueLoader.load(
            using: GolemioHTTPClient(transport: paged, clock: TestClock()))
        #expect(
            paged.requests.contains {
                $0.url!.query()?.contains("stopId=U1072Z101P&limit=1000&offset=1000") == true
            })

        let endless = FakeTransport { request in
            if request.url!.path() == "/v2/gtfs/trips" {
                return FakeTransport.Reply(body: fullPage)
            }
            return FakeTransport.Reply(body: try await base.data(for: request).0)
        }
        await #expect(throws: GolemioError.invalidData) {
            try await CatalogueLoader.load(
                using: GolemioHTTPClient(transport: endless, clock: TestClock()))
        }
    }
}
