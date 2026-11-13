#if DEBUG
    import Foundation

    /// Serves hand-made boards, for previews and tests. Boards go through the same time rules
    /// as saved ones.
    nonisolated struct FixedMetroDataSource: MetroDataSource {
        var boards: @Sendable (_ stationID: String, _ now: Date) -> DepartureBoard

        var canReload: Bool { false }

        func loadSnapshot() async -> LoadedSnapshot? { nil }

        func reload(
            progress: @escaping @MainActor @Sendable (MetroReloadPhase) -> Void
        ) async throws -> LoadedSnapshot {
            throw GolemioError.invalidData
        }

        func board(for stationID: String, in data: LoadedSnapshot?, at now: Date) -> DepartureBoard
        {
            boards(stationID, now).evaluated(at: now)
        }
    }
#endif
