import SwiftUI

struct SearchView: View {
    @Environment(MetroDataStore.self) private var metroData

    var body: some View {
        NavigationStack {
            ContentUnavailableView(
                "Station search",
                systemImage: "magnifyingglass",
                description: Text("Coming soon.")
            )
            .navigationTitle("Search")
            .navigationDestination(for: DepartureRoute.self) { route in
                StationDeparturesView(route: route, metroData: metroData)
            }
        }
    }
}

#Preview {
    SearchView()
        .environment(MetroDataStore(source: MockMetroDataSource()))
}
