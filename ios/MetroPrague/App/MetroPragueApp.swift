import SwiftUI

@main
struct MetroPragueApp: App {
    @State private var metroData = MetroDataStore(source: Self.makeDataSource())
    @State private var favorites = FavoritesStore()
    @State private var settings = AppSettings()

    var body: some Scene {
        WindowGroup {
            RootTabView()
                .environment(metroData)
                .environment(favorites)
                .environment(settings)
                .preferredColorScheme(settings.theme.colorScheme)
                // Reads saved data only; nothing is requested at launch.
                .task { await metroData.loadSavedData() }
        }
    }

    /// Live data needs a Golemio key, which only Debug builds can contain. Other builds use
    /// generated departures, as on Android.
    private static func makeDataSource() -> any MetroDataSource {
        guard let apiKey = AppConfiguration.main.golemioAPIKey else { return MockMetroDataSource() }
        return GolemioMetroDataSource(client: GolemioHTTPClient(apiKey: apiKey))
    }
}
