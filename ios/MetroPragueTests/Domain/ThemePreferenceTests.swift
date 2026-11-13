import Testing

@testable import MetroPrague

@Suite("ThemePreference")
struct ThemePreferenceTests {
    @Test("Stored values round-trip", arguments: ThemePreference.allCases)
    func roundTrip(preference: ThemePreference) {
        #expect(ThemePreference(storedValue: preference.storedValue) == preference)
    }

    @Test("Missing or unknown values resolve to System", arguments: [nil, "", "Dark", "sepia"])
    func fallback(value: String?) {
        #expect(ThemePreference(storedValue: value) == .system)
    }
}
