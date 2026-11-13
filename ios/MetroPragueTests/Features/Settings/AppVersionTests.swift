import Foundation
import Testing

@testable import MetroPrague

@Suite("AppVersion")
struct AppVersionTests {
    @Test("Shows the version and build as \"1.0 (1)\"")
    func label() {
        let version = AppVersion(infoDictionary: [
            "CFBundleShortVersionString": "1.0", "CFBundleVersion": "1",
        ])
        #expect(String(localized: version.label) == "1.0 (1)")
    }

    @Test("Missing values show a placeholder")
    func missing() {
        #expect(String(localized: AppVersion(infoDictionary: [:]).label) == "? (?)")
    }
}
