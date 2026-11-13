import Foundation
import Testing

@testable import MetroPrague

@Suite("MockMetroDataSource")
struct MockMetroDataSourceTests {
    private let source = MockMetroDataSource(timeZone: TimeZone(identifier: "Europe/Prague")!)

    /// 2026-01-15 at `hour`:00 Prague time (UTC+1).
    private func date(hour: Int) -> Date {
        Date(timeIntervalSince1970: 1_768_431_600 + TimeInterval(hour * 3600))
    }

    private func gaps(_ departures: [Departure], headsign: String) -> [TimeInterval] {
        let times = departures.filter { $0.headsign == headsign }.map(\.scheduled).sorted()
        return zip(times, times.dropFirst()).map { $1.timeIntervalSince($0) }
    }

    @Test("No departures during the night break")
    func noService() {
        #expect(source.board(for: "andel", in: nil, at: date(hour: 2)).departures.isEmpty)
    }

    @Test("Peak hours have five future departures per direction, two minutes apart")
    func peak() {
        let now = date(hour: 8)
        let board = source.board(for: "andel", in: nil, at: now)

        #expect(Set(board.departures.map(\.headsign)) == ["Zličín", "Černý Most"])
        #expect(board.departures.filter { $0.headsign == "Zličín" }.count == 5)
        #expect(board.departures.allSatisfy { $0.effectiveTime > now })
        #expect(
            board.departures.map(\.effectiveTime) == board.departures.map(\.effectiveTime).sorted())
        #expect(gaps(board.departures, headsign: "Zličín") == [120, 120, 120, 120])
    }

    @Test("Off-peak departures are five minutes apart")
    func offPeak() {
        let board = source.board(for: "andel", in: nil, at: date(hour: 12))
        #expect(gaps(board.departures, headsign: "Zličín") == [300, 300, 300, 300])
    }

    @Test("An interchange has four directions, and every fifth trip is late")
    func interchangeAndDelays() {
        let board = source.board(for: "muzeum", in: nil, at: date(hour: 8))
        #expect(Set(board.departures.map { "\($0.line)\($0.headsign)" }).count == 4)
        let delayed = (0..<6).flatMap {
            source.board(
                for: "andel", in: nil, at: date(hour: 8).addingTimeInterval(Double($0 * 120))
            )
            .departures
        }
        #expect(
            delayed.contains {
                $0.delaySeconds == 60 && $0.predicted == $0.scheduled.addingTimeInterval(60)
            })
        #expect(delayed.contains { $0.delaySeconds == 0 })
    }
}
