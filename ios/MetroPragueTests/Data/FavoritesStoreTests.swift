import Foundation
import Testing

@testable import MetroPrague

@Suite("FavoritesStore")
struct FavoritesStoreTests {
    private let defaults: UserDefaults
    private let muzeumToMotol = FavoriteKey(
        stationID: "muzeum", line: .a, terminusStationID: "nemocnice-motol")
    private let muzeumToDepo = FavoriteKey(
        stationID: "muzeum", line: .a, terminusStationID: "depo-hostivar")
    private let florencToLetnany = FavoriteKey(
        stationID: "florenc", line: .c, terminusStationID: "letnany")

    init() {
        let suite = "FavoritesStoreTests-\(UUID().uuidString)"
        defaults = UserDefaults(suiteName: suite)!
        defaults.removePersistentDomain(forName: suite)
    }

    @Test("An empty store has no favorites")
    func empty() {
        #expect(FavoritesStore(defaults: defaults).favorites.isEmpty)
    }

    @Test("Insertion order is kept, and adding a duplicate does nothing")
    func orderAndDuplicates() {
        let store = FavoritesStore(defaults: defaults)
        store.add(muzeumToDepo)
        store.add(florencToLetnany)
        store.add(muzeumToMotol)
        store.add(muzeumToDepo)
        #expect(store.favorites == [muzeumToDepo, florencToLetnany, muzeumToMotol])
    }

    @Test("Remove and toggle change only the matching key")
    func removeAndToggle() {
        let store = FavoritesStore(defaults: defaults)
        store.add(muzeumToMotol)
        store.add(muzeumToDepo)
        store.remove(muzeumToMotol)
        #expect(store.favorites == [muzeumToDepo])

        store.toggle(florencToLetnany)
        #expect(store.favorites == [muzeumToDepo, florencToLetnany])
        store.toggle(muzeumToDepo)
        #expect(store.favorites == [florencToLetnany])
        #expect(store.contains(florencToLetnany))
        #expect(!store.contains(muzeumToDepo))
    }

    @Test("Favorites are saved as Android's version-1 JSON and read back")
    func storedJSON() {
        FavoritesStore(defaults: defaults).add(muzeumToDepo)
        #expect(
            defaults.string(forKey: "favorites_v1")
                == #"{"version":1,"favorites":[{"stationId":"muzeum","line":"A","terminusStationId":"depo-hostivar"}]}"#
        )
        #expect(FavoritesStore(defaults: defaults).favorites == [muzeumToDepo])
    }

    @Test(
        "Blank or malformed data is no favorites",
        arguments: ["", "  ", "not json", #"{"version":1,"favorites":[{"line":"A"}]}"#])
    func malformed(value: String) {
        defaults.set(value, forKey: "favorites_v1")
        #expect(FavoritesStore(defaults: defaults).favorites.isEmpty)
    }

    @Test("An entry with an unknown line is skipped and its valid sibling kept")
    func unknownLine() {
        defaults.set(
            #"{"version":1,"favorites":[{"stationId":"muzeum","line":"Z","terminusStationId":"unknown"},{"stationId":"muzeum","line":"A","terminusStationId":"depo-hostivar"}]}"#,
            forKey: "favorites_v1")
        #expect(FavoritesStore(defaults: defaults).favorites == [muzeumToDepo])
    }
}
