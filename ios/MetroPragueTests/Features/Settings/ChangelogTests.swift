import Foundation
import Testing

@testable import MetroPrague

@Suite("Changelog")
struct ChangelogTests {
    @Test("Version 1.0 lists its release notes")
    func firstRelease() throws {
        let entry = try #require(ChangelogEntry.all.first)
        #expect(ChangelogEntry.all.count == 1)
        #expect(entry.version == "1.0")
        #expect(
            entry.notes.map { String(localized: $0) } == [
                "Initial release.", "Metro Prague line and station search.", "Theme preferences.",
            ])
    }
}
