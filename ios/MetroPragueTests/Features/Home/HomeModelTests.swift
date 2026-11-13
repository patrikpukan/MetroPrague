import Foundation
import Testing

@testable import MetroPrague

@Suite("HomeModel")
struct HomeModelTests {
    private let now = Date(timeIntervalSince1970: 1_791_288_000)
    private let favorites: FavoritesStore
    private let muzeumToHaje = FavoriteKey(stationID: "muzeum", line: .c, terminusStationID: "haje")
    private let andelToZlicin = FavoriteKey(
        stationID: "andel", line: .b, terminusStationID: "zlicin")

    init() {
        let suite = "HomeModelTests-\(UUID().uuidString)"
        let defaults = UserDefaults(suiteName: suite)!
        defaults.removePersistentDomain(forName: suite)
        favorites = FavoritesStore(defaults: defaults)
    }

    private func model(
        board: @escaping @Sendable (String, Date) -> DepartureBoard = { stationID, now in
            DepartureBoard(stationID: stationID, departures: [], fetchedAt: now)
        }
    ) -> HomeModel {
        HomeModel(
            metroData: MetroDataStore(source: FixedMetroDataSource(boards: board)),
            favorites: favorites, timeZone: TimeZone(identifier: "UTC")!)
    }

    @Test("No favorites, no rows")
    func empty() {
        #expect(model().rows(at: now).isEmpty)
    }

    @Test("Favorites resolve names and keep their stored order")
    func order() {
        favorites.add(muzeumToHaje)
        favorites.add(andelToZlicin)
        let rows = model().rows(at: now)
        #expect(rows.map(\.key) == [muzeumToHaje, andelToZlicin])
        #expect(rows.map(\.stationName) == ["Muzeum", "Anděl"])
        #expect(rows.map(\.destination) == ["Háje", "Zličín"])
        #expect(rows.map(\.line) == [.c, .b])
    }

    @Test("Favorites that no longer resolve are omitted")
    func unresolvable() {
        favorites.add(
            FavoriteKey(stationID: "unknown", line: .a, terminusStationID: "depo-hostivar"))
        favorites.add(FavoriteKey(stationID: "andel", line: .a, terminusStationID: "depo-hostivar"))
        favorites.add(andelToZlicin)
        #expect(model().rows(at: now).map(\.key) == [andelToZlicin])
    }

    @Test("Display states: next departure, no service, stale, unavailable")
    func displays() {
        favorites.add(andelToZlicin)
        let live = model { stationID, now in
            DepartureBoard(
                stationID: stationID,
                departures: [
                    Departure(
                        tripID: "t", line: .b, headsign: "Nové Butovice",
                        scheduled: now.addingTimeInterval(240), predicted: nil, delaySeconds: nil,
                        isAtStop: false, isCanceled: false, directionTerminusStationID: "zlicin")
                ],
                fetchedAt: now)
        }
        #expect(live.rows(at: now).first?.display == .inMinutes(4))
        #expect(live.rows(at: now).first?.destination == "Nové Butovice")
        #expect(model().rows(at: now).first?.display == .noService)

        let stale = model { stationID, now in
            DepartureBoard(
                stationID: stationID, departures: [], fetchedAt: now.addingTimeInterval(-400))
        }
        #expect(stale.rows(at: now).first?.display == .stale)

        let unavailable = model { stationID, _ in
            DepartureBoard(stationID: stationID, departures: [], isUnavailable: true)
        }
        #expect(unavailable.rows(at: now).first?.display == .unavailable)
    }

    @Test("Removing a favorite removes its row")
    func remove() {
        favorites.add(muzeumToHaje)
        favorites.add(andelToZlicin)
        let model = model()
        model.remove(muzeumToHaje)
        #expect(model.rows(at: now).map(\.key) == [andelToZlicin])
        #expect(favorites.favorites == [andelToZlicin])
    }
}
