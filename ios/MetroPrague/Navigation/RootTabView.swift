import SwiftUI

/// The app's tab bar. Each tab's root view owns its own `NavigationStack`.
struct RootTabView: View {
    @State private var selectedTab: AppTab = .home

    var body: some View {
        TabView(selection: $selectedTab) {
            Tab("Home", systemImage: "house", value: .home) {
                HomeView { selectedTab = .search }
            }
            Tab("Settings", systemImage: "gear", value: .settings) {
                SettingsView()
            }
            // The search role lets the system place and style this as the search tab (trailing).
            Tab("Search", systemImage: "magnifyingglass", value: .search, role: .search) {
                SearchView()
            }
        }
    }
}

#Preview {
    RootTabView()
        .environment(MetroDataStore(source: MockMetroDataSource()))
        .environment(FavoritesStore())
        .environment(AppSettings())
}
