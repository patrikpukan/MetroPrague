import Foundation
import Testing

@Suite("Shared fixtures")
struct FixturesTests {
    @Test(
        "Every departure-board fixture is a JSON object with a departures array",
        arguments: [
            "golemio/departureboards/metro-predicted-delay.json",
            "golemio/departureboards/mixed-transport-scheduled-only.json",
            "golemio/departureboards/empty-board.json",
            "golemio/departureboards/short-turn.json",
        ]
    )
    func departureBoardLoads(path: String) throws {
        let departures = try departures(in: path)
        #expect(departures != nil)
    }

    @Test("The empty board has no departures")
    func emptyBoardHasNoDepartures() throws {
        let departures = try #require(
            try departures(in: "golemio/departureboards/empty-board.json"))
        #expect(departures.isEmpty)
    }

    @Test("A missing fixture throws")
    func missingFixtureThrows() {
        #expect(throws: Fixtures.NotFound.self) {
            try Fixtures.data("golemio/departureboards/does-not-exist.json")
        }
    }

    private func departures(in path: String) throws -> [Any]? {
        let object = try JSONSerialization.jsonObject(with: Fixtures.data(path))
        return (object as? [String: Any])?["departures"] as? [Any]
    }
}
