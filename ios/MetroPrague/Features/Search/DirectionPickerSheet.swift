import SwiftUI

/// The directions at the selected station. Choosing one opens its departures.
struct DirectionPickerSheet: View {
    let model: SearchModel
    let choose: (Direction) -> Void

    @Environment(\.dismiss) private var dismiss

    var body: some View {
        NavigationStack {
            TimelineView(.periodic(from: .now, by: 15)) { context in
                List {
                    Section("Choose a direction") {
                        ForEach(model.directionRows(at: context.date)) { row in
                            DirectionRow(
                                line: row.direction.line,
                                destination: row.destination,
                                display: row.display,
                                action: { choose(row.direction) }
                            ) {
                                FavoriteButton(isFavorite: row.isFavorite) {
                                    model.toggleFavorite(row.direction)
                                }
                            }
                        }
                    }
                }
            }
            .navigationTitle(model.selectedStation?.name ?? "")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Close", systemImage: "xmark", role: .close) {
                        dismiss()
                    }
                }
            }
        }
        .presentationDetents([.medium, .large])
    }
}

#Preview {
    let model = SearchModel(
        metroData: MetroDataStore(source: MockMetroDataSource()), favorites: FavoritesStore())
    model.selectedStation = MetroNetwork.stations.first { $0.id == "muzeum" }?.domainStation
    return Color.clear
        .sheet(isPresented: .constant(true)) {
            DirectionPickerSheet(model: model) { _ in }
        }
}
