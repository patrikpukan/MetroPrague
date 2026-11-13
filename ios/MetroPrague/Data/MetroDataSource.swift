import Foundation

/// Where a metro data reload is in its work.
nonisolated enum MetroReloadPhase: Hashable, Sendable {
    case idle
    case loadingCatalogue
    case loadingBoards(completed: Int, total: Int)
}

/// Provides saved metro data and departure boards. The app uses `GolemioMetroDataSource` when
/// it has a Golemio key and `MockMetroDataSource` otherwise, so stores and screens never branch.
nonisolated protocol MetroDataSource: Sendable {
    /// Whether this source can download data.
    var canReload: Bool { get }

    /// The saved snapshot, if any. Never makes a request.
    func loadSnapshot() async -> LoadedSnapshot?

    /// Downloads and saves a new complete snapshot, reporting progress on the main actor. On
    /// failure, the previous snapshot stays saved.
    func reload(
        progress: @escaping @MainActor @Sendable (MetroReloadPhase) -> Void
    ) async throws -> LoadedSnapshot

    /// The board of `stationID` at `now`, with the time-based rules applied.
    func board(for stationID: String, in data: LoadedSnapshot?, at now: Date) -> DepartureBoard
}
