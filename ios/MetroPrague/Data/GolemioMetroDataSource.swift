import Foundation

/// Downloads metro data from Golemio and keeps it in one snapshot file.
nonisolated struct GolemioMetroDataSource: MetroDataSource {
    let client: GolemioHTTPClient
    let fileStore: SnapshotFileStore
    let now: @Sendable () -> Date

    init(
        client: GolemioHTTPClient,
        fileStore: SnapshotFileStore = SnapshotFileStore(),
        now: @escaping @Sendable () -> Date = { .now }
    ) {
        self.client = client
        self.fileStore = fileStore
        self.now = now
    }

    var canReload: Bool { true }

    // Reading and parsing a 1.5 MB snapshot must not block the UI, so it runs on the concurrent
    // pool instead of the caller's (main) actor.
    @concurrent
    func loadSnapshot() async -> LoadedSnapshot? {
        fileStore.read().map(LoadedSnapshot.init(snapshot:))
    }

    @concurrent
    func reload(
        progress: @escaping @MainActor @Sendable (MetroReloadPhase) -> Void
    ) async throws -> LoadedSnapshot {
        await progress(.loadingCatalogue)
        let catalogue = try await CatalogueLoader.load(using: client)
        let total = catalogue.stations.count
        await progress(.loadingBoards(completed: 0, total: total))

        var boards: [String: SavedBoard] = [:]
        var departureCount = 0
        for (index, station) in catalogue.stations.enumerated() {
            let stopIDs = catalogue.platforms.filter { $0.stationID == station.id }.map(\.id)
            guard !stopIDs.isEmpty else { throw GolemioError.invalidData }
            let data = try await client.get(GolemioEndpoints.departureBoard(stopIDs: stopIDs))
            guard let response = String(data: data, encoding: .utf8) else {
                throw GolemioError.invalidData
            }
            // Departures without a mapped direction still match favorites by headsign.
            let board = try DepartureBoardParser.parse(
                stationID: station.id, response: response, catalogue: catalogue)
            departureCount += board.departures.count
            boards[station.id] = SavedBoard(response: response, fetchedAt: now())
            await progress(.loadingBoards(completed: index + 1, total: total))
        }

        try Task.checkCancellation()
        let snapshot = MetroSnapshot(
            catalogue: catalogue, boards: boards, reloadedAt: now(),
            departureCount: departureCount)
        // One atomic write: a failed or cancelled reload never replaces part of the saved data.
        try fileStore.write(snapshot)
        return LoadedSnapshot(snapshot: snapshot)
    }

    func board(for stationID: String, in data: LoadedSnapshot?, at now: Date) -> DepartureBoard {
        // Nothing saved yet: ask for a reload rather than reporting an outage.
        guard let data else {
            return DepartureBoard(
                stationID: stationID, departures: [], isUnavailable: true, isStale: true)
        }
        guard let board = data.boards[stationID] else {
            return DepartureBoard(stationID: stationID, departures: [], isUnavailable: true)
        }
        return board.evaluated(at: now)
    }
}
