import Testing

@testable import MetroPrague

@Suite("MetroNetwork")
struct MetroNetworkTests {
    @Test("The network has 58 unique stations")
    func uniqueStations() {
        #expect(MetroNetwork.stations.count == 58)
        #expect(Set(MetroNetwork.stations.map(\.id)).count == 58)
    }

    @Test("Line memberships", arguments: [(Line.a, 17), (.b, 24), (.c, 20)])
    func memberships(line: Line, count: Int) {
        #expect(MetroNetwork.stations.filter { $0.lineOrders[line] != nil }.count == count)
    }

    @Test(
        "Station IDs",
        arguments: [
            ("I. P. Pavlova", "ip-pavlova"),
            ("Nádraží Holešovice", "nadrazi-holesovice"),
            ("Pražského povstání", "prazskeho-povstani"),
            ("Černý Most", "cerny-most"),
            ("Želivského", "zelivskeho"),
            ("  Jiřího z Poděbrad ", "jiriho-z-podebrad"),
        ])
    func stationID(name: String, id: String) {
        #expect(MetroNetwork.stationID(for: name) == id)
    }

    @Test("Interchanges keep each line's order")
    func interchangeOrders() throws {
        let muzeum = try #require(MetroNetwork.stations.first { $0.id == "muzeum" })
        #expect(muzeum.lineOrders == [.a: 9, .c: 9])
        #expect(
            muzeum.domainStation.lines == [
                LinePosition(line: .a, order: 9), LinePosition(line: .c, order: 9),
            ])
    }
}
