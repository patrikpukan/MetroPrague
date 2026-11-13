import SwiftUI

/// Station search with a line filter. Choosing a station opens its direction picker, and
/// choosing a direction opens its departures on this tab's stack.
struct SearchView: View {
    @Environment(MetroDataStore.self) private var metroData
    @Environment(FavoritesStore.self) private var favorites

    var body: some View {
        SearchContent(metroData: metroData, favorites: favorites)
    }
}

/// Owns the search model, which needs the stores from the environment at creation.
private struct SearchContent: View {
    let metroData: MetroDataStore
    @State private var model: SearchModel
    @State private var path: [DepartureRoute] = []

    init(metroData: MetroDataStore, favorites: FavoritesStore) {
        self.metroData = metroData
        _model = State(initialValue: SearchModel(metroData: metroData, favorites: favorites))
    }

    var body: some View {
        @Bindable var model = model
        NavigationStack(path: $path) {
            List {
                Section {
                    ForEach(model.filteredStations) { station in
                        StationRow(station: station) {
                            model.selectedStation = station
                        }
                    }
                } header: {
                    LineFilterBar(selectedLine: model.selectedLine, select: model.toggleLine)
                        .textCase(nil)
                }
            }
            .overlay {
                if model.filteredStations.isEmpty {
                    ContentUnavailableView.search(text: model.query)
                }
            }
            .navigationTitle("Search")
            .sheet(item: $model.selectedStation) { _ in
                DirectionPickerSheet(model: model) { direction in
                    guard let route = model.route(for: direction) else { return }
                    model.selectedStation = nil
                    path.append(route)
                }
            }
            .navigationDestination(for: DepartureRoute.self) { route in
                StationDeparturesView(route: route, metroData: metroData)
            }
        }
        // Always visible under the title, so search is the first thing on the tab.
        .searchable(
            text: $model.query, placement: .navigationBarDrawer(displayMode: .always),
            prompt: "Station name")
    }
}

/// A station's name and the badges of its lines.
private struct StationRow: View {
    let station: Station
    let select: () -> Void

    @Environment(\.dynamicTypeSize) private var dynamicTypeSize

    var body: some View {
        // At accessibility sizes the badges move below the name, leaving it the full width.
        let layout =
            dynamicTypeSize.isAccessibilitySize
            ? AnyLayout(VStackLayout(alignment: .leading, spacing: 8))
            : AnyLayout(HStackLayout())
        Button(action: select) {
            layout {
                Text(station.name)
                    .foregroundStyle(.primary)
                    .frame(maxWidth: .infinity, alignment: .leading)
                HStack {
                    ForEach(station.lines, id: \.line) { position in
                        LineBadge(line: position.line)
                    }
                }
            }
            .contentShape(.rect)
        }
        .buttonStyle(.plain)
        .accessibilityElement(children: .combine)
    }
}

/// All, A, B, and C chips. Tapping the selected line clears the filter.
private struct LineFilterBar: View {
    let selectedLine: Line?
    let select: (Line?) -> Void

    var body: some View {
        HStack(spacing: 8) {
            chip(Text("All"), isSelected: selectedLine == nil) { select(nil) }
            ForEach(Line.allCases, id: \.self) { line in
                chip(Text(verbatim: line.rawValue), isSelected: selectedLine == line) {
                    select(line)
                }
                .accessibilityLabel(Text("Line \(line.rawValue)"))
            }
        }
        .padding(.vertical, 4)
    }

    private func chip(
        _ title: Text,
        isSelected: Bool,
        action: @escaping () -> Void
    ) -> some View {
        Button(action: action) {
            title
                .font(.subheadline.weight(.semibold))
                .padding(.horizontal, 12)
                .padding(.vertical, 6)
        }
        .buttonStyle(.plain)
        // The selected chip inverts the label colors, which keeps full contrast in both modes.
        .foregroundStyle(isSelected ? Color(.systemBackground) : Color.primary)
        .background(isSelected ? Color.primary : Color(.secondarySystemFill), in: .capsule)
        .accessibilityAddTraits(isSelected ? .isSelected : [])
    }
}

#Preview {
    SearchView()
        .environment(MetroDataStore(source: MockMetroDataSource()))
        .environment(FavoritesStore())
}
