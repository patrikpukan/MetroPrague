import SwiftUI

struct HomeView: View {
    var body: some View {
        NavigationStack {
            ContentUnavailableView(
                "No favorites yet",
                systemImage: "star",
                description: Text("Search for a station and pick a direction to add it here.")
            )
            .navigationTitle("Favorites")
        }
    }
}

#Preview {
    HomeView()
}
