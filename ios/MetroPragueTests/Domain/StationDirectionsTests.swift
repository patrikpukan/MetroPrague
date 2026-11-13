import Testing

@testable import MetroPrague

@Suite("StationDirections")
struct StationDirectionsTests {
    private let stations = MetroNetwork.stations.map(\.domainStation)

    private func station(_ id: String) throws -> Station {
        try #require(stations.first { $0.id == id })
    }

    @Test("Every line has its two termini in line order")
    func terminiOfEveryLine() {
        #expect(
            StationDirections.termini(of: .a, in: stations).map(\.name) == [
                "Nemocnice Motol", "Depo Hostivař",
            ])
        #expect(
            StationDirections.termini(of: .b, in: stations).map(\.name) == ["Zličín", "Černý Most"])
        #expect(StationDirections.termini(of: .c, in: stations).map(\.name) == ["Letňany", "Háje"])
    }

    @Test("A line with one station has one terminus, and none without stations")
    func terminiOfShortLines() throws {
        let only = Station(id: "x", name: "X", lines: [LinePosition(line: .a, order: 3)])
        #expect(StationDirections.termini(of: .a, in: [only]) == [only])
        #expect(StationDirections.termini(of: .b, in: [only]).isEmpty)
    }

    @Test("A middle station has both termini of its line")
    func middleStation() throws {
        #expect(
            StationDirections.directions(at: try station("andel"), in: stations) == [
                Direction(line: .b, terminusStationID: "zlicin", terminusName: "Zličín"),
                Direction(line: .b, terminusStationID: "cerny-most", terminusName: "Černý Most"),
            ])
    }

    @Test("A terminus has only the opposite terminus")
    func terminus() throws {
        #expect(
            StationDirections.directions(at: try station("haje"), in: stations) == [
                Direction(line: .c, terminusStationID: "letnany", terminusName: "Letňany")
            ])
    }

    @Test("An interchange has every direction in line and terminus order")
    func interchange() throws {
        #expect(
            StationDirections.directions(at: try station("muzeum"), in: stations) == [
                Direction(
                    line: .a, terminusStationID: "nemocnice-motol", terminusName: "Nemocnice Motol"),
                Direction(
                    line: .a, terminusStationID: "depo-hostivar", terminusName: "Depo Hostivař"),
                Direction(line: .c, terminusStationID: "letnany", terminusName: "Letňany"),
                Direction(line: .c, terminusStationID: "haje", terminusName: "Háje"),
            ])
    }
}
