import Foundation

/// The Settings text for the saved metro data and the reload. Pure, so each state is testable.
extension MetroDataStatus {
    /// The saved counts and reload time, or a hint when nothing is saved yet.
    var savedDataLines: [LocalizedStringResource] {
        guard let reloadedAt else {
            return ["No metro data saved yet. Reload to load stations and departures."]
        }
        let reloadTime = reloadedAt.formatted(date: .abbreviated, time: .shortened)
        return [
            "\(stationCount) stations · \(departureCount) saved departures",
            "Last reload: \(reloadTime)",
        ]
    }

    /// What the running reload is doing, or `nil` when idle.
    var progressMessage: LocalizedStringResource? {
        switch phase {
        case .idle: nil
        case .loadingCatalogue: "Loading routes, stations and platforms…"
        case .loadingBoards(let completed, let total):
            "Loading departures: \(completed) of \(total) stations…"
        }
    }

    /// The share of stations loaded, or `nil` while the amount of work is not yet known.
    var progressFraction: Double? {
        guard case .loadingBoards(let completed, let total) = phase, total > 0 else { return nil }
        return Double(completed) / Double(total)
    }

    var isReloading: Bool { phase != .idle }

    /// Why the last reload failed. Each message says the saved data was kept.
    var errorMessage: LocalizedStringResource? {
        switch error {
        case nil: nil
        case .authentication: "Golemio rejected the API key. Previous saved data was kept."
        case .rateLimited:
            "Golemio’s request limit was reached. Wait before trying again. Previous saved data was kept."
        case .networkOrData:
            "Could not reload metro data. Check your connection and try again. Previous saved data was kept."
        }
    }
}
