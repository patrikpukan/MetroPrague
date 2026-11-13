import SwiftUI

struct SearchView: View {
    var body: some View {
        NavigationStack {
            ContentUnavailableView(
                "Station search",
                systemImage: "magnifyingglass",
                description: Text("Coming soon.")
            )
            .navigationTitle("Search")
        }
    }
}

#Preview {
    SearchView()
}
