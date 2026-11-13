import SwiftUI

/// The favorite station directions with their next departures.
struct HomeView: View {
    /// Opens the Search tab from the empty state.
    var openSearch: () -> Void = {}

    @Environment(MetroDataStore.self) private var metroData
    @Environment(FavoritesStore.self) private var favorites

    var body: some View {
        HomeContent(metroData: metroData, favorites: favorites, openSearch: openSearch)
    }
}

/// Owns the Home model, which needs the stores from the environment at creation.
private struct HomeContent: View {
    let openSearch: () -> Void
    @State private var model: HomeModel

    init(metroData: MetroDataStore, favorites: FavoritesStore, openSearch: @escaping () -> Void) {
        self.openSearch = openSearch
        _model = State(initialValue: HomeModel(metroData: metroData, favorites: favorites))
    }

    var body: some View {
        NavigationStack {
            TimelineView(.periodic(from: .now, by: 15)) { context in
                let rows = model.rows(at: context.date)
                List {
                    ForEach(rows) { row in
                        DirectionRow(
                            line: row.line, stationName: row.stationName,
                            destination: row.destination, display: row.display
                        ) {
                            Button("Remove from favorites", systemImage: "star.slash") {
                                model.remove(row.key)
                            }
                            .labelStyle(.iconOnly)
                            .foregroundStyle(.secondary)
                            .frame(minWidth: 44, minHeight: 44)
                        }
                        .swipeActions {
                            Button(
                                "Remove from favorites", systemImage: "trash", role: .destructive
                            ) {
                                model.remove(row.key)
                            }
                        }
                    }
                }
                .overlay {
                    if rows.isEmpty {
                        ContentUnavailableView {
                            Label("No favorites yet", systemImage: "star")
                        } description: {
                            Text("Search for a station and pick a direction to add it here.")
                        } actions: {
                            Button("Search stations", action: openSearch)
                                .buttonStyle(.borderedProminent)
                        }
                    }
                }
            }
            .navigationTitle("Favorites")
        }
    }
}

#if DEBUG
    #Preview("Favorites") {
        let defaults = UserDefaults(suiteName: "HomeViewPreview")!
        defaults.removePersistentDomain(forName: "HomeViewPreview")
        let favorites = FavoritesStore(defaults: defaults)
        favorites.add(FavoriteKey(stationID: "muzeum", line: .c, terminusStationID: "haje"))
        favorites.add(FavoriteKey(stationID: "andel", line: .b, terminusStationID: "zlicin"))
        return HomeView()
            .environment(MetroDataStore(source: MockMetroDataSource()))
            .environment(favorites)
    }

    #Preview("Empty") {
        let defaults = UserDefaults(suiteName: "HomeViewEmptyPreview")!
        defaults.removePersistentDomain(forName: "HomeViewEmptyPreview")
        return HomeView()
            .environment(MetroDataStore(source: MockMetroDataSource()))
            .environment(FavoritesStore(defaults: defaults))
    }
#endif
