import Foundation
import SwiftUI
import Testing

@testable import MetroPrague

@Suite("Theme selection")
struct ThemeSelectionTests {
    @Test("A chosen theme persists and maps to the app's color scheme")
    func selection() throws {
        let suite = "ThemeSelectionTests-\(UUID().uuidString)"
        let defaults = try #require(UserDefaults(suiteName: suite))
        defer { defaults.removePersistentDomain(forName: suite) }

        let settings = AppSettings(defaults: defaults)
        #expect(settings.theme.colorScheme == nil)
        settings.theme = .dark
        #expect(settings.theme.colorScheme == .dark)
        #expect(AppSettings(defaults: defaults).theme == .dark)
        settings.theme = .light
        #expect(AppSettings(defaults: defaults).theme.colorScheme == .light)
    }

    @Test("Option labels")
    func labels() {
        #expect(
            ThemePreference.allCases.map { String(localized: $0.label) } == [
                "System default", "Light", "Dark",
            ])
    }
}
