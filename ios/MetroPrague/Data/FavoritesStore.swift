import Foundation
import Observation

/// The user's favorite station directions, in insertion order.
///
/// Persisted in `UserDefaults` under `favorites_v1` as the version-1 JSON shared with Android:
/// `{"version":1,"favorites":[{"stationId":…,"line":"A","terminusStationId":…}]}`.
@Observable
final class FavoritesStore {
    static let defaultsKey = "favorites_v1"

    private(set) var favorites: [FavoriteKey]

    @ObservationIgnored private let defaults: UserDefaults

    init(defaults: UserDefaults = .standard) {
        self.defaults = defaults
        favorites = Self.decode(defaults.string(forKey: Self.defaultsKey))
    }

    func contains(_ key: FavoriteKey) -> Bool {
        favorites.contains(key)
    }

    /// Appends `key`; does nothing when it is already a favorite.
    func add(_ key: FavoriteKey) {
        guard !favorites.contains(key) else { return }
        save(favorites + [key])
    }

    func remove(_ key: FavoriteKey) {
        save(favorites.filter { $0 != key })
    }

    /// Removes `key` when it is a favorite, otherwise appends it.
    func toggle(_ key: FavoriteKey) {
        if favorites.contains(key) { remove(key) } else { add(key) }
    }

    private func save(_ newFavorites: [FavoriteKey]) {
        favorites = newFavorites
        defaults.set(Self.encode(newFavorites), forKey: Self.defaultsKey)
    }

    // MARK: Version-1 JSON

    private struct StoredFavorites: Decodable {
        var version: Int?
        var favorites: [StoredFavorite]?
    }

    private struct StoredFavorite: Decodable {
        let stationId: String
        let line: String
        let terminusStationId: String
    }

    /// Missing, blank, or malformed data is an empty list; entries with an unknown line are
    /// skipped.
    static func decode(_ value: String?) -> [FavoriteKey] {
        guard let value, !value.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty,
            let stored = try? JSONDecoder().decode(StoredFavorites.self, from: Data(value.utf8))
        else { return [] }
        return (stored.favorites ?? []).compactMap { favorite in
            Line(rawValue: favorite.line).map {
                FavoriteKey(
                    stationID: favorite.stationId, line: $0,
                    terminusStationID: favorite.terminusStationId)
            }
        }
    }

    /// Writes the keys in Android's order: `version` first, then each favorite's `stationId`,
    /// `line`, and `terminusStationId`.
    static func encode(_ favorites: [FavoriteKey]) -> String {
        let entries = favorites.map { favorite in
            #"{"stationId":\#(jsonString(favorite.stationID)),"line":\#(jsonString(favorite.line.rawValue)),"#
                + #""terminusStationId":\#(jsonString(favorite.terminusStationID))}"#
        }
        return #"{"version":1,"favorites":[\#(entries.joined(separator: ","))]}"#
    }

    /// `value` as a quoted, escaped JSON string.
    private static func jsonString(_ value: String) -> String {
        let encoder = JSONEncoder()
        encoder.outputFormatting = .withoutEscapingSlashes
        // Encoding a string cannot fail.
        let data = (try? encoder.encode(value)) ?? Data(#""""#.utf8)
        return String(decoding: data, as: UTF8.self)
    }
}
