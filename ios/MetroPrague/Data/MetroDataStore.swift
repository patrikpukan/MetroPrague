import Foundation
import Observation

/// Why the last reload failed. Saved data is always kept.
nonisolated enum MetroReloadError: Hashable, Sendable {
    /// HTTP 401 or 403.
    case authentication
    /// HTTP 429.
    case rateLimited
    /// Anything else.
    case networkOrData

    init(_ error: any Error) {
        switch error {
        case GolemioError.http(401), GolemioError.http(403): self = .authentication
        case GolemioError.http(429): self = .rateLimited
        default: self = .networkOrData
        }
    }
}

/// What Settings shows about saved data and the reload.
nonisolated struct MetroDataStatus: Hashable, Sendable {
    var canReload = false
    var phase = MetroReloadPhase.idle
    var stationCount = 0
    var departureCount = 0
    var reloadedAt: Date? = nil
    var error: MetroReloadError? = nil
}

/// The app's metro data: stations, saved boards, and the user-started reload.
///
/// It reads the saved snapshot once at launch and never makes a request on its own; only
/// `reload()` downloads.
@Observable
final class MetroDataStore {
    /// The saved catalogue's stations, or the bundled network before the first reload.
    private(set) var stations: [Station]
    private(set) var phase = MetroReloadPhase.idle
    private(set) var error: MetroReloadError?
    private var data: LoadedSnapshot?

    @ObservationIgnored private let source: any MetroDataSource
    @ObservationIgnored private var hasLoaded = false
    @ObservationIgnored private var reloadTask: Task<Void, Never>?

    init(source: any MetroDataSource) {
        self.source = source
        stations = MetroNetwork.stations.map(\.domainStation)
    }

    var status: MetroDataStatus {
        MetroDataStatus(
            canReload: source.canReload,
            phase: phase,
            stationCount: data?.snapshot.catalogue.stations.count ?? 0,
            departureCount: data?.snapshot.departureCount ?? 0,
            reloadedAt: data?.snapshot.reloadedAt,
            error: error
        )
    }

    /// The board of `stationID` at `now`. Without saved data it is unavailable and stale (a
    /// reload is needed); a station without a readable saved board is unavailable.
    func board(for stationID: String, at now: Date) -> DepartureBoard {
        source.board(for: stationID, in: data, at: now)
    }

    /// Reads the saved snapshot. Only the first call has an effect.
    func loadSavedData() async {
        guard !hasLoaded else { return }
        hasLoaded = true
        let loaded = await source.loadSnapshot()
        // A reload that finished in the meantime is newer.
        if data == nil, let loaded { apply(loaded) }
    }

    /// Starts a reload and returns immediately. The store owns the work, so leaving Settings
    /// does not cancel it. Calls while a reload is running, or when the source cannot reload,
    /// are ignored. A failure keeps the previous snapshot and sets `error`.
    func reload() {
        guard source.canReload, reloadTask == nil else { return }
        error = nil
        phase = .loadingCatalogue
        reloadTask = Task {
            do {
                let loaded = try await source.reload { phase in
                    self.phase = phase
                }
                apply(loaded)
            } catch is CancellationError {
                // Cancelled work keeps the saved data and reports nothing.
            } catch {
                self.error = MetroReloadError(error)
            }
            phase = .idle
            reloadTask = nil
        }
    }

    /// Waits until the current reload, if any, has finished.
    func waitForReload() async {
        await reloadTask?.value
    }

    private func apply(_ loaded: LoadedSnapshot) {
        data = loaded
        stations = loaded.snapshot.catalogue.domainStations
    }
}
