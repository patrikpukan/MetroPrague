import Foundation
import Testing

@testable import MetroPrague

@Suite("SearchModel")
struct SearchModelTests {
    private let now = Date(timeIntervalSince1970: 1_791_288_000)
    private let favorites: FavoritesStore

    init() {
        let suite = "SearchModelTests-\(UUID().uuidString)"
        let defaults = UserDefaults(suiteName: suite)!
        defaults.removePersistentDomain(forName: suite)
        favorites = FavoritesStore(defaults: defaults)
    }

    private func model(
        board: @escaping @Sendable (String, Date) -> DepartureBoard = { stationID, now in
            DepartureBoard(stationID: stationID, departures: [], fetchedAt: now)
        }
    ) -> SearchModel {
        SearchModel(
            metroData: MetroDataStore(source: FixedMetroDataSource(boards: board)),
            favorites: favorites, timeZone: TimeZone(identifier: "UTC")!)
    }

    private func station(_ id: String) -> Station {
        MetroNetwork.stations.first { $0.id == id }!.domainStation
    }

    // MARK: Search and filter

    @Test("An empty query matches every station")
    func emptyQuery() {
        #expect(model().filteredStations.count == 58)
    }

    @Test("The query matches a case-insensitive substring", arguments: ["muz", "MUZ", "zeu"])
    func query(text: String) {
        let model = model()
        model.query = text
        #expect(model.filteredStations.map(\.name) == ["Muzeum"])
    }

    @Test("A query without matches finds nothing; diacritics must match")
    func noMatch() {
        let model = model()
        model.query = "xyz"
        #expect(model.filteredStations.isEmpty)
        model.query = "mustek"
        #expect(model.filteredStations.isEmpty)
    }

    @Test("The line filter keeps stations serving the line, combined with the query")
    func lineFilter() {
        let model = model()
        model.toggleLine(.c)
        #expect(model.selectedLine == .c)
        #expect(model.filteredStations.count == 20)
        model.query = "m"
        #expect(model.filteredStations.map(\.name) == ["Muzeum"])
        model.toggleLine(.a)
        #expect(
            model.filteredStations.map(\.name) == [
                "Nemocnice Motol", "Malostranská", "Staroměstská", "Můstek", "Muzeum",
                "Náměstí Míru",
            ])
    }

    @Test("Selecting the selected line, or All, clears the filter")
    func clearFilter() {
        let model = model()
        model.toggleLine(.b)
        model.toggleLine(.b)
        #expect(model.selectedLine == nil)
        model.toggleLine(.a)
        model.toggleLine(nil)
        #expect(model.selectedLine == nil)
        #expect(model.filteredStations.count == 58)
    }

    // MARK: Directions

    @Test("No rows without a selected station")
    func noSelection() {
        #expect(model().directionRows(at: now).isEmpty)
    }

    @Test("A terminus has one direction, an interchange four")
    func directions() {
        let model = model()
        model.selectedStation = station("haje")
        #expect(model.directionRows(at: now).map(\.direction.terminusStationID) == ["letnany"])
        model.selectedStation = station("muzeum")
        #expect(
            model.directionRows(at: now).map(\.direction.terminusStationID) == [
                "nemocnice-motol", "depo-hostivar", "letnany", "haje",
            ])
    }

    @Test("Rows preview the next departure, else no service")
    func nextDeparture() {
        let model = model { stationID, now in
            DepartureBoard(
                stationID: stationID,
                departures: [
                    Departure(
                        tripID: "t", line: .b, headsign: "Nové Butovice",
                        scheduled: now.addingTimeInterval(150), predicted: nil, delaySeconds: nil,
                        isAtStop: false, isCanceled: false, directionTerminusStationID: "zlicin")
                ],
                fetchedAt: now)
        }
        model.selectedStation = station("andel")
        let rows = model.directionRows(at: now)
        #expect(rows.map(\.display) == [.inMinutes(2), .noService])
        #expect(rows.map(\.destination) == ["Nové Butovice", "Černý Most"])
    }

    @Test("Stale and unavailable boards have their own states")
    func boardStates() {
        let stale = model { stationID, now in
            DepartureBoard(
                stationID: stationID, departures: [], fetchedAt: now.addingTimeInterval(-300))
        }
        stale.selectedStation = station("andel")
        #expect(stale.directionRows(at: now).map(\.display) == [.stale, .stale])

        let unavailable = model { stationID, _ in
            DepartureBoard(stationID: stationID, departures: [], isUnavailable: true)
        }
        unavailable.selectedStation = station("andel")
        #expect(unavailable.directionRows(at: now).map(\.display) == [.unavailable, .unavailable])
    }

    // MARK: Favorites

    @Test("Toggling a displayed direction adds and removes the exact favorite")
    func toggleFavorite() throws {
        let model = model()
        model.selectedStation = station("muzeum")
        let toHaje = try #require(model.directionRows(at: now).last).direction

        model.toggleFavorite(toHaje)
        #expect(
            favorites.favorites == [
                FavoriteKey(stationID: "muzeum", line: .c, terminusStationID: "haje")
            ])
        #expect(model.directionRows(at: now).map(\.isFavorite) == [false, false, false, true])

        model.toggleFavorite(toHaje)
        #expect(favorites.favorites.isEmpty)
        #expect(model.directionRows(at: now).allSatisfy { !$0.isFavorite })
    }

    @Test("A direction not displayed for the selected station is not toggled")
    func toggleGuard() {
        let model = model()
        let towardZlicin = Direction(line: .b, terminusStationID: "zlicin", terminusName: "Zličín")
        model.toggleFavorite(towardZlicin)
        model.selectedStation = station("muzeum")
        model.toggleFavorite(towardZlicin)
        #expect(favorites.favorites.isEmpty)
    }

    @Test("Choosing a direction gives its departures route")
    func route() {
        let model = model()
        model.selectedStation = station("muzeum")
        let direction = Direction(line: .c, terminusStationID: "haje", terminusName: "Háje")
        #expect(
            model.route(for: direction)
                == DepartureRoute(stationID: "muzeum", line: .c, terminusStationID: "haje"))
    }
}
