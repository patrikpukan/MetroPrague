import Foundation
import Testing

@testable import MetroPrague

@Suite("AppSettings")
struct AppSettingsTests {
    private let defaults: UserDefaults

    init() {
        let suite = "AppSettingsTests-\(UUID().uuidString)"
        defaults = UserDefaults(suiteName: suite)!
        defaults.removePersistentDomain(forName: suite)
    }

    @Test("The theme defaults to System")
    func defaultTheme() {
        #expect(AppSettings(defaults: defaults).theme == .system)
    }

    @Test("A chosen theme is saved under theme_preference", arguments: ThemePreference.allCases)
    func roundTrip(theme: ThemePreference) {
        AppSettings(defaults: defaults).theme = theme
        #expect(defaults.string(forKey: "theme_preference") == theme.storedValue)
        #expect(AppSettings(defaults: defaults).theme == theme)
    }

    @Test("An unknown stored value is System")
    func unknownValue() {
        defaults.set("sepia", forKey: "theme_preference")
        #expect(AppSettings(defaults: defaults).theme == .system)
    }
}
