import Foundation
import Testing

@testable import MetroPrague

@Suite("Settings metro data text")
struct MetroDataStatusTextTests {
    private func text(_ resource: LocalizedStringResource?) -> String? {
        resource.map { String(localized: $0) }
    }

    @Test("Without saved data, a hint asks for a reload")
    func notLoaded() {
        let status = MetroDataStatus(canReload: true)
        #expect(
            status.savedDataLines.map { String(localized: $0) } == [
                "No metro data saved yet. Reload to load stations and departures."
            ])
        #expect(status.progressMessage == nil)
        #expect(status.errorMessage == nil)
        #expect(!status.isReloading)
    }

    @Test("Saved data shows its counts and reload time")
    func loaded() {
        let reloadedAt = Date(timeIntervalSince1970: 1_791_288_000)
        let status = MetroDataStatus(
            stationCount: 58, departureCount: 1_200, reloadedAt: reloadedAt)
        let lines = status.savedDataLines.map { String(localized: $0) }
        #expect(lines.count == 2)
        #expect(lines.first?.hasPrefix("58 stations · ") == true)
        #expect(lines.first?.hasSuffix(" saved departures") == true)
        #expect(
            lines.last
                == "Last reload: \(reloadedAt.formatted(date: .abbreviated, time: .shortened))")
    }

    @Test("Progress for each phase")
    func progress() {
        let catalogue = MetroDataStatus(phase: .loadingCatalogue)
        #expect(text(catalogue.progressMessage) == "Loading routes, stations and platforms…")
        #expect(catalogue.progressFraction == nil)
        #expect(catalogue.isReloading)

        let boards = MetroDataStatus(phase: .loadingBoards(completed: 29, total: 58))
        #expect(text(boards.progressMessage) == "Loading departures: 29 of 58 stations…")
        #expect(boards.progressFraction == 0.5)
    }

    @Test(
        "Each error says the saved data was kept",
        arguments: [
            (
                MetroReloadError.authentication,
                "Golemio rejected the API key. Previous saved data was kept."
            ),
            (
                .rateLimited,
                "Golemio’s request limit was reached. Wait before trying again. Previous saved data was kept."
            ),
            (
                .networkOrData,
                "Could not reload metro data. Check your connection and try again. Previous saved data was kept."
            ),
        ])
    func errors(error: MetroReloadError, message: String) {
        #expect(text(MetroDataStatus(error: error).errorMessage) == message)
    }
}
